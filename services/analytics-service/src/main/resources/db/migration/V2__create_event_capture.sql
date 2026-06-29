-- Durable event-capture sink (debt H-4): a "capture everything" store so the full historical
-- event stream accrues now, even before analytics logic exists — lost history is unrecoverable.
-- The raw JSON envelope is stored verbatim (schema-stable) for later replay / feature building.
CREATE TABLE captured_events (
    id          BIGSERIAL PRIMARY KEY,
    topic       VARCHAR(255) NOT NULL,
    event_key   VARCHAR(255),
    event_id    VARCHAR(128),
    event_type  VARCHAR(255),
    payload     TEXT         NOT NULL,
    captured_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_captured_events_type ON captured_events (event_type);
CREATE INDEX idx_captured_events_captured_at ON captured_events (captured_at);

-- Idempotent consumption ledger (shared H-3 pattern) so redelivered events are captured once.
CREATE TABLE processed_events (
    consumer     VARCHAR(128) NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
