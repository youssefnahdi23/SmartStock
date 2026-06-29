-- Warehouse Service — Full Schema
-- V1: warehouses, zones, shelves, bins, staff, equipment, operations, metrics, audit

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── warehouses ───────────────────────────────────────────────────────────────

CREATE TABLE warehouses (
    id                          VARCHAR(36) PRIMARY KEY,
    warehouse_code              VARCHAR(100) UNIQUE NOT NULL,
    warehouse_name              VARCHAR(255) NOT NULL,
    description                 TEXT,
    warehouse_type              VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    location_address            TEXT,
    city                        VARCHAR(100),
    state_province              VARCHAR(100),
    postal_code                 VARCHAR(20),
    country_code                VARCHAR(2)  NOT NULL DEFAULT 'US',
    latitude                    DECIMAL(10, 8),
    longitude                   DECIMAL(11, 8),
    total_area_sqm              DECIMAL(12, 2),
    total_capacity_units        INT,
    current_utilization_percentage DECIMAL(5, 2) DEFAULT 0,
    max_weight_kg               DECIMAL(12, 2),
    used_weight_kg              DECIMAL(12, 2) DEFAULT 0,
    contact_phone               VARCHAR(20),
    contact_email               VARCHAR(255),
    manager_id                  VARCHAR(36),
    manager_email               VARCHAR(255),
    hours_monday_friday         VARCHAR(20),
    hours_saturday              VARCHAR(20),
    hours_sunday                VARCHAR(20),
    temperature_controlled      BOOLEAN NOT NULL DEFAULT false,
    min_temperature             DECIMAL(5, 2),
    max_temperature             DECIMAL(5, 2),
    hazmat_capable              BOOLEAN NOT NULL DEFAULT false,
    is_active                   BOOLEAN NOT NULL DEFAULT true,
    deactivated_at              TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(36) NOT NULL,
    updated_by                  VARCHAR(36) NOT NULL,
    deleted_at                  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_warehouse_type CHECK (warehouse_type IN ('GENERAL','COLD_STORAGE','HAZMAT','DISTRIBUTION','FULFILLMENT','PRIMARY','SECONDARY','DISTRIBUTION_CENTER'))
);

