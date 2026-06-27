package com.smartstock.inventory.integration;

import com.smartstock.inventory.AbstractIntegrationTest;
import com.smartstock.inventory.api.dto.request.StockOutRequest;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the optimistic lock (debt C-3) prevents lost updates / oversell under concurrency.
 * Without {@code @Version} on {@link InventoryLevel}, concurrent read-modify-write dispatches
 * would interleave and drive stock negative; with it, conflicting writers fail and the invariant
 * {@code on_hand == initial - 10 * successes} (and never negative) holds exactly.
 */
@DisplayName("Inventory concurrency — no oversell under parallel dispatch (C-3)")
class InventoryConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    InventoryService inventoryService;

    @Autowired
    InventoryLevelRepository inventoryLevelRepository;

    @Test
    @DisplayName("20 parallel dispatches of 10 against 100 stock never oversell")
    void parallelDispatchesNeverOversell() throws InterruptedException {
        InventoryLevel seeded = inventoryLevelRepository.save(InventoryLevel.builder()
                .productId("prod-conc")
                .warehouseId("wh-conc")
                .quantityOnHand(100)
                .quantityReserved(0)
                .unitCost(BigDecimal.valueOf(5))
                .build());
        assertThat(seeded.getVersion()).isNotNull();

        int threads = 20;
        int qtyEach = 10; // total demand 200 > 100 available
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                StockOutRequest req = new StockOutRequest();
                req.setProductId("prod-conc");
                req.setWarehouseId("wh-conc");
                req.setQuantity(qtyEach);
                req.setReferenceType("ORDER");
                try {
                    start.await();
                    // Call the transactional unit directly (bypasses @PreAuthorize); the
                    // optimistic lock + the InsufficientStock guard enforce correctness.
                    inventoryService.dispatchStockTransactional(req, "tester");
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // OptimisticLockException or InsufficientStock — a correctly rejected attempt.
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        InventoryLevel finalLevel = inventoryLevelRepository
                .findByProductIdAndWarehouseId("prod-conc", "wh-conc").orElseThrow();

        assertThat(finalLevel.getQuantityOnHand()).isGreaterThanOrEqualTo(0);
        assertThat(finalLevel.getQuantityOnHand()).isEqualTo(100 - qtyEach * successes.get());
        assertThat(successes.get()).isBetween(1, 10);
    }
}
