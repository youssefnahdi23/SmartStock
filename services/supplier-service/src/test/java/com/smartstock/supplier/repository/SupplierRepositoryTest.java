package com.smartstock.supplier.repository;

import com.smartstock.supplier.domain.model.Supplier;
import com.smartstock.supplier.domain.repository.SupplierRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class SupplierRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_supplier_test")
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

    @Autowired private SupplierRepository supplierRepository;

    private Supplier activeSupplier;
    private Supplier inactiveSupplier;

    @BeforeEach
    void seed() {
        supplierRepository.deleteAll();

        activeSupplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-REPO-001")
                .supplierName("Acme Supplies")
                .isActive(true)
                .createdBy("seed")
                .updatedBy("seed")
                .build());

        inactiveSupplier = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-REPO-002")
                .supplierName("Beta Logistics")
                .isActive(false)
                .createdBy("seed")
                .updatedBy("seed")
                .build());
    }

    @Test
    void existsBySupplierCode_existingCode_returnsTrue() {
        assertThat(supplierRepository.existsBySupplierCode("SUP-REPO-001")).isTrue();
    }

    @Test
    void existsBySupplierCode_unknownCode_returnsFalse() {
        assertThat(supplierRepository.existsBySupplierCode("NONEXISTENT")).isFalse();
    }

    @Test
    void findBySupplierCode_existingCode_returnsSupplier() {
        Optional<Supplier> result = supplierRepository.findBySupplierCode("SUP-REPO-001");
        assertThat(result).isPresent();
        assertThat(result.get().getSupplierName()).isEqualTo("Acme Supplies");
    }

    @Test
    void findBySupplierCode_unknownCode_returnsEmpty() {
        assertThat(supplierRepository.findBySupplierCode("NO-SUCH-CODE")).isEmpty();
    }

    @Nested
    class FindWithFilters {

        @Test
        void noFilters_returnsAll() {
            Page<Supplier> page = supplierRepository.findWithFilters(
                    null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterByActive_returnsOnlyActive() {
            Page<Supplier> page = supplierRepository.findWithFilters(
                    null, "ACTIVE", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getSupplierCode()).isEqualTo("SUP-REPO-001");
        }

        @Test
        void filterByInactive_returnsOnlyInactive() {
            Page<Supplier> page = supplierRepository.findWithFilters(
                    null, "INACTIVE", null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(inactiveSupplier.getId());
        }
    }

    @Test
    void findTopRated_onlyReturnsActiveSuppliers() {
        Page<Supplier> topRated = supplierRepository.findTopRated(PageRequest.of(0, 10));
        assertThat(topRated.getContent()).allMatch(s -> s.getIsActive());
    }

    @Test
    void save_autoAssignsId() {
        Supplier saved = supplierRepository.save(Supplier.builder()
                .supplierCode("SUP-REPO-003")
                .supplierName("Gamma Traders")
                .isActive(true)
                .createdBy("test")
                .updatedBy("test")
                .build());
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
