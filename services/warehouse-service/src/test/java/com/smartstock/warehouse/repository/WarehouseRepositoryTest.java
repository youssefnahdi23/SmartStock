package com.smartstock.warehouse.repository;

import com.smartstock.warehouse.domain.model.Warehouse;
import com.smartstock.warehouse.domain.repository.WarehouseRepository;
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

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class WarehouseRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_warehouse_test")
                    .withUsername("smartstock")
                    .withPassword("smartstock");

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

    @Autowired private WarehouseRepository warehouseRepository;

    private Warehouse mainWarehouse;
    private Warehouse coldStorage;

    @BeforeEach
    void seed() {
        warehouseRepository.deleteAll();

        mainWarehouse = warehouseRepository.save(Warehouse.builder()
                .code("WH-MAIN-001")
                .name("Main Distribution Centre")
                .type("DISTRIBUTION")
                .active(true)
                .createdBy("seed")
                .updatedBy("seed")
                .build());

        coldStorage = warehouseRepository.save(Warehouse.builder()
                .code("WH-COLD-001")
                .name("Cold Storage Facility")
                .type("COLD_STORAGE")
                .active(false)
                .createdBy("seed")
                .updatedBy("seed")
                .build());
    }

    @Nested
    class FindByIdAndNotDeleted {

        @Test
        void existingWarehouse_returnsIt() {
            Optional<Warehouse> result =
                    warehouseRepository.findByIdAndNotDeleted(mainWarehouse.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("WH-MAIN-001");
        }

        @Test
        void softDeletedWarehouse_returnsEmpty() {
            mainWarehouse.setDeletedAt(Instant.now());
            warehouseRepository.save(mainWarehouse);

            assertThat(warehouseRepository.findByIdAndNotDeleted(mainWarehouse.getId())).isEmpty();
        }

        @Test
        void unknownId_returnsEmpty() {
            assertThat(warehouseRepository.findByIdAndNotDeleted("no-such-id")).isEmpty();
        }
    }

    @Nested
    class ExistsByCode {

        @Test
        void existingActiveCode_returnsTrue() {
            assertThat(warehouseRepository.existsByCodeAndDeletedAtIsNull("WH-MAIN-001")).isTrue();
        }

        @Test
        void unknownCode_returnsFalse() {
            assertThat(warehouseRepository.existsByCodeAndDeletedAtIsNull("NO-SUCH-WH")).isFalse();
        }

        @Test
        void softDeletedCode_returnsFalse() {
            mainWarehouse.setDeletedAt(Instant.now());
            warehouseRepository.save(mainWarehouse);

            assertThat(warehouseRepository.existsByCodeAndDeletedAtIsNull("WH-MAIN-001")).isFalse();
        }
    }

    @Nested
    class FindAllWithFilters {

        @Test
        void noFilters_returnsBothWarehouses() {
            Page<Warehouse> page = warehouseRepository
                    .findAllWithFilters(null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterByActive_returnsOnlyActive() {
            Page<Warehouse> page = warehouseRepository
                    .findAllWithFilters(null, null, true, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getCode()).isEqualTo("WH-MAIN-001");
        }

        @Test
        void filterByType_returnsColdStorageOnly() {
            Page<Warehouse> page = warehouseRepository
                    .findAllWithFilters(null, "COLD_STORAGE", null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(coldStorage.getId());
        }

        @Test
        void searchByName_returnsMatchingWarehouse() {
            Page<Warehouse> page = warehouseRepository
                    .findAllWithFilters("Distribution", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getCode()).isEqualTo("WH-MAIN-001");
        }
    }

    @Test
    void save_generatesIdAndTimestamps() {
        Warehouse saved = warehouseRepository.save(Warehouse.builder()
                .code("WH-NEW-001")
                .name("New Facility")
                .type("STANDARD")
                .active(true)
                .createdBy("test")
                .updatedBy("test")
                .build());
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
