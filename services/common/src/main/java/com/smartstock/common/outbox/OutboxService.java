package com.smartstock.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes domain events to the transactional outbox instead of publishing to Kafka directly
 * (debt C-2). Because the insert joins the caller's transaction, the event row commits
 * atomically with the state change — eliminating the dual-write window where a committed DB
 * change could be followed by a lost Kafka send. The {@link OutboxRelay} publishes rows
 * afterwards, at-least-once.
 *
 * <p>Drop-in for the old fire-and-forget publishers: call {@link #append(String, Object)}
 * synchronously from within the service transaction that produced the event.
 */
@Slf4j
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Append an event to the outbox in the current transaction.
     *
     * @param topic destination Kafka topic (use a {@code Topics} constant)
     * @param event the domain event; if it extends {@link DomainEvent} its aggregate id is
     *              used as the Kafka key, preserving per-aggregate ordering
     */
    public void append(String topic, Object event) {
        append(topic, null, event);
    }

    /**
     * Append an event with an explicit Kafka key (e.g. when the aggregate id is not carried on
     * a {@link DomainEvent}). A {@code null} key falls back to the event's aggregate id, then
     * its type.
     */
    public void append(String topic, String key, Object event) {
        String aggregateType = null;
        String aggregateId = null;
        String eventType = event.getClass().getSimpleName();
        if (event instanceof DomainEvent de) {
            aggregateType = de.getAggregateType();
            aggregateId = de.getAggregateId();
            eventType = de.getEventType() != null ? de.getEventType() : eventType;
        }
        if (key == null) {
            key = aggregateId != null ? aggregateId : eventType;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.append(aggregateType, aggregateId, topic, key, eventType, payload);
            log.debug("Outbox appended {} for aggregate {} -> {}", eventType, aggregateId, topic);
        } catch (JsonProcessingException ex) {
            // Serialization failure is a programming error, not a transient one: fail the
            // transaction so the state change rolls back with it rather than silently dropping
            // the event (the exact loss mode C-2 set out to remove).
            throw new IllegalStateException("Failed to serialize event " + eventType + " for outbox", ex);
        }
    }
}
