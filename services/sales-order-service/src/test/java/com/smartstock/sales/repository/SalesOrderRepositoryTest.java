package com.smartstock.sales.repository;

import com.smartstock.sales.domain.model.SalesOrder;
import com.smartstock.sales.domain.repository.SalesOrderRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class SalesOrderRepositoryTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_sales_order_test")
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

    @Autowired private SalesOrderRepository salesOrderRepository;

    private SalesOrder createdOrder;
    private SalesOrder confirmedOrder;

    @BeforeEach
    void seed() {
        salesOrderRepository.deleteAll();

        createdOrder = salesOrderRepository.save(SalesOrder.builder()
                .soNumber("SO-REPO-001")
                .customerId("cust-001")
                .customerName("Acme Corp")
                .orderDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(3))
                .pickingWarehouseId("WH-001")
                .status("CREATED")
                .fulfillmentStatus("PENDING")
                .paymentStatus("UNPAID")
                .totalAmount(BigDecimal.valueOf(1500))
                .totalLineAmount(BigDecimal.valueOf(1500))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineItems(new ArrayList<>())
                .shipments(new ArrayList<>())
                .createdBy("test-user")
                .updatedBy("test-user")
                .build());

        confirmedOrder = salesOrderRepository.save(SalesOrder.builder()
                .soNumber("SO-REPO-002")
                .customerId("cust-002")
                .customerName("Beta Ltd")
                .orderDate(LocalDate.now().minusDays(1))
                .dueDate(LocalDate.now().plusDays(2))
                .pickingWarehouseId("WH-001")
                .status("CONFIRMED")
                .fulfillmentStatus("PENDING")
                .paymentStatus("PAID")
                .totalAmount(BigDecimal.valueOf(800))
                .totalLineAmount(BigDecimal.valueOf(800))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineItems(new ArrayList<>())
                .shipments(new ArrayList<>())
                .createdBy("test-user")
                .updatedBy("test-user")
                .build());
    }

    @Test
    void existsBySoNumber_existingSoNumber_returnsTrue() {
        assertThat(salesOrderRepository.existsBySoNumber("SO-REPO-001")).isTrue();
    }

    @Test
    void existsBySoNumber_unknownSoNumber_returnsFalse() {
        assertThat(salesOrderRepository.existsBySoNumber("SO-NONEXISTENT")).isFalse();
    }

    @Nested
    class FindWithFilters {

        @Test
        void noFilters_returnsAll() {
            Page<SalesOrder> page = salesOrderRepository
                    .findWithFilters(null, null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filterByStatus_returnsOnlyMatchingOrders() {
            Page<SalesOrder> page = salesOrderRepository
                    .findWithFilters("CONFIRMED", null, null, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getId()).isEqualTo(confirmedOrder.getId());
        }

        @Test
        void filterByCustomerId_returnsCustomerOrders() {
            Page<SalesOrder> page = salesOrderRepository
                    .findWithFilters(null, "cust-001", null, null, null, PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getSoNumber()).isEqualTo("SO-REPO-001");
        }

        @Test
        void filterByDateRange_returnsBothOrders() {
            Page<SalesOrder> page = salesOrderRepository
                    .findWithFilters(null, null, null,
                            LocalDate.now().minusDays(2),
                            LocalDate.now().plusDays(1),
                            PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isEqualTo(2);
        }
    }

    @Nested
    class CustomerOrderHistory {

        @Test
        void findByCustomerId_returnsMostRecentFirst() {
            List<SalesOrder> orders =
                    salesOrderRepository.findByCustomerIdOrderByCreatedAtDesc("cust-001");
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getSoNumber()).isEqualTo("SO-REPO-001");
        }

        @Test
        void findByCustomerId_unknownCustomer_returnsEmpty() {
            assertThat(salesOrderRepository
                    .findByCustomerIdOrderByCreatedAtDesc("unknown-customer")).isEmpty();
        }
    }

    @Nested
    class PendingDeliveryQuery {

        @Test
        void confirmedOrders_includedInPendingDelivery() {
            List<SalesOrder> pending = salesOrderRepository.findPendingDelivery();
            assertThat(pending).anyMatch(so -> so.getId().equals(confirmedOrder.getId()));
        }

        @Test
        void createdOrders_notIncludedInPendingDelivery() {
            List<SalesOrder> pending = salesOrderRepository.findPendingDelivery();
            assertThat(pending).noneMatch(so -> so.getId().equals(createdOrder.getId()));
        }
    }

    @Test
    void countByDateRange_returnsCorrectCount() {
        long count = salesOrderRepository.countByDateRange(
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));
        assertThat(count).isEqualTo(2);
    }

    @Test
    void optimisticLocking_versionIncrementsOnUpdate() {
        Long v0 = createdOrder.getVersion();
        createdOrder.setCustomerName("Updated Name");
        SalesOrder saved = salesOrderRepository.saveAndFlush(createdOrder);
        assertThat(saved.getVersion()).isGreaterThan(v0);
    }
}
