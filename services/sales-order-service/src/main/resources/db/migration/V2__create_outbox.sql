-- Transactional outbox (debt C-2): domain events are inserted here in the SAME transaction as
-- the state change, then relayed to Kafka by the shared OutboxRelay. Owned by Flyway and mapped
-- to NO JPA entity (the relay uses JDBC), so Hibernate ddl-auto=validate is unaffected.
CREATE TABLE outbox (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(128),
    aggregate_id   VARCHAR(128),
    topic          VARCHAR(255) NOT NULL,
    event_key      VARCHAR(255),
    event_type     VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    published_at   TIMESTAMP
);

-- The relay drains PENDING rows in id order; a partial index keeps that hot path small.
CREATE INDEX idx_outbox_pending ON outbox (status, id) WHERE status = 'PENDING';
