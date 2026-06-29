-- Idempotency ledger (debt H-3): Kafka is at-least-once, so each consumer handler
-- claims an (consumer, event_id) pair here before mutating state. The composite primary
-- key makes the INSERT atomic; a duplicate claim returns 0 rows inserted and the handler
-- skips the mutation safely.
CREATE TABLE processed_events (
    consumer     VARCHAR(128) NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
