package com.smartstock.notificationservice.integration;

import com.smartstock.notificationservice.AbstractIntegrationTest;
import com.smartstock.notificationservice.domain.repository.NotificationLogRepository;
import com.smartstock.notificationservice.infrastructure.messaging.DomainEventPayload;
import com.smartstock.notificationservice.infrastructure.messaging.NotificationEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the domain-event → notification-log pipeline end-to-end:
 * event payload → idempotency claim → NotificationLog row persisted.
 *
 * Excluded from unit-test phase (surefire *IntegrationTest pattern).
 */
@DisplayName("Domain events → notification log (end-to-end)")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationEventIntegrationTest extends AbstractIntegrationTest {

    @Autowired NotificationEventListener  listener;
    @Autowired NotificationLogRepository  notificationLogRepository;
    @Autowired JdbcTemplate               jdbc;

    // ── helpers ──────────────────────────────────────────────────────────────

    private static DomainEventPayload lowStock(String eventId, String productId) {
        return new DomainEventPayload(eventId, "LowStockThresholdReachedEvent",
                UUID.randomUUID().toString(),
                productId, "WH-1", 5,
                null, null, null,
                null, null,
                null, null);
    }

    private static DomainEventPayload deliveryCompleted(String eventId, String soNumber) {
        return new DomainEventPayload(eventId, "DeliveryCompletedEvent",
                UUID.randomUUID().toString(),
                null, null, null,
                soNumber, "CUST-1", BigDecimal.valueOf(299.99),
                null, null,
                null, null);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("LowStockThresholdReachedEvent creates a notification log row")
    void lowStockEventCreatesNotificationLog() {
        long before = notificationLogRepository.count();

        listener.onEvent(lowStock(UUID.randomUUID().toString(), "PROD-001"));

        assertThat(notificationLogRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("DeliveryCompletedEvent creates a notification log row")
    void deliveryCompletedEventCreatesNotificationLog() {
        long before = notificationLogRepository.count();

        listener.onEvent(deliveryCompleted(UUID.randomUUID().toString(), "SO-100"));

        assertThat(notificationLogRepository.count()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("Redelivered event is idempotent — notification logged exactly once (H-3)")
    void redeliveryDoesNotDuplicateNotification() {
        DomainEventPayload payload = lowStock(UUID.randomUUID().toString(), "PROD-002");
        long before = notificationLogRepository.count();

        listener.onEvent(payload);
        listener.onEvent(payload); // simulated redelivery

        assertThat(notificationLogRepository.count())
                .as("Redelivery must produce exactly one notification row (H-3)")
                .isEqualTo(before + 1);
    }

    @Test
    @DisplayName("Non-actionable events are silently ignored")
    void nonActionableEventsAreIgnored() {
        long before = notificationLogRepository.count();

        listener.onEvent(new DomainEventPayload(
                UUID.randomUUID().toString(), "SalesOrderCancelledEvent",
                UUID.randomUUID().toString(),
                null, null, null,
                "SO-999", "CUST-1", null,
                null, null,
                null, null));

        assertThat(notificationLogRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("Processed event ID is recorded in the idempotency ledger")
    void processedEventRecordedInLedger() {
        String eventId = UUID.randomUUID().toString();

        listener.onEvent(lowStock(eventId, "PROD-003"));

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM processed_events WHERE event_id = ?", Long.class, eventId);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("Null event is safely ignored without throwing")
    void nullEventIsIgnored() {
        long before = notificationLogRepository.count();
        listener.onEvent(null); // must not throw
        assertThat(notificationLogRepository.count()).isEqualTo(before);
    }
}
