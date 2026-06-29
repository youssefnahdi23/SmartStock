package com.smartstock.common.outbox;

/**
 * A single row from the transactional {@code outbox} table awaiting relay to Kafka.
 *
 * @param id        primary key
 * @param topic     destination Kafka topic
 * @param eventKey  partition key (typically the aggregate id)
 * @param eventType simple event type name (diagnostics / filtering)
 * @param payload   the event serialized as JSON — relayed verbatim
 * @param attempts  number of failed relay attempts so far
 */
public record OutboxRecord(
        long id,
        String topic,
        String eventKey,
        String eventType,
        String payload,
        int attempts) {
}
