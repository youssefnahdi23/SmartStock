-- Supplier Service — Full Schema
-- Port 8005 | DB: supplier_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── suppliers ────────────────────────────────────────────────────────────────
CREATE TABLE suppliers (
    id                              VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_code                   VARCHAR(100)      NOT NULL,
    supplier_name                   VARCHAR(255)      NOT NULL,
    supplier_type                   VARCHAR(50)       NOT NULL DEFAULT 'VENDOR',
    business_registration_number    VARCHAR(100),
    tax_id                          VARCHAR(100),
    website_url                     VARCHAR(500),
    email_address                   VARCHAR(255),
    phone_number                    VARCHAR(20),
    payment_terms                   VARCHAR(100),
    currency_code                   VARCHAR(3)        DEFAULT 'USD',
    country_code                    VARCHAR(2)        NOT NULL DEFAULT 'US',
    headquarter_address             TEXT,
    city                            VARCHAR(100),
    state_province                  VARCHAR(100),
    postal_code                     VARCHAR(20),
    primary_contact_id              VARCHAR(36),
    account_manager_id              VARCHAR(36),
    credit_limit                    DECIMAL(15, 2),
    average_lead_time_days          INT               DEFAULT 7,
    minimum_order_quantity          INT               DEFAULT 1,
    minimum_order_value             DECIMAL(12, 2),
    is_active                       BOOLEAN           NOT NULL DEFAULT true,
    is_verified                     BOOLEAN           NOT NULL DEFAULT false,
    verification_date               TIMESTAMP WITH TIME ZONE,
    risk_rating                     VARCHAR(50)       DEFAULT 'MEDIUM',
    rating                          DECIMAL(3, 2),
    total_orders                    INT               DEFAULT 0,
    total_spent                     DECIMAL(15, 2)    DEFAULT 0,
    suspension_reason               TEXT,
    suspended_at                    TIMESTAMP WITH TIME ZONE,
    resume_date                     DATE,
    notes                           TEXT,
    certifications                  TEXT,
    created_by                      VARCHAR(36)       NOT NULL,
    updated_by                      VARCHAR(36)       NOT NULL,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_supplier_code         UNIQUE (supplier_code),
    CONSTRAINT ck_supplier_name_not_empty CHECK (supplier_name != ''),
    CONSTRAINT ck_valid_supplier_type   CHECK (supplier_type IN ('VENDOR', 'DISTRIBUTOR', 'MANUFACTURER', 'WHOLESALER', 'AGENT')),
    CONSTRAINT ck_valid_risk_rating     CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_suppliers_code        ON suppliers(supplier_code);
CREATE INDEX idx_suppliers_name        ON suppliers(supplier_name);
CREATE INDEX idx_suppliers_active      ON suppliers(is_active);
CREATE INDEX idx_suppliers_verified    ON suppliers(is_verified);
CREATE INDEX idx_suppliers_risk        ON suppliers(risk_rating);
CREATE INDEX idx_suppliers_type        ON suppliers(supplier_type);
CREATE INDEX idx_suppliers_name_trgm   ON suppliers USING gin(supplier_name gin_trgm_ops);

-- ─── supplier_contacts ────────────────────────────────────────────────────────
CREATE TABLE supplier_contacts (
    id                  VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_id         VARCHAR(36)    NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    contact_name        VARCHAR(255)   NOT NULL,
    contact_title       VARCHAR(100),
    email_address       VARCHAR(255),
    phone_number        VARCHAR(20),
    mobile_number       VARCHAR(20),
    address_line1       TEXT,
    address_line2       TEXT,
    city                VARCHAR(100),
    state_province      VARCHAR(100),
    postal_code         VARCHAR(20),
    contact_type        VARCHAR(50)    DEFAULT 'GENERAL',
    is_primary          BOOLEAN        NOT NULL DEFAULT false,
    is_active           BOOLEAN        NOT NULL DEFAULT true,
    last_contacted_at   TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_contact_name_not_empty CHECK (contact_name != ''),
    CONSTRAINT ck_valid_contact_type CHECK (contact_type IN ('GENERAL', 'SALES', 'BILLING', 'TECHNICAL', 'LOGISTICS'))
);

CREATE INDEX idx_contacts_supplier    ON supplier_contacts(supplier_id);
CREATE INDEX idx_contacts_primary     ON supplier_contacts(supplier_id) WHERE is_primary = true;
CREATE INDEX idx_contacts_type        ON supplier_contacts(contact_type);

-- ─── supplier_products ────────────────────────────────────────────────────────
CREATE TABLE supplier_products (
    id                      VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_id             VARCHAR(36)    NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    product_id              VARCHAR(36)    NOT NULL,
    supplier_product_code   VARCHAR(255),
    unit_price              DECIMAL(12, 2) NOT NULL,
    minimum_order_quantity  INT            DEFAULT 1,
    lead_time_days          INT            DEFAULT 7,
    quality_rating          DECIMAL(3, 2),
    is_active               BOOLEAN        NOT NULL DEFAULT true,
    last_ordered_at         TIMESTAMP WITH TIME ZONE,
    total_quantity_ordered  INT            DEFAULT 0,
    total_spent             DECIMAL(15, 2) DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_supplier_product      UNIQUE (supplier_id, product_id),
    CONSTRAINT ck_positive_unit_price   CHECK (unit_price > 0),
    CONSTRAINT ck_positive_lead_time    CHECK (lead_time_days >= 0)
);

CREATE INDEX idx_supplier_products_supplier ON supplier_products(supplier_id);
CREATE INDEX idx_supplier_products_product  ON supplier_products(product_id);
CREATE INDEX idx_supplier_products_active   ON supplier_products(supplier_id, is_active);

-- ─── supplier_contracts ───────────────────────────────────────────────────────
CREATE TABLE supplier_contracts (
    id                  VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_id         VARCHAR(36)    NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    contract_number     VARCHAR(100)   NOT NULL,
    contract_title      VARCHAR(255)   NOT NULL,
    contract_type       VARCHAR(50)    NOT NULL,
    description         TEXT,
    start_date          DATE           NOT NULL,
    end_date            DATE           NOT NULL,
    renewal_date        DATE,
    contract_value      DECIMAL(15, 2),
    payment_terms       VARCHAR(100),
    discount_percentage DECIMAL(5, 2)  DEFAULT 0,
    minimum_volume      INT,
    contract_status     VARCHAR(50)    DEFAULT 'ACTIVE',
    approval_status     VARCHAR(50)    DEFAULT 'PENDING',
    approved_by         VARCHAR(36),
    approved_at         TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(36)    NOT NULL,
    updated_by          VARCHAR(36)    NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contract_number           UNIQUE (contract_number),
    CONSTRAINT ck_contract_title_not_empty  CHECK (contract_title != ''),
    CONSTRAINT ck_valid_contract_type       CHECK (contract_type IN ('PURCHASE_AGREEMENT', 'MASTER_SUPPLY', 'FRAMEWORK', 'SPOT', 'BLANKET')),
    CONSTRAINT ck_valid_contract_status     CHECK (contract_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED')),
    CONSTRAINT ck_contract_dates            CHECK (start_date <= end_date)
);

CREATE INDEX idx_contracts_supplier ON supplier_contracts(supplier_id);
CREATE INDEX idx_contracts_number   ON supplier_contracts(contract_number);
CREATE INDEX idx_contracts_status   ON supplier_contracts(contract_status);
CREATE INDEX idx_contracts_end_date ON supplier_contracts(end_date);

-- ─── supplier_deliveries ──────────────────────────────────────────────────────
CREATE TABLE supplier_deliveries (
    id                          VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_id                 VARCHAR(36)    NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    purchase_order_id           VARCHAR(36),
    delivery_number             VARCHAR(100)   NOT NULL,
    order_date                  DATE           NOT NULL,
    promised_delivery_date      DATE           NOT NULL,
    actual_delivery_date        DATE,
    quantity_ordered            INT            NOT NULL,
    quantity_received           INT            NOT NULL DEFAULT 0,
    quantity_rejected           INT            DEFAULT 0,
    delivery_status             VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    on_time                     BOOLEAN,
    on_time_days_variance       INT,
    quality_inspection_status   VARCHAR(50)    DEFAULT 'PENDING',
    quality_issues_found        INT            DEFAULT 0,
    quality_rating              DECIMAL(3, 2),
    carrier_name                VARCHAR(255),
    tracking_number             VARCHAR(255),
    total_value                 DECIMAL(15, 2),
    notes                       TEXT,
    received_by                 VARCHAR(36),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_delivery_number           UNIQUE (delivery_number),
    CONSTRAINT ck_valid_delivery_status     CHECK (delivery_status IN ('PENDING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_valid_inspection_status   CHECK (quality_inspection_status IN ('PENDING', 'PASSED', 'FAILED', 'PARTIAL')),
    CONSTRAINT ck_positive_qty_ordered      CHECK (quantity_ordered > 0)
);

CREATE INDEX idx_deliveries_supplier        ON supplier_deliveries(supplier_id);
CREATE INDEX idx_deliveries_number          ON supplier_deliveries(delivery_number);
CREATE INDEX idx_deliveries_status          ON supplier_deliveries(delivery_status);
CREATE INDEX idx_deliveries_order_date      ON supplier_deliveries(order_date DESC);
CREATE INDEX idx_deliveries_promised_date   ON supplier_deliveries(promised_delivery_date);
CREATE INDEX idx_deliveries_supplier_status ON supplier_deliveries(supplier_id, delivery_status, on_time);

-- ─── supplier_metrics ─────────────────────────────────────────────────────────
CREATE TABLE supplier_metrics (
    id                          VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    supplier_id                 VARCHAR(36)    NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    metric_date                 DATE           NOT NULL,
    total_orders                INT            DEFAULT 0,
    total_units_received        INT            DEFAULT 0,
    on_time_deliveries          INT            DEFAULT 0,
    on_time_delivery_rate       DECIMAL(5, 2),
    quality_pass_rate           DECIMAL(5, 2),
    average_quality_rating      DECIMAL(3, 2),
    quality_issues_count        INT            DEFAULT 0,
    order_accuracy_rate         DECIMAL(5, 2),
    average_lead_time_days      DECIMAL(8, 2),
    total_value_received        DECIMAL(15, 2) DEFAULT 0,
    communication_score         DECIMAL(3, 2),
    overall_performance_score   DECIMAL(3, 2),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_supplier_metric UNIQUE (supplier_id, metric_date)
);

CREATE INDEX idx_metrics_supplier ON supplier_metrics(supplier_id);
CREATE INDEX idx_metrics_date     ON supplier_metrics(supplier_id, metric_date DESC);

-- ─── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id              VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    event_type      VARCHAR(100)   NOT NULL,
    entity_type     VARCHAR(100)   NOT NULL,
    entity_id       VARCHAR(36)    NOT NULL,
    actor_id        VARCHAR(36)    NOT NULL,
    action_type     VARCHAR(50)    NOT NULL,
    old_values      JSONB,
    new_values      JSONB,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id  VARCHAR(36),
    request_id      VARCHAR(36),
    CONSTRAINT ck_audit_action CHECK (action_type IN
        ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'VERIFY', 'SUSPEND', 'RESUME'))
);

CREATE INDEX idx_audit_entity    ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_actor     ON audit_logs(actor_id);

-- Immutability rules on audit_logs
CREATE RULE no_update_audit_logs AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_logs AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
