-- Inventory Service — Full Schema
-- Port 8003 | DB: smartstock_inventory

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── inventory_levels ─────────────────────────────────────────────────────────
-- Real-time stock levels tracked at warehouse level (productId + warehouseId)
CREATE TABLE inventory_levels (
    id                 VARCHAR(36)      PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    product_id         VARCHAR(36)      NOT NULL,
    warehouse_id       VARCHAR(36)      NOT NULL,
    quantity_on_hand   INT              NOT NULL DEFAULT 0,
    quantity_reserved  INT              NOT NULL DEFAULT 0,
    quantity_damaged   INT              NOT NULL DEFAULT 0,
    unit_cost          DECIMAL(12, 2)   NOT NULL DEFAULT 0,
    reorder_point      INT              DEFAULT 0,
    reorder_quantity   INT              DEFAULT 0,
    max_stock          INT,
    last_moved_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_counted_at    TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_warehouse    UNIQUE (product_id, warehouse_id),
    CONSTRAINT ck_non_negative_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_non_negative_reserved CHECK (quantity_reserved >= 0),
    CONSTRAINT ck_non_negative_damaged  CHECK (quantity_damaged >= 0)
);

CREATE INDEX idx_inv_levels_product   ON inventory_levels(product_id);
CREATE INDEX idx_inv_levels_warehouse ON inventory_levels(warehouse_id);
CREATE INDEX idx_inv_levels_low_stock ON inventory_levels(warehouse_id, quantity_on_hand)
    WHERE reorder_point > 0;

-- ─── stock_movements ──────────────────────────────────────────────────────────
-- Immutable ledger — no UPDATE or DELETE after insert
CREATE TABLE stock_movements (
    id               VARCHAR(36)      PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    movement_type    VARCHAR(50)      NOT NULL,
    product_id       VARCHAR(36)      NOT NULL,
    warehouse_id     VARCHAR(36)      NOT NULL,
    quantity         INT              NOT NULL,
    unit_cost        DECIMAL(12, 2),
    movement_total   DECIMAL(15, 2),
    previous_balance INT,
    new_balance      INT,
    reference_id     VARCHAR(36),
    reference_type   VARCHAR(100),
    movement_reason  VARCHAR(200),
    notes            TEXT,
    actor_id         VARCHAR(36)      NOT NULL,
    correlation_id   VARCHAR(36),
    timestamp        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_valid_movement_type CHECK (movement_type IN
        ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'DAMAGE', 'RETURN', 'WASTE')),
    CONSTRAINT ck_positive_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_sm_product        ON stock_movements(product_id);
CREATE INDEX idx_sm_warehouse      ON stock_movements(warehouse_id);
CREATE INDEX idx_sm_type_timestamp ON stock_movements(movement_type, timestamp DESC);
CREATE INDEX idx_sm_reference      ON stock_movements(reference_id, reference_type);
CREATE INDEX idx_sm_actor          ON stock_movements(actor_id);

-- Immutability rule
CREATE RULE no_update_stock_movements AS ON UPDATE TO stock_movements DO INSTEAD NOTHING;
CREATE RULE no_delete_stock_movements AS ON DELETE TO stock_movements DO INSTEAD NOTHING;

