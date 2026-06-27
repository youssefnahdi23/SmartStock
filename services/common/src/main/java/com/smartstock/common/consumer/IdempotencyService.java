package com.smartstock.common.consumer;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Idempotent-consumption guard (debt H-3). Kafka is at-least-once and the outbox relay (C-2)
 * can redeliver, so a handler that mutates state (e.g. {@code customer.recordOrder(amount)})
 * must dedupe or it double-counts. {@link #claim(String, String)} inserts a marker keyed by
 * (consumer, eventId); the unique primary key makes the insert atomic across instances. Call it
 * inside the handler's transaction: if the handler rolls back, the claim rolls back with it, so
 * the event is correctly reprocessed rather than lost.
 */
public class IdempotencyService {

    private final JdbcTemplate jdbc;

    public IdempotencyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Attempt to claim an event for processing.
     *
     * @return {@code true} if this is the first time the (consumer, eventId) pair is seen and the
     * handler should proceed; {@code false} if it was already processed and should be skipped.
     */
    public boolean claim(String consumer, String eventId) {
        if (eventId == null || eventId.isBlank()) {
            // No id to dedupe on — let it through rather than silently dropping. Producers set
            // eventId on every DomainEvent, so this is the defensive edge only.
            return true;
        }
        int inserted = jdbc.update("""
                INSERT INTO processed_events (consumer, event_id, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (consumer, event_id) DO NOTHING
                """, consumer, eventId);
        return inserted == 1;
    }
}
