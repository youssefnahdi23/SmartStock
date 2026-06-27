package com.smartstock.analyticsservice.integration;

import com.smartstock.analyticsservice.AbstractIntegrationTest;
import com.smartstock.analyticsservice.infrastructure.messaging.EventCaptureSink;
import com.smartstock.common.event.Topics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the durable capture sink (debt H-4) persists the raw event envelope and dedupes
 * redeliveries. Drives {@link EventCaptureSink#capture} directly (the Kafka listener is stopped in
 * the base) against a real Postgres so the JDBC write + idempotency ledger are exercised together.
 */
@DisplayName("Event capture sink — durable, idempotent capture (H-4)")
class EventCaptureSinkIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    EventCaptureSink sink;

    @Autowired
    JdbcTemplate jdbc;

    private ConsumerRecord<String, String> record(String eventId, String json) {
        return new ConsumerRecord<>(Topics.SALES_ORDER_EVENTS, 0, 0L, "agg-1", json);
    }

    @Test
    @DisplayName("captures the raw envelope with parsed id/type")
    void capturesEnvelope() {
        String json = "{\"eventId\":\"evt-100\",\"eventType\":\"DeliveryCompletedEvent\",\"customerId\":\"c1\"}";

        sink.capture(record("evt-100", json));

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM captured_events WHERE event_id = ?", Long.class, "evt-100");
        assertThat(count).isEqualTo(1L);
        String type = jdbc.queryForObject(
                "SELECT event_type FROM captured_events WHERE event_id = ?", String.class, "evt-100");
        assertThat(type).isEqualTo("DeliveryCompletedEvent");
        String payload = jdbc.queryForObject(
                "SELECT payload FROM captured_events WHERE event_id = ?", String.class, "evt-100");
        assertThat(payload).isEqualTo(json);
    }

    @Test
    @DisplayName("redelivered event is captured only once (idempotent)")
    void idempotentCapture() {
        String json = "{\"eventId\":\"evt-200\",\"eventType\":\"SalesOrderCreatedEvent\"}";

        sink.capture(record("evt-200", json));
        sink.capture(record("evt-200", json)); // redelivery

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM captured_events WHERE event_id = ?", Long.class, "evt-200");
        assertThat(count).isEqualTo(1L);
    }
}
