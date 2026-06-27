package com.smartstock.common.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Drains the transactional outbox to Kafka (debt C-2). Runs on a fixed schedule; each tick
 * claims a batch of pending rows (within a transaction, using {@code FOR UPDATE SKIP LOCKED}
 * so multiple instances cooperate) and publishes them with an idempotent, {@code acks=all}
 * producer. A row is marked {@code PUBLISHED} only after the broker acknowledges; on failure
 * the attempt count is bumped and the row stays {@code PENDING} for the next tick — at-least-once
 * delivery with no silent loss. Consumers must be idempotent (debt H-3 / M6).
 *
 * <p>The payload is the event's JSON, relayed verbatim via a {@code String} value serializer,
 * so the bytes on the wire match what the previous direct {@code JsonSerializer} path produced.
 */
@Slf4j
public class OutboxRelay {

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate, int batchSize) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${smartstock.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relay() {
        List<OutboxRecord> batch = repository.fetchBatch(batchSize);
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxRecord record : batch) {
            try {
                // Block on the broker ack so a failure keeps the row PENDING (no false success).
                kafkaTemplate.send(record.topic(), record.eventKey(), record.payload()).get();
                repository.markPublished(record.id());
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
                repository.recordFailure(record.id(), ex.getMessage());
                log.warn("Outbox relay failed for id={} topic={} (attempt {}): {}",
                        record.id(), record.topic(), record.attempts() + 1, ex.getMessage());
                // Stop this tick; the row stays PENDING and is retried next interval. Avoids
                // hammering a down broker and preserves ordering for the remaining rows.
                break;
            }
        }
    }
}
