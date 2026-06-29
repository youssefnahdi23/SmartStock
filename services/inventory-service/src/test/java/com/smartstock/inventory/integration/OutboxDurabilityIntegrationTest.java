package com.smartstock.inventory.integration;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.common.outbox.OutboxRelay;
import com.smartstock.common.outbox.OutboxService;
import com.smartstock.inventory.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests proving the transactional outbox (S-4 / debt C-2) guarantees at-least-once
 * delivery even when the Kafka broker is temporarily unavailable.
 *
 * <p>The outbox's KafkaTemplate is replaced with a controllable mock so that broker
 * availability can be simulated without a real Kafka cluster. The Postgres database is
 * real (Testcontainers), so all atomicity and persistence assertions are meaningful.
 *
 * <p>These tests are excluded from the unit phase (surefire {@code *IntegrationTest} pattern)
 * and run in CI under the {@code integration-test} failsafe profile.
 */
@DisplayName("Outbox durability: events survive Kafka outage (C-2 / S-4)")
class OutboxDurabilityIntegrationTest extends AbstractIntegrationTest {

    // Replace the outbox relay's KafkaTemplate with a mock so we can simulate broker
    // availability. The bean name matches the one registered in OutboxAutoConfiguration.
    @MockBean(name = "outboxKafkaTemplate")
    KafkaTemplate<String, String> outboxKafkaTemplate;

    @Autowired
    OutboxService outboxService;

    @Autowired
    OutboxRelay outboxRelay;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager txManager;

    @BeforeEach
    void truncateOutbox() {
        jdbc.execute("TRUNCATE TABLE outbox");
    }

    @Test
    @DisplayName("event row written to outbox in same DB transaction as the state change")
    void eventPersistedAtomicallyWithStateChange() {
        new TransactionTemplate(txManager).execute(tx -> {
            outboxService.append("inventory.events", "agg-1", new SampleEvent("agg-1"));
            return null;
        });

        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE status = 'PENDING' AND event_type = 'SampleEvent'",
                Long.class);
        assertThat(count).as("event row must be in the outbox after the transaction commits").isEqualTo(1L);
    }

    @Test
    @DisplayName("rolled-back transaction leaves no orphan outbox row")
    void rolledBackTransactionLeavesNoOutboxRow() {
        try {
            new TransactionTemplate(txManager).execute(tx -> {
                outboxService.append("inventory.events", "agg-x", new SampleEvent("agg-x"));
                throw new RuntimeException("force rollback");
            });
        } catch (RuntimeException ignored) {
            // expected
        }

        Long count = jdbc.queryForObject("SELECT count(*) FROM outbox", Long.class);
        assertThat(count).as("rollback must not leave an orphan outbox row").isZero();
    }

    @Test
    @DisplayName("relay leaves row PENDING and increments attempts when Kafka is unreachable")
    void rowStaysPendingWhenKafkaDown() {
        jdbc.update("""
                INSERT INTO outbox (topic, event_key, event_type, payload, status, attempts)
                VALUES ('inventory.events', 'k1', 'StockInEvent', '{"x":1}', 'PENDING', 0)
                """);
        when(outboxKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        outboxRelay.relay();

        Long pending = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE status = 'PENDING'", Long.class);
        Integer attempts = jdbc.queryForObject(
                "SELECT attempts FROM outbox WHERE status = 'PENDING'", Integer.class);
        assertThat(pending).as("row must stay PENDING — event must not be lost").isEqualTo(1L);
        assertThat(attempts).as("relay must increment the failure attempt counter").isEqualTo(1);
    }

    @Test
    @DisplayName("relay publishes and marks row PUBLISHED once Kafka recovers")
    @SuppressWarnings("unchecked")
    void publishedAfterKafkaRecovery() {
        jdbc.update("""
                INSERT INTO outbox (topic, event_key, event_type, payload, status, attempts)
                VALUES ('inventory.events', 'k1', 'StockInEvent', '{"x":1}', 'PENDING', 3)
                """);
        when(outboxKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        outboxRelay.relay();

        Long published = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE status = 'PUBLISHED'", Long.class);
        assertThat(published).as("row must be marked PUBLISHED after successful broker ack").isEqualTo(1L);
        Long pending = jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE status = 'PENDING'", Long.class);
        assertThat(pending).as("no rows must remain PENDING after successful relay").isZero();
    }

    @Test
    @DisplayName("relay stops batch processing on first failure to preserve per-aggregate ordering")
    @SuppressWarnings("unchecked")
    void batchStopsOnFirstFailurePreservingOrdering() {
        jdbc.update("""
                INSERT INTO outbox (topic, event_key, event_type, payload, status, attempts)
                VALUES ('inventory.events', 'k1', 'Ev1', '{}', 'PENDING', 0),
                       ('inventory.events', 'k2', 'Ev2', '{}', 'PENDING', 0)
                """);
        when(outboxKafkaTemplate.send(anyString(), eq("k1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        when(outboxKafkaTemplate.send(anyString(), eq("k2"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker hiccup")));

        outboxRelay.relay();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status='PUBLISHED'", Long.class))
                .as("first row must be published before the failure").isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status='PENDING'", Long.class))
                .as("second row must stay PENDING after the batch stops — it will be retried").isEqualTo(1L);
    }

    @Test
    @DisplayName("previous failures do not prevent eventual delivery — event published on later relay tick")
    @SuppressWarnings("unchecked")
    void eventDeliveredAfterMultipleFailures() {
        jdbc.update("""
                INSERT INTO outbox (topic, event_key, event_type, payload, status, attempts)
                VALUES ('inventory.events', 'k1', 'StockOutEvent', '{}', 'PENDING', 5)
                """);
        // First tick: broker still down
        when(outboxKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("still down")));
        outboxRelay.relay();

        assertThat(jdbc.queryForObject("SELECT attempts FROM outbox", Integer.class)).isEqualTo(6);

        // Second tick: broker recovered
        when(outboxKafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        outboxRelay.relay();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status='PUBLISHED'", Long.class))
                .as("event must be delivered on the tick after broker recovery").isEqualTo(1L);
    }

    // Minimal concrete DomainEvent used only in this test suite.
    static class SampleEvent extends DomainEvent {
        SampleEvent(String aggregateId) {
            super(aggregateId, "SampleAggregate", "inventory-service");
        }
    }
}