-- ─── stock_in ─────────────────────────────────────────────────────────────────
CREATE TABLE stock_in (
    id                VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    stock_movement_id VARCHAR(36)    NOT NULL REFERENCES stock_movements(id),
    supplier_id       VARCHAR(36),
    purchase_order_id VARCHAR(36),
    receipt_number    VARCHAR(100),
    quantity_received INT            NOT NULL,
    quantity_accepted INT            NOT NULL,
    quantity_rejected INT            DEFAULT 0,
    received_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_by       VARCHAR(36)    NOT NULL,
    inspection_status VARCHAR(50)    DEFAULT 'PENDING',
    inspection_notes  TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_in_movement  ON stock_in(stock_movement_id);
CREATE INDEX idx_stock_in_supplier  ON stock_in(supplier_id);

-- ─── stock_out ────────────────────────────────────────────────────────────────
CREATE TABLE stock_out (
    id                VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    stock_movement_id VARCHAR(36)    NOT NULL REFERENCES stock_movements(id),
    order_id          VARCHAR(36),
    customer_id       VARCHAR(36),
    shipped_at        TIMESTAMP WITH TIME ZONE,
    shipped_by        VARCHAR(36),
    delivery_date     TIMESTAMP WITH TIME ZONE,
    destination_address TEXT,
    tracking_number   VARCHAR(255),
    is_back_order     BOOLEAN        DEFAULT false,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stock_out_movement  ON stock_out(stock_movement_id);
CREATE INDEX idx_stock_out_order     ON stock_out(order_id);
CREATE INDEX idx_stock_out_customer  ON stock_out(customer_id);

-- ─── stock_transfer ───────────────────────────────────────────────────────────
CREATE TABLE stock_transfer (
    id                    VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    stock_movement_id     VARCHAR(36)  NOT NULL REFERENCES stock_movements(id),
    from_warehouse_id     VARCHAR(36)  NOT NULL,
    to_warehouse_id       VARCHAR(36)  NOT NULL,
    transfer_status       VARCHAR(50)  DEFAULT 'PENDING',
    transfer_reason       VARCHAR(100),
    from_stock_before     INT,
    from_stock_after      INT,
    to_stock_before       INT,
    to_stock_after        INT,
    shipped_by            VARCHAR(36),
    shipped_at            TIMESTAMP WITH TIME ZONE,
    received_at           TIMESTAMP WITH TIME ZONE,
    received_by           VARCHAR(36),
    notes                 TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_valid_transfer_status CHECK (transfer_status IN ('PENDING', 'SHIPPED', 'RECEIVED', 'CANCELLED', 'CREATED'))
);

CREATE INDEX idx_transfer_movement  ON stock_transfer(stock_movement_id);
CREATE INDEX idx_transfer_from_wh   ON stock_transfer(from_warehouse_id);
CREATE INDEX idx_transfer_to_wh     ON stock_transfer(to_warehouse_id);
CREATE INDEX idx_transfer_status    ON stock_transfer(transfer_status);

-- ─── stock_adjustments ────────────────────────────────────────────────────────
CREATE TABLE stock_adjustments (
    id                VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    stock_movement_id VARCHAR(36)    NOT NULL REFERENCES stock_movements(id),
    adjustment_type   VARCHAR(50)    NOT NULL,
    adjustment_reason VARCHAR(200)   NOT NULL,
    adjustment_quantity INT          NOT NULL,
    previous_quantity INT,
    new_quantity      INT,
    adjusted_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    adjusted_by       VARCHAR(36)    NOT NULL,
    approval_status   VARCHAR(50)    DEFAULT 'PENDING',
    approved_by       VARCHAR(36),
    approved_at       TIMESTAMP WITH TIME ZONE,
    approver_notes    TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_valid_adj_type CHECK (adjustment_type IN
        ('SHRINKAGE', 'CORRECTION', 'COUNT_VARIANCE', 'WRITE_OFF', 'VARIANCE')),
    CONSTRAINT ck_valid_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_adj_movement ON stock_adjustments(stock_movement_id);
CREATE INDEX idx_adj_type     ON stock_adjustments(adjustment_type);
CREATE INDEX idx_adj_status   ON stock_adjustments(approval_status);

-- ─── damaged_inventory ────────────────────────────────────────────────────────
CREATE TABLE damaged_inventory (
    id                   VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    product_id           VARCHAR(36)    NOT NULL,
    warehouse_id         VARCHAR(36)    NOT NULL,
    stock_movement_id    VARCHAR(36),
    quantity             INT            NOT NULL,
    damage_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    damage_reason        VARCHAR(200)   NOT NULL,
    reported_by          VARCHAR(36)    NOT NULL,
    damage_severity      VARCHAR(50)    NOT NULL,
    salvage_value        DECIMAL(12, 2),
    is_resaleable        BOOLEAN        DEFAULT false,
    action_taken         VARCHAR(200),
    resolved_at          TIMESTAMP WITH TIME ZONE,
    resolved_by          VARCHAR(36),
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_damage_severity CHECK (damage_severity IN ('MINOR', 'MODERATE', 'SEVERE', 'TOTAL_LOSS')),
    CONSTRAINT ck_damaged_positive_qty CHECK (quantity > 0)
);

CREATE INDEX idx_damaged_product   ON damaged_inventory(product_id);
CREATE INDEX idx_damaged_warehouse ON damaged_inventory(warehouse_id);
CREATE INDEX idx_damaged_severity  ON damaged_inventory(damage_severity);

-- ─── inventory_holds ──────────────────────────────────────────────────────────
CREATE TABLE inventory_holds (
    id            VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    product_id    VARCHAR(36)    NOT NULL,
    warehouse_id  VARCHAR(36)    NOT NULL,
    quantity_held INT            NOT NULL,
    hold_reason   VARCHAR(100)   NOT NULL,
    order_id      VARCHAR(36),
    customer_id   VARCHAR(36),
    held_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    held_by       VARCHAR(36)    NOT NULL,
    release_date  TIMESTAMP WITH TIME ZONE,
    released_at   TIMESTAMP WITH TIME ZONE,
    released_by   VARCHAR(36),
    status        VARCHAR(50)    DEFAULT 'ACTIVE',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_holds_positive_qty CHECK (quantity_held > 0)
);

CREATE INDEX idx_holds_product   ON inventory_holds(product_id);
CREATE INDEX idx_holds_warehouse ON inventory_holds(warehouse_id);
CREATE INDEX idx_holds_order     ON inventory_holds(order_id);
CREATE INDEX idx_holds_active    ON inventory_holds(product_id, warehouse_id) WHERE released_at IS NULL;

-- ─── inventory_counts ─────────────────────────────────────────────────────────
CREATE TABLE inventory_counts (
    id                  VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    warehouse_id        VARCHAR(36)    NOT NULL,
    count_type          VARCHAR(50)    NOT NULL,
    name                VARCHAR(255)   NOT NULL,
    count_date          DATE           NOT NULL,
    count_reason        VARCHAR(100),
    status              VARCHAR(50)    NOT NULL DEFAULT 'IN_PROGRESS',
    expected_duration   VARCHAR(100),
    count_team          TEXT,
    total_items_counted INT            DEFAULT 0,
    total_variances     INT            DEFAULT 0,
    adjustments_created INT            DEFAULT 0,
    variance_rate       DECIMAL(8, 2),
    created_by          VARCHAR(36)    NOT NULL,
    completed_by        VARCHAR(36),
    approver_comments   TEXT,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_count_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_count_type   CHECK (count_type IN ('FULL', 'PARTIAL', 'CYCLE'))
);

CREATE INDEX idx_counts_warehouse ON inventory_counts(warehouse_id);
CREATE INDEX idx_counts_status    ON inventory_counts(status);

-- ─── inventory_count_items ────────────────────────────────────────────────────
CREATE TABLE inventory_count_items (
    id               VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    count_id         VARCHAR(36)    NOT NULL REFERENCES inventory_counts(id) ON DELETE CASCADE,
    product_id       VARCHAR(36)    NOT NULL,
    system_quantity  INT            NOT NULL,
    counted_quantity INT            NOT NULL,
    location         VARCHAR(255),
    condition        VARCHAR(50),
    recorded_by      VARCHAR(36)    NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_count_items_count   ON inventory_count_items(count_id);
CREATE INDEX idx_count_items_product ON inventory_count_items(product_id);

-- ─── inventory_snapshots ──────────────────────────────────────────────────────
-- Daily analytics snapshots (populated by scheduled job; not exposed via API in M3)
CREATE TABLE inventory_snapshots (
    id                VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    snapshot_date     DATE           NOT NULL,
    product_id        VARCHAR(36)    NOT NULL,
    warehouse_id      VARCHAR(36)    NOT NULL,
    quantity_on_hand  INT            NOT NULL,
    quantity_reserved INT            NOT NULL,
    inventory_value   DECIMAL(15, 2) NOT NULL,
    unit_cost         DECIMAL(12, 2) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_snapshot UNIQUE (snapshot_date, product_id, warehouse_id)
);

CREATE INDEX idx_snapshots_date      ON inventory_snapshots(snapshot_date DESC);
CREATE INDEX idx_snapshots_product   ON inventory_snapshots(product_id);
CREATE INDEX idx_snapshots_warehouse ON inventory_snapshots(warehouse_id);

-- ─── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id             VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    event_type     VARCHAR(100)   NOT NULL,
    entity_type    VARCHAR(100)   NOT NULL,
    entity_id      VARCHAR(36)    NOT NULL,
    actor_id       VARCHAR(36)    NOT NULL,
    action_type    VARCHAR(50)    NOT NULL,
    old_values     JSONB,
    new_values     JSONB,
    timestamp      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(36),
    request_id     VARCHAR(36),
    CONSTRAINT ck_audit_action CHECK (action_type IN
        ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'DAMAGE_REPORTED',
         'HOLD_CREATED', 'HOLD_RELEASED'))
);

CREATE INDEX idx_audit_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_actor     ON audit_logs(actor_id);

-- Immutability rules on audit_logs
CREATE RULE no_update_audit_logs AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_logs AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
