-- Idempotent consumption ledger (debt H-3): records every (consumer, event_id) a handler has
-- applied, so at-least-once redeliveries are deduped instead of double-counting customer spend.
-- Written in the handler's transaction; the composite PK makes the claim atomic across instances.
CREATE TABLE processed_events (
    consumer     VARCHAR(128) NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
