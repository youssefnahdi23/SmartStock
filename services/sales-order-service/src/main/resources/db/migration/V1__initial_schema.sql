-- Sales Order Service — Full Schema
-- Port 8008 | DB: sales_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── sales_orders ─────────────────────────────────────────────────────────────
CREATE TABLE sales_orders (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    so_number               VARCHAR(100)      NOT NULL,
    customer_id             VARCHAR(36)       NOT NULL,
    customer_name           VARCHAR(255),
    order_date              DATE,
    due_date                DATE,
    picking_warehouse_id    VARCHAR(36),
    status                  VARCHAR(50)       NOT NULL DEFAULT 'CREATED',
    fulfillment_status      VARCHAR(50)       DEFAULT 'PENDING',
    payment_status          VARCHAR(50)       DEFAULT 'UNPAID',
    shipping_address        VARCHAR(500),
    shipping_method         VARCHAR(50),
    payment_terms           VARCHAR(100),
    total_quantity          INT               DEFAULT 0,
    picked_quantity         INT               DEFAULT 0,
    shipped_quantity        INT               DEFAULT 0,
    total_line_amount       DECIMAL(15, 2)    DEFAULT 0,
    discount_amount         DECIMAL(15, 2)    DEFAULT 0,
    tax_amount              DECIMAL(15, 2)    DEFAULT 0,
    total_amount            DECIMAL(15, 2)    DEFAULT 0,
    paid_amount             DECIMAL(15, 2)    DEFAULT 0,
    confirmation_date       TIMESTAMP WITH TIME ZONE,
    cancelled_at            TIMESTAMP WITH TIME ZONE,
    cancellation_reason     TEXT,
    notes                   TEXT,
    created_by              VARCHAR(36)       NOT NULL,
    updated_by              VARCHAR(36)       NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_so_number             UNIQUE (so_number),
    CONSTRAINT ck_so_status             CHECK (status IN (
        'CREATED', 'CONFIRMED', 'PICKING', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_so_fulfillment_status CHECK (fulfillment_status IN (
        'PENDING', 'PICKING', 'PICKING_COMPLETE', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_so_payment_status     CHECK (payment_status IN (
        'UNPAID', 'PARTIALLY_PAID', 'PAID'
    )),
    CONSTRAINT ck_so_non_negative_amounts CHECK (
        total_amount >= 0 AND discount_amount >= 0 AND tax_amount >= 0
    )
);

CREATE INDEX idx_sales_orders_number          ON sales_orders(so_number);
CREATE INDEX idx_sales_orders_customer        ON sales_orders(customer_id);
CREATE INDEX idx_sales_orders_status          ON sales_orders(status);
CREATE INDEX idx_sales_orders_fulfillment     ON sales_orders(fulfillment_status);
CREATE INDEX idx_sales_orders_warehouse       ON sales_orders(picking_warehouse_id);
CREATE INDEX idx_sales_orders_order_date      ON sales_orders(order_date DESC);
CREATE INDEX idx_sales_orders_due_date        ON sales_orders(due_date);
CREATE INDEX idx_sales_orders_created         ON sales_orders(created_at DESC);
CREATE INDEX idx_sales_orders_customer_date   ON sales_orders(customer_id, order_date DESC);
CREATE INDEX idx_sales_orders_status_date     ON sales_orders(status, order_date DESC);

-- ─── so_line_items ────────────────────────────────────────────────────────────
CREATE TABLE so_line_items (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    sales_order_id          VARCHAR(36)       NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    product_id              VARCHAR(36)       NOT NULL,
    product_name            VARCHAR(255),
    quantity_ordered        INT               NOT NULL,
    quantity_picked         INT               NOT NULL DEFAULT 0,
    quantity_shipped        INT               NOT NULL DEFAULT 0,
    unit_price              DECIMAL(12, 2)    NOT NULL,
    discount_percentage     DECIMAL(5, 2)     DEFAULT 0,
    line_total              DECIMAL(15, 2),
    status                  VARCHAR(50)       DEFAULT 'PENDING',
    bin_location            VARCHAR(255),
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_so_line_status            CHECK (status IN (
        'PENDING', 'PICKED', 'SHIPPED', 'DELIVERED', 'CANCELLED'
    )),
    CONSTRAINT ck_so_positive_qty_ordered   CHECK (quantity_ordered > 0),
    CONSTRAINT ck_so_positive_unit_price    CHECK (unit_price > 0),
    CONSTRAINT ck_so_non_negative_picked    CHECK (quantity_picked >= 0),
    CONSTRAINT ck_so_non_negative_shipped   CHECK (quantity_shipped >= 0)
);

CREATE INDEX idx_so_line_items_order    ON so_line_items(sales_order_id);
CREATE INDEX idx_so_line_items_product  ON so_line_items(product_id);
CREATE INDEX idx_so_line_items_status   ON so_line_items(sales_order_id, status);

-- ─── order_shipments ──────────────────────────────────────────────────────────
CREATE TABLE order_shipments (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    sales_order_id          VARCHAR(36)       NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    shipment_number         VARCHAR(100)      NOT NULL,
    carrier_name            VARCHAR(255),
    tracking_number         VARCHAR(255),
    shipping_method         VARCHAR(100),
    shipped_quantity        INT               DEFAULT 0,
    ship_date               DATE,
    estimated_delivery_date DATE,
    actual_delivery_date    DATE,
    status                  VARCHAR(50)       DEFAULT 'SHIPPED',
    shipped_by              VARCHAR(36),
    signed_by               VARCHAR(255),
    delivery_notes          TEXT,
    shipped_at              TIMESTAMP WITH TIME ZONE,
    delivered_at            TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shipment_number           UNIQUE (shipment_number),
    CONSTRAINT ck_shipment_status           CHECK (status IN (
        'SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION', 'RETURNED'
    ))
);

CREATE INDEX idx_shipments_order        ON order_shipments(sales_order_id);
CREATE INDEX idx_shipments_tracking     ON order_shipments(tracking_number);
CREATE INDEX idx_shipments_status       ON order_shipments(status);
CREATE INDEX idx_shipments_ship_date    ON order_shipments(ship_date DESC);

-- ─── order_returns ────────────────────────────────────────────────────────────
CREATE TABLE order_returns (
    id                      VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    sales_order_id          VARCHAR(36)       NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    return_number           VARCHAR(100)      NOT NULL,
    return_reason           VARCHAR(500)      NOT NULL,
    status                  VARCHAR(50)       DEFAULT 'INITIATED',
    items_returned          INT               DEFAULT 0,
    refund_amount           DECIMAL(15, 2)    DEFAULT 0,
    carrier_name            VARCHAR(255),
    tracking_number         VARCHAR(255),
    notes                   TEXT,
    requested_by            VARCHAR(36)       NOT NULL,
    approved_by             VARCHAR(36),
    approved_at             TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_return_number             UNIQUE (return_number),
    CONSTRAINT ck_return_status             CHECK (status IN (
        'INITIATED', 'AUTHORIZED', 'SHIPPED', 'RECEIVED', 'APPROVED', 'REJECTED'
    ))
);

CREATE INDEX idx_returns_order   ON order_returns(sales_order_id);
CREATE INDEX idx_returns_number  ON order_returns(return_number);
CREATE INDEX idx_returns_status  ON order_returns(status);

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
    CONSTRAINT ck_audit_action CHECK (action_type IN (
        'CREATE', 'UPDATE', 'CONFIRM', 'SHIP', 'DELIVER', 'CANCEL', 'RETURN', 'PICK'
    ))
);

CREATE INDEX idx_audit_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_actor     ON audit_logs(actor_id);

CREATE RULE no_update_audit_logs AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_logs AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
