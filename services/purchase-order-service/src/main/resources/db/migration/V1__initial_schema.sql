-- Purchase Order Service — Full Schema
-- Port 8007 | DB: purchase_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── purchase_orders ──────────────────────────────────────────────────────────
CREATE TABLE purchase_orders (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    po_number               VARCHAR(100)      NOT NULL,
    supplier_id             VARCHAR(36)       NOT NULL,
    supplier_name           VARCHAR(255),
    order_date              DATE,
    due_date                DATE,
    expected_delivery_date  DATE,
    delivery_warehouse_id   VARCHAR(36),
    status                  VARCHAR(50)       NOT NULL DEFAULT 'CREATED',
    shipping_method         VARCHAR(50),
    payment_terms           VARCHAR(100),
    total_quantity          INT               DEFAULT 0,
    delivered_quantity      INT               DEFAULT 0,
    total_line_amount       DECIMAL(15, 2)    DEFAULT 0,
    discount_amount         DECIMAL(15, 2)    DEFAULT 0,
    tax_amount              DECIMAL(15, 2)    DEFAULT 0,
    total_amount            DECIMAL(15, 2)    DEFAULT 0,
    paid_amount             DECIMAL(15, 2)    DEFAULT 0,
    delivery_status         VARCHAR(50)       DEFAULT 'NOT_RECEIVED',
    payment_status          VARCHAR(50)       DEFAULT 'UNPAID',
    confirmation_date       TIMESTAMP WITH TIME ZONE,
    confirmation_number     VARCHAR(100),
    cancelled_at            TIMESTAMP WITH TIME ZONE,
    cancellation_reason     TEXT,
    notes                   TEXT,
    created_by              VARCHAR(36)       NOT NULL,
    updated_by              VARCHAR(36)       NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_po_number             UNIQUE (po_number),
    CONSTRAINT ck_po_status             CHECK (status IN ('CREATED', 'CONFIRMED', 'SHIPPED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_po_delivery_status    CHECK (delivery_status IN ('NOT_RECEIVED', 'PARTIALLY_RECEIVED', 'FULLY_RECEIVED')),
    CONSTRAINT ck_po_payment_status     CHECK (payment_status IN ('UNPAID', 'PARTIALLY_PAID', 'PAID'))
);

CREATE INDEX idx_purchase_orders_number     ON purchase_orders(po_number);
CREATE INDEX idx_purchase_orders_supplier   ON purchase_orders(supplier_id);
CREATE INDEX idx_purchase_orders_status     ON purchase_orders(status);
CREATE INDEX idx_purchase_orders_warehouse  ON purchase_orders(delivery_warehouse_id);
CREATE INDEX idx_purchase_orders_order_date ON purchase_orders(order_date DESC);
CREATE INDEX idx_purchase_orders_created    ON purchase_orders(created_at DESC);

-- ─── po_line_items ────────────────────────────────────────────────────────────
CREATE TABLE po_line_items (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    purchase_order_id       VARCHAR(36)       NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id              VARCHAR(36)       NOT NULL,
    product_name            VARCHAR(255),
    quantity_ordered        INT               NOT NULL,
    quantity_received       INT               NOT NULL DEFAULT 0,
    unit_price              DECIMAL(12, 2)    NOT NULL,
    discount_percentage     DECIMAL(5, 2)     DEFAULT 0,
    line_total              DECIMAL(15, 2),
    status                  VARCHAR(50)       DEFAULT 'PENDING',
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_line_status           CHECK (status IN ('PENDING', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    CONSTRAINT ck_positive_qty_ordered  CHECK (quantity_ordered > 0),
    CONSTRAINT ck_positive_unit_price   CHECK (unit_price > 0),
    CONSTRAINT ck_non_negative_received CHECK (quantity_received >= 0)
);

CREATE INDEX idx_po_line_items_po       ON po_line_items(purchase_order_id);
CREATE INDEX idx_po_line_items_product  ON po_line_items(product_id);
CREATE INDEX idx_po_line_items_status   ON po_line_items(purchase_order_id, status);

-- ─── delivery_tracking ────────────────────────────────────────────────────────
CREATE TABLE delivery_tracking (
    id                          VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    purchase_order_id           VARCHAR(36)   NOT NULL,
    tracking_number             VARCHAR(255),
    carrier_name                VARCHAR(255),
    estimated_arrival           DATE,
    actual_arrival              DATE,
    delivery_date               DATE,
    total_received_quantity     INT           DEFAULT 0,
    damage_count                INT           DEFAULT 0,
    status                      VARCHAR(50)   DEFAULT 'PENDING',
    delivery_notes              TEXT,
    received_by                 VARCHAR(36),
    received_at                 TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_delivery_status CHECK (status IN ('PENDING', 'IN_TRANSIT', 'DELIVERED', 'DELAYED', 'CANCELLED'))
);

CREATE INDEX idx_delivery_tracking_po       ON delivery_tracking(purchase_order_id);
CREATE INDEX idx_delivery_tracking_status   ON delivery_tracking(status);
CREATE INDEX idx_delivery_tracking_date     ON delivery_tracking(delivery_date DESC);

-- ─── quality_issues ───────────────────────────────────────────────────────────
CREATE TABLE quality_issues (
    id                  VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    purchase_order_id   VARCHAR(36)   NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    line_item_id        VARCHAR(36),
    issue_type          VARCHAR(100)  NOT NULL,
    quantity            INT,
    description         TEXT,
    severity            VARCHAR(50),
    proposed_resolution VARCHAR(100),
    status              VARCHAR(50)   DEFAULT 'OPEN',
    resolution_notes    TEXT,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    resolved_by         VARCHAR(36),
    created_by          VARCHAR(36)   NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_issue_status      CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    CONSTRAINT ck_issue_severity    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') OR severity IS NULL)
);

CREATE INDEX idx_quality_issues_po      ON quality_issues(purchase_order_id);
CREATE INDEX idx_quality_issues_status  ON quality_issues(status);
CREATE INDEX idx_quality_issues_type    ON quality_issues(issue_type);

-- ─── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id              VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    event_type      VARCHAR(100)  NOT NULL,
    entity_type     VARCHAR(100)  NOT NULL,
    entity_id       VARCHAR(36)   NOT NULL,
    actor_id        VARCHAR(36)   NOT NULL,
    action_type     VARCHAR(50)   NOT NULL,
    old_values      JSONB,
    new_values      JSONB,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id  VARCHAR(36),
    request_id      VARCHAR(36),
    CONSTRAINT ck_audit_action CHECK (action_type IN
        ('CREATE', 'UPDATE', 'DELETE', 'CONFIRM', 'CANCEL', 'RECEIVE', 'QUALITY_ISSUE'))
);

CREATE INDEX idx_audit_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_actor     ON audit_logs(actor_id);

CREATE RULE no_update_audit_logs AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_logs AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
