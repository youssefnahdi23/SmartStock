-- Notification log: durable record of every notification triggered by a domain event.
CREATE TABLE notification_log (
    id            BIGSERIAL    PRIMARY KEY,
    event_id      VARCHAR(128) NOT NULL,
    event_type    VARCHAR(128) NOT NULL,
    channel       VARCHAR(64)  NOT NULL DEFAULT 'LOG',
    recipient     VARCHAR(255),
    subject       VARCHAR(512),
    body          TEXT,
    status        VARCHAR(32)  NOT NULL DEFAULT 'SENT',
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_log_event ON notification_log (event_id);
CREATE INDEX idx_notification_log_type  ON notification_log (event_type);

-- Idempotency ledger: deduplicates at-least-once Kafka redeliveries (debt H-3).
CREATE TABLE processed_events (
    consumer     VARCHAR(128) NOT NULL,
    event_id     VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);
