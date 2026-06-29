package com.smartstock.customer.integration;

import com.smartstock.customer.AbstractIntegrationTest;
import com.smartstock.customer.domain.model.Customer;
import com.smartstock.customer.domain.repository.CustomerRepository;
import com.smartstock.customer.event.SalesOrderEventPayload;
import com.smartstock.customer.service.OrderEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end event flow test: sales-order DeliveryCompletedEvent → customer spend update.
 *
 * <p>Drives {@link OrderEventListener#onSalesOrderEvent} directly — the same path a message
 * arriving from {@code sales-order.events} would take — against a real Postgres schema. This
 * validates the complete wiring: event payload deserialization, idempotency ledger
 * ({@code processed_events}), and customer aggregate mutation all in one transaction (C-4, H-3).
 *
 * <p>The test is excluded from unit-test phase by surefire ({@code *IntegrationTest} pattern)
 * and runs in CI under the {@code integration-test} failsafe profile.
 */
@DisplayName("Sales-order → customer spend: end-to-end event flow (C-4)")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderEventFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    OrderEventListener listener;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    JdbcTemplate jdbc;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(Customer.builder()
                .customerCode("TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .customerName("Test Customer")
                .customerType("RETAIL")
                .build());
    }

    @Test
    @DisplayName("DeliveryCompletedEvent increments totalOrders and totalSpent")
    void deliveryCompletedUpdatesCustomerSpend() {
        String eventId = UUID.randomUUID().toString();
        SalesOrderEventPayload payload = new SalesOrderEventPayload(
                eventId, "DeliveryCompletedEvent", customer.getId(), new BigDecimal("199.99"));

        listener.onSalesOrderEvent(payload);

        Customer updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getTotalOrders()).isEqualTo(1);
        assertThat(updated.getTotalSpent()).isEqualByComparingTo("199.99");
    }

    @Test
    @DisplayName("second DeliveryCompletedEvent accumulates spend correctly")
    void multipleDeliveryEventsAccumulateSpend() {
        String eventId1 = UUID.randomUUID().toString();
        String eventId2 = UUID.randomUUID().toString();

        listener.onSalesOrderEvent(new SalesOrderEventPayload(
                eventId1, "DeliveryCompletedEvent", customer.getId(), new BigDecimal("100.00")));
        listener.onSalesOrderEvent(new SalesOrderEventPayload(
                eventId2, "DeliveryCompletedEvent", customer.getId(), new BigDecimal("50.00")));

        Customer updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getTotalOrders()).isEqualTo(2);
        assertThat(updated.getTotalSpent()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("redelivered DeliveryCompletedEvent is idempotent — spend counted once (H-3)")
    void redeliveryDoesNotDoubleCount() {
        String eventId = UUID.randomUUID().toString();
        SalesOrderEventPayload payload = new SalesOrderEventPayload(
                eventId, "DeliveryCompletedEvent", customer.getId(), new BigDecimal("75.00"));

        listener.onSalesOrderEvent(payload);
        listener.onSalesOrderEvent(payload); // simulated redelivery

        Customer updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getTotalOrders())
                .as("Redelivery must not double-count orders (H-3 idempotency)")
                .isEqualTo(1);
        assertThat(updated.getTotalSpent()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("non-completion event types do not update customer spend")
    void nonCompletionEventsAreIgnored() {
        listener.onSalesOrderEvent(new SalesOrderEventPayload(
                UUID.randomUUID().toString(), "SalesOrderCreatedEvent",
                customer.getId(), new BigDecimal("300.00")));
        listener.onSalesOrderEvent(new SalesOrderEventPayload(
                UUID.randomUUID().toString(), "SalesOrderConfirmedEvent",
                customer.getId(), new BigDecimal("300.00")));

        Customer unchanged = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(unchanged.getTotalOrders()).isNull();
        assertThat(unchanged.getTotalSpent()).isNull();
    }

    @Test
    @DisplayName("event is persisted in the idempotency ledger after processing")
    void processedEventIsRecordedInLedger() {
        String eventId = UUID.randomUUID().toString();
        listener.onSalesOrderEvent(new SalesOrderEventPayload(
                eventId, "DeliveryCompletedEvent", customer.getId(), new BigDecimal("42.00")));

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?", Long.class, eventId);
        assertThat(count)
                .as("Processed event must be recorded in the idempotency ledger")
                .isEqualTo(1L);
    }
}
