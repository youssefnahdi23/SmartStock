package com.smartstock.inventory.repository;

import com.smartstock.inventory.domain.model.InventoryLevel;
import com.smartstock.inventory.domain.repository.InventoryLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null") // IDE strict-null false positives on Lombok @NonNull + JPA save()
class InventoryLevelRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_inventory_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",          postgres::getJdbcUrl);
        registry.add("spring.flyway.user",         postgres::getUsername);
        registry.add("spring.flyway.password",     postgres::getPassword);
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired private InventoryLevelRepository inventoryLevelRepository;

    private static final String WAREHOUSE_A  = "WH-A";
    private static final String WAREHOUSE_B  = "WH-B";
    private static final String PRODUCT_SKU1 = "prod-sku-001";
    private static final String PRODUCT_SKU2 = "prod-sku-002";

    @BeforeEach
    void seed() {
        inventoryLevelRepository.deleteAll();

        // Product 1 in warehouse A — normal stock (available = 100 - 10 = 90)
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId(PRODUCT_SKU1)
                .warehouseId(WAREHOUSE_A)
                .quantityOnHand(100)
                .quantityReserved(10)
                .reorderPoint(20)
                .build());

        // Product 2 in warehouse A — LOW STOCK (onHand=5 <= reorderPoint=20)
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId(PRODUCT_SKU2)
                .warehouseId(WAREHOUSE_A)
                .quantityOnHand(5)
                .quantityReserved(0)
                .reorderPoint(20)
                .build());

        // Product 1 in warehouse B — healthy stock
        inventoryLevelRepository.save(InventoryLevel.builder()
                .productId(PRODUCT_SKU1)
                .warehouseId(WAREHOUSE_B)
                .quantityOnHand(50)
                .quantityReserved(0)
                .reorderPoint(10)
                .build());
    }

    @Nested
    class FindByProductAndWarehouse {

        @Test
        void existingCombination_returnsLevel() {
            Optional<InventoryLevel> result =
                    inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_SKU1, WAREHOUSE_A);
            assertThat(result).isPresent();
            assertThat(result.get().getQuantityOnHand()).isEqualTo(100);
        }

        @Test
        void unknownCombination_returnsEmpty() {
            assertThat(inventoryLevelRepository
                    .findByProductIdAndWarehouseId("no-prod", "no-wh")).isEmpty();
        }

        @Test
        void existsByProductAndWarehouse_returnsTrue() {
            assertThat(inventoryLevelRepository
                    .existsByProductIdAndWarehouseId(PRODUCT_SKU1, WAREHOUSE_A)).isTrue();
        }

        @Test
        void notExistsByProductAndWarehouse_returnsFalse() {
            assertThat(inventoryLevelRepository
                    .existsByProductIdAndWarehouseId("ghost-prod", WAREHOUSE_A)).isFalse();
        }
    }

    @Nested
    class FindAllByProductId {

        @Test
        void productInMultipleWarehouses_returnsAllLevels() {
            List<InventoryLevel> levels =
                    inventoryLevelRepository.findAllByProductId(PRODUCT_SKU1);
            assertThat(levels).hasSize(2);
            assertThat(levels.stream().map(InventoryLevel::getWarehouseId))
                    .containsExactlyInAnyOrder(WAREHOUSE_A, WAREHOUSE_B);
        }

        @Test
        void productInOneWarehouse_returnsSingleLevel() {
            List<InventoryLevel> levels =
                    inventoryLevelRepository.findAllByProductId(PRODUCT_SKU2);
            assertThat(levels).hasSize(1);
        }
    }

    @Nested
    class FindByWarehouseWithFilters {

        @Test
        void noFilters_returnsAllForWarehouse() {
            Page<InventoryLevel> page = inventoryLevelRepository
                    .findByWarehouseWithFilters(WAREHOUSE_A, false, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void lowStockFilter_returnsOnlyLowStockItems() {
            Page<InventoryLevel> page = inventoryLevelRepository
                    .findByWarehouseWithFilters(WAREHOUSE_A, true, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getProductId()).isEqualTo(PRODUCT_SKU2);
        }
    }

    @Nested
    class FindLowStockByWarehouse {

        @Test
        void warehouseA_returnsOnlyLowStockItems() {
            List<InventoryLevel> lowStock =
                    inventoryLevelRepository.findLowStockByWarehouse(WAREHOUSE_A);
            assertThat(lowStock).hasSize(1);
            assertThat(lowStock.get(0).getProductId()).isEqualTo(PRODUCT_SKU2);
        }

        @Test
        void warehouseB_noLowStockItems_returnsEmpty() {
            List<InventoryLevel> lowStock =
                    inventoryLevelRepository.findLowStockByWarehouse(WAREHOUSE_B);
            assertThat(lowStock).isEmpty();
        }
    }
}
