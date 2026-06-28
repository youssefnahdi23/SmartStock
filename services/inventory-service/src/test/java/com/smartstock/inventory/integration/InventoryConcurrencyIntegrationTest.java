package com.smartstock.inventory.integration;

import com.smartstock.inventory.AbstractIntegrationTest;
import com.smartstock.inventory.api.dto.request.ReservationRequest;
import com.smartstock.inventory.api.dto.request.StockOutRequest;
import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import com.smartstock.inventory.service.ConcurrencyRetry;
import com.smartstock.inventory.service.InventoryService;
import com.smartstock.inventory.service.ReservationService;
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
 * Proves the optimistic lock (S-5 / debt C-3) prevents lost updates and oversell under
 * real concurrent DB writes. Each test seeds its own product+warehouse pair so tests are
 * isolated even though they share the same Testcontainers PostgreSQL instance.
 */
@DisplayName("Inventory concurrency — no oversell or lost updates (S-5 / C-3)")
class InventoryConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired InventoryService inventoryService;
    @Autowired ReservationService reservationService;
    @Autowired InventoryLevelRepository inventoryLevelRepository;
    @Autowired ConcurrencyRetry concurrencyRetry;

    // ── Test 1: oversell prevention ───────────────────────────────────────────

    @Test
    @DisplayName("20 parallel dispatches of 10 against 100 stock never oversell")
    void parallelDispatchesNeverOversell() throws InterruptedException {
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId("prod-conc").warehouseId("wh-conc")
                .quantityOnHand(100).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(5)).build());

        int threads = 20, qtyEach = 10; // total demand 200 > 100
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
                    inventoryService.dispatchStockTransactional(req, "tester");
                    successes.incrementAndGet();
                } catch (Exception ignored) { }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId("prod-conc", "wh-conc").orElseThrow();

        assertThat(level.getQuantityOnHand()).isGreaterThanOrEqualTo(0);
        assertThat(level.getQuantityOnHand()).isEqualTo(100 - qtyEach * successes.get());
        assertThat(successes.get()).isBetween(1, 10);
    }

    // ── Test 2: no over-reservation ──────────────────────────────────────────

    @Test
    @DisplayName("10 parallel reservations of 10 against 50 stock never over-reserve")
    void parallelReservationsNeverOverReserve() throws InterruptedException {
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId("prod-resv").warehouseId("wh-resv")
                .quantityOnHand(50).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(10)).build());

        int threads = 10, qtyEach = 10; // total demand 100 > 50
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int ordinal = i;
            pool.submit(() -> {
                ReservationRequest req = new ReservationRequest();
                req.setProductId("prod-resv");
                req.setWarehouseId("wh-resv");
                req.setQuantity(qtyEach);
                req.setOrderId("order-resv-" + ordinal);
                try {
                    start.await();
                    // Call the transactional inner method directly to bypass @PreAuthorize.
                    reservationService.reserveTransactional(req, "tester");
                    successes.incrementAndGet();
                } catch (Exception ignored) { }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId("prod-resv", "wh-resv").orElseThrow();

        // Available stock can never go negative: reserved ≤ on-hand.
        assertThat(level.getQuantityAvailable()).isGreaterThanOrEqualTo(0);
        assertThat(level.getQuantityReserved()).isEqualTo(successes.get() * qtyEach);
        assertThat(successes.get()).isBetween(1, 5);
    }

    // ── Test 3: retry enables all dispatches when stock is sufficient ─────────

    @Test
    @DisplayName("ConcurrencyRetry lets all 5 dispatches succeed when total demand equals supply")
    void retryAllowsAllDispatches_whenStockIsSufficient() throws InterruptedException {
        // demand == supply: every thread SHOULD succeed eventually via retry.
        // Without retry, optimistic-lock losers would fail even though stock was available.
        int threads = 5, qtyEach = 10, total = threads * qtyEach; // 50

        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId("prod-retry").warehouseId("wh-retry")
                .quantityOnHand(total).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(3)).build());

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                StockOutRequest req = new StockOutRequest();
                req.setProductId("prod-retry");
                req.setWarehouseId("wh-retry");
                req.setQuantity(qtyEach);
                req.setReferenceType("ORDER");
                try {
                    start.await();
                    // Route through ConcurrencyRetry so lock losers are retried against fresh state.
                    concurrencyRetry.execute(() ->
                            inventoryService.dispatchStockTransactional(req, "tester"));
                    successes.incrementAndGet();
                } catch (Exception ignored) { }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId("prod-retry", "wh-retry").orElseThrow();

        // Every thread should have succeeded: stock exactly drained to 0.
        assertThat(successes.get()).isEqualTo(threads);
        assertThat(level.getQuantityOnHand()).isZero();
    }

    // ── Test 4: no lost updates — final balance is always exact ──────────────

    @Test
    @DisplayName("Final stock balance is exactly initial minus dispatched — no lost updates")
    void stockBalanceIsExact_noLostUpdates() throws InterruptedException {
        // With @Version, each committed dispatch is visible to the next: the tally is exact.
        // Without @Version, two threads reading the same balance would both subtract and only
        // one decrement would persist — the final balance would be too high (lost update).
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId("prod-balance").warehouseId("wh-balance")
                .quantityOnHand(200).quantityReserved(0)
                .unitCost(BigDecimal.valueOf(1)).build());

        int threads = 30, qtyEach = 5; // 150 total demand < 200 supply → all should succeed
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                StockOutRequest req = new StockOutRequest();
                req.setProductId("prod-balance");
                req.setWarehouseId("wh-balance");
                req.setQuantity(qtyEach);
                req.setReferenceType("ORDER");
                try {
                    start.await();
                    concurrencyRetry.execute(() ->
                            inventoryService.dispatchStockTransactional(req, "tester"));
                    successes.incrementAndGet();
                } catch (Exception ignored) { }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        InventoryLevel level = inventoryLevelRepository
                .findByProductIdAndWarehouseId("prod-balance", "wh-balance").orElseThrow();

        // The invariant that @Version enforces: every committed decrement is reflected exactly.
        assertThat(level.getQuantityOnHand()).isEqualTo(200 - qtyEach * successes.get());
        assertThat(level.getQuantityOnHand()).isGreaterThanOrEqualTo(0);
        // All 30 dispatches of 5 from 200 should succeed (150 ≤ 200).
        assertThat(successes.get()).isEqualTo(threads);
    }
}
