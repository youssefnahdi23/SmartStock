package com.smartstock.purchase.repository;

import com.smartstock.purchase.domain.model.PurchaseOrder;
import com.smartstock.purchase.domain.repository.PurchaseOrderRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class PurchaseOrderRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_purchase_order_test")
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

    @Autowired private PurchaseOrderRepository purchaseOrderRepository;

    private PurchaseOrder createdPO;
    private PurchaseOrder confirmedPO;

    @BeforeEach
    void seed() {
        purchaseOrderRepository.deleteAll();

        createdPO = purchaseOrderRepository.save(PurchaseOrder.builder()
                .poNumber("PO-REPO-001")
                .supplierId("sup-001")
                .supplierName("Acme Supplies")
                .status("CREATED")
                .orderDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .expectedDeliveryDate(LocalDate.now().plusDays(7))
                .deliveryWarehouseId("WH-001")
                .totalQuantity(100)
                .deliveredQuantity(0)
                .totalAmount(BigDecimal.valueOf(5000))
                .totalLineAmount(BigDecimal.valueOf(5000))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .deliveryStatus("NOT_RECEIVED")
                .paymentStatus("UNPAID")
                .lineItems(new ArrayList<>())
                .qualityIssues(new ArrayList<>())
                .createdBy("test-user")
                .updatedBy("test-user")
                .build());

        confirmedPO = purchaseOrderRepository.save(PurchaseOrder.builder()
                .poNumber("PO-REPO-002")
                .supplierId("sup-002")
                .supplierName("Beta Logistics")
                .status("CONFIRMED")
                .orderDate(LocalDate.now().minusDays(2))
                .dueDate(LocalDate.now().plusDays(5))
                .expectedDeliveryDate(LocalDate.now().plusDays(5))
                .deliveryWarehouseId("WH-001")
                .totalQuantity(50)
                .deliveredQuantity(0)
                .totalAmount(BigDecimal.valueOf(2500))
                .totalLineAmount(BigDecimal.valueOf(2500))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .deliveryStatus("NOT_RECEIVED")
                .paymentStatus("UNPAID")
                .lineItems(new ArrayList<>())
                .qualityIssues(new ArrayList<>())
                .createdBy("test-user")
                .updatedBy("test-user")
                .build());
    }

    @Nested
    class ExistsByPoNumber {

        @Test
        void existingPoNumber_returnsTrue() {
            assertThat(purchaseOrderRepository.existsByPoNumber("PO-REPO-001")).isTrue();
        }

        @Test
        void unknownPoNumber_returnsFalse() {
            assertThat(purchaseOrderRepository.existsByPoNumber("PO-NONEXISTENT")).isFalse();
        }
    }

    @Nested
    class FindById {

        @Test
        void existingId_returnsPO() {
            Optional<PurchaseOrder> result = purchaseOrderRepository.findById(createdPO.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getPoNumber()).isEqualTo("PO-REPO-001");
        }

        @Test
        void unknownId_returnsEmpty() {
            assertThat(purchaseOrderRepository.findById("no-such-id")).isEmpty();
        }
    }

    @Nested
    class FindWithFilters {

        @Test
        void noFilters_returnsAllOrders() {
            Page<PurchaseOrder> page = purchaseOrderRepository
                    .findWithFilters(null, null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterByStatus_createdOnly_returnsOneOrder() {
            Page<PurchaseOrder> page = purchaseOrderRepository
                    .findWithFilters("CREATED", null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getPoNumber()).isEqualTo("PO-REPO-001");
        }

        @Test
        void filterBySupplierId_returnsMatchingOrders() {
            Page<PurchaseOrder> page = purchaseOrderRepository
                    .findWithFilters(null, "sup-001", null, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getSupplierId()).isEqualTo("sup-001");
        }

        @Test
        void filterByConfirmedStatus_returnsConfirmedPO() {
            Page<PurchaseOrder> page = purchaseOrderRepository
                    .findWithFilters("CONFIRMED", null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(confirmedPO.getId());
        }

        @Test
        void filterByWarehouseId_returnsBothOrders() {
            Page<PurchaseOrder> page = purchaseOrderRepository
                    .findWithFilters(null, null, "WH-001", null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    class DomainStateTransitions {

        @Test
        void confirm_persistsStatusChange() {
            createdPO.confirm("CONF-REPO-001", "test-user");
            purchaseOrderRepository.saveAndFlush(createdPO);

            PurchaseOrder fetched = purchaseOrderRepository.findById(createdPO.getId()).orElseThrow();
            assertThat(fetched.getStatus()).isEqualTo("CONFIRMED");
            assertThat(fetched.getConfirmationNumber()).isEqualTo("CONF-REPO-001");
            assertThat(fetched.getConfirmationDate()).isNotNull();
        }

        @Test
        void cancel_persistsCancellationData() {
            createdPO.cancel("Supplier out of stock", "test-user");
            purchaseOrderRepository.saveAndFlush(createdPO);

            PurchaseOrder fetched = purchaseOrderRepository.findById(createdPO.getId()).orElseThrow();
            assertThat(fetched.getStatus()).isEqualTo("CANCELLED");
            assertThat(fetched.getCancellationReason()).isEqualTo("Supplier out of stock");
            assertThat(fetched.getCancelledAt()).isNotNull();
        }

        @Test
        void optimisticLocking_versionIncrements() {
            Long v0 = createdPO.getVersion();
            createdPO.setNotes("Updated notes");
            PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(createdPO);
            assertThat(saved.getVersion()).isGreaterThan(v0);
        }
    }
}
