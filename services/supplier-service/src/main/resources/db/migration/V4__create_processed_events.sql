-- Idempotency ledger (debt H-3): deduplicates at-least-once Kafka redeliveries.
CREATE TABLE processed_events (
    consumer     VARCHAR(128) NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