CREATE INDEX idx_warehouses_code   ON warehouses(warehouse_code);
CREATE INDEX idx_warehouses_type   ON warehouses(warehouse_type);
CREATE INDEX idx_warehouses_active ON warehouses(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_warehouses_city   ON warehouses(city);
CREATE INDEX idx_warehouses_name_trgm ON warehouses USING gin (warehouse_name gin_trgm_ops);

-- ─── warehouse_zones ──────────────────────────────────────────────────────────

CREATE TABLE warehouse_zones (
    id                      VARCHAR(36) PRIMARY KEY,
    warehouse_id            VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    zone_code               VARCHAR(100) NOT NULL,
    zone_name               VARCHAR(255) NOT NULL,
    description             TEXT,
    zone_type               VARCHAR(50) NOT NULL,
    area_sqm                DECIMAL(12, 2),
    capacity_units          INT,
    current_utilization     INT NOT NULL DEFAULT 0,
    temperature_controlled  BOOLEAN NOT NULL DEFAULT false,
    min_temperature         DECIMAL(5, 2),
    max_temperature         DECIMAL(5, 2),
    temperature_unit        VARCHAR(10) DEFAULT 'CELSIUS',
    is_active               BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(36) NOT NULL,
    updated_by              VARCHAR(36) NOT NULL,
    deleted_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_zone_code UNIQUE (warehouse_id, zone_code),
    CONSTRAINT chk_zone_type CHECK (zone_type IN ('RECEIVING','STORAGE','PICKING','PACKING','SHIPPING','RETURNS','QUALITY','COLD_STORAGE','HAZMAT','HIGH_VALUE','STANDARD'))
);

CREATE INDEX idx_zones_warehouse ON warehouse_zones(warehouse_id);
CREATE INDEX idx_zones_type      ON warehouse_zones(zone_type);
CREATE INDEX idx_zones_active    ON warehouse_zones(warehouse_id, is_active) WHERE deleted_at IS NULL;

-- ─── warehouse_shelves ────────────────────────────────────────────────────────

CREATE TABLE warehouse_shelves (
    id                  VARCHAR(36) PRIMARY KEY,
    zone_id             VARCHAR(36) NOT NULL REFERENCES warehouse_zones(id) ON DELETE CASCADE,
    shelf_code          VARCHAR(100) NOT NULL,
    shelf_name          VARCHAR(255) NOT NULL,
    shelf_level         INT,
    capacity_units      INT,
    current_load_units  INT NOT NULL DEFAULT 0,
    max_weight_kg       DECIMAL(12, 2),
    current_weight_kg   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(36) NOT NULL,
    updated_by          VARCHAR(36) NOT NULL,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_shelf_code UNIQUE (zone_id, shelf_code),
    CONSTRAINT chk_shelf_level_positive CHECK (shelf_level IS NULL OR shelf_level > 0)
);

CREATE INDEX idx_shelves_zone   ON warehouse_shelves(zone_id);
CREATE INDEX idx_shelves_active ON warehouse_shelves(zone_id, is_active) WHERE deleted_at IS NULL;

-- ─── warehouse_bins ───────────────────────────────────────────────────────────

CREATE TABLE warehouse_bins (
    id                  VARCHAR(36) PRIMARY KEY,
    shelf_id            VARCHAR(36) NOT NULL REFERENCES warehouse_shelves(id) ON DELETE CASCADE,
    bin_code            VARCHAR(100) UNIQUE NOT NULL,
    bin_name            VARCHAR(255),
    bin_number          INT,
    bin_position        VARCHAR(50),
    bin_type            VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    capacity_units      INT,
    current_units       INT NOT NULL DEFAULT 0,
    max_weight_kg       DECIMAL(12, 2),
    current_weight_kg   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    is_full             BOOLEAN NOT NULL DEFAULT false,
    current_product_id  VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_bin_per_shelf UNIQUE (shelf_id, bin_number),
    CONSTRAINT chk_bin_type CHECK (bin_type IN ('STANDARD','BULK','PALLET','COLD','HAZMAT','HIGH_VALUE'))
);

CREATE INDEX idx_bins_code    ON warehouse_bins(bin_code);
CREATE INDEX idx_bins_shelf   ON warehouse_bins(shelf_id);
CREATE INDEX idx_bins_full    ON warehouse_bins(is_full) WHERE deleted_at IS NULL AND is_active = true;
CREATE INDEX idx_bins_product ON warehouse_bins(current_product_id) WHERE current_product_id IS NOT NULL;

-- ─── warehouse_staff ──────────────────────────────────────────────────────────

CREATE TABLE warehouse_staff (
    id              VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    warehouse_id    VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    user_id         VARCHAR(36) NOT NULL,
    role            VARCHAR(100) NOT NULL,
    shift_type      VARCHAR(50) NOT NULL DEFAULT 'DAY',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    assigned_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by     VARCHAR(36) NOT NULL,
    unassigned_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staff_warehouse_active ON warehouse_staff(warehouse_id, is_active);
CREATE INDEX idx_staff_user             ON warehouse_staff(user_id);

-- ─── warehouse_operations ─────────────────────────────────────────────────────

CREATE TABLE warehouse_operations (
    id                      VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    warehouse_id            VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    operation_date          DATE NOT NULL,
    operation_type          VARCHAR(100) NOT NULL,
    inbound_transactions    INT NOT NULL DEFAULT 0,
    outbound_transactions   INT NOT NULL DEFAULT 0,
    transfers_completed     INT NOT NULL DEFAULT 0,
    cycle_counts_completed  INT NOT NULL DEFAULT 0,
    full_inventory_count    BOOLEAN NOT NULL DEFAULT false,
    discrepancies_found     INT NOT NULL DEFAULT 0,
    discrepancies_resolved  INT NOT NULL DEFAULT 0,
    staff_hours_worked      DECIMAL(8, 2),
    notes                   TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(36) NOT NULL
);

CREATE INDEX idx_operations_daily ON warehouse_operations(warehouse_id, operation_date DESC);

-- ─── warehouse_metrics ────────────────────────────────────────────────────────

CREATE TABLE warehouse_metrics (
    id                          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    warehouse_id                VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    metric_date                 DATE NOT NULL,
    total_inventory_units       INT,
    total_inventory_value       DECIMAL(15, 2),
    average_bin_utilization     DECIMAL(5, 2),
    average_zone_utilization    DECIMAL(5, 2),
    stock_accuracy_percentage   DECIMAL(5, 2),
    order_fulfillment_rate      DECIMAL(5, 2),
    order_processing_time_hours DECIMAL(8, 2),
    labor_cost_per_unit         DECIMAL(8, 2),
    damaged_units               INT,
    shrinkage_percentage        DECIMAL(5, 2),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_metric UNIQUE (warehouse_id, metric_date)
);

CREATE INDEX idx_metrics_date ON warehouse_metrics(warehouse_id, metric_date DESC);

-- ─── audit_logs ───────────────────────────────────────────────────────────────

CREATE TABLE audit_logs (
    id              VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    event_type      VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(36) NOT NULL,
    actor_id        VARCHAR(36) NOT NULL,
    action_type     VARCHAR(50) NOT NULL,
    old_values      JSONB,
    new_values      JSONB,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id  VARCHAR(36),
    request_id      VARCHAR(36),
    CONSTRAINT chk_action_type CHECK (action_type IN ('CREATE','UPDATE','DELETE','ACTIVATE','DEACTIVATE','ZONE_CONFIGURED','CAPACITY_UPDATED'))
);

CREATE INDEX idx_audit_entity   ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time     ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_actor    ON audit_logs(actor_id);

-- Immutability rules
CREATE OR REPLACE RULE audit_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE OR REPLACE RULE audit_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
