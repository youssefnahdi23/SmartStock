package com.smartstock.analyticsservice.infrastructure.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * JDBC writer for the durable event-capture store (debt H-4). JDBC (not JPA) keeps the table
 * free of a managed entity, so Hibernate {@code ddl-auto: validate} stays satisfied.
 */
@Repository
public class EventCaptureRepository {

    private final JdbcTemplate jdbc;

    public EventCaptureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void capture(String topic, String eventKey, String eventId, String eventType, String payload) {
        jdbc.update("""
                INSERT INTO captured_events (topic, event_key, event_id, event_type, payload)
                VALUES (?, ?, ?, ?, ?)
                """, topic, eventKey, eventId, eventType, payload);
    }
}
