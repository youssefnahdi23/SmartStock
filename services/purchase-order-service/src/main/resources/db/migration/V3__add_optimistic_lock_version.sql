-- Optimistic locking (S-5): add a JPA @Version column to the mutable PurchaseOrder aggregate
-- so concurrent status transitions (confirm, cancel, deliver) cannot lost-update each other.
-- NOT NULL DEFAULT 0 backfills existing rows; Hibernate increments it on every UPDATE.
ALTER TABLE purchase_orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
