-- Optimistic locking (debt C-3): add a JPA @Version column to the mutable inventory aggregates
-- so concurrent read-modify-write cannot lost-update / oversell. NOT NULL DEFAULT 0 backfills
-- existing rows; Hibernate increments it on every update and rejects stale writers.
ALTER TABLE inventory_levels ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stock_out        ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory_holds  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
