package com.smartstock.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * JDBC access to the {@code outbox} table. Deliberately JDBC (not JPA) so the shared module
 * carries no managed entity — services need no {@code @EntityScan} wiring, and Hibernate
 * {@code ddl-auto: validate} stays satisfied (the table is owned by Flyway, mapped to no
 * entity). All writes join the caller's active transaction via the shared {@link JdbcTemplate}.
 */
public class OutboxRepository {

    private static final RowMapper<OutboxRecord> MAPPER = (rs, n) -> new OutboxRecord(
            rs.getLong("id"),
            rs.getString("topic"),
            rs.getString("event_key"),
            rs.getString("event_type"),
            rs.getString("payload"),
            rs.getInt("attempts"));

    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert an event in the current transaction. Returns nothing — the relay drains it later. */
    public void append(String aggregateType, String aggregateId, String topic,
                       String eventKey, String eventType, String payload) {
        jdbc.update("""
                INSERT INTO outbox (aggregate_type, aggregate_id, topic, event_key, event_type, payload, status, attempts)
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0)
                """, aggregateType, aggregateId, topic, eventKey, eventType, payload);
    }

    /**
     * Claim a batch of pending rows. {@code FOR UPDATE SKIP LOCKED} lets multiple service
     * instances drain the same outbox concurrently without double-publishing or blocking.
     * Must run inside the relay's transaction.
     */
    public List<OutboxRecord> fetchBatch(int limit) {
        return jdbc.query("""
                SELECT id, topic, event_key, event_type, payload, attempts
                FROM outbox
                WHERE status = 'PENDING'
                ORDER BY id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """, MAPPER, limit);
    }

    public void markPublished(long id) {
        jdbc.update("UPDATE outbox SET status = 'PUBLISHED', published_at = now() WHERE id = ?", id);
    }

    public void recordFailure(long id, String error) {
        String trimmed = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        jdbc.update("UPDATE outbox SET attempts = attempts + 1, last_error = ? WHERE id = ?", trimmed, id);
    }
}
