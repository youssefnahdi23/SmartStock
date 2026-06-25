-- Warehouse Service — V3: warehouse_aisles and warehouse_equipment tables

-- ─── warehouse_aisles ─────────────────────────────────────────────────────────

CREATE TABLE warehouse_aisles (
    id                      VARCHAR(36) PRIMARY KEY,
    warehouse_zone_id       VARCHAR(36) NOT NULL REFERENCES warehouse_zones(id) ON DELETE CASCADE,
    aisle_number            INT         NOT NULL,
    aisle_length_m          DECIMAL(8, 2),
    shelf_count             INT         NOT NULL DEFAULT 0,
    total_bins              INT         NOT NULL DEFAULT 0,
    is_active               BOOLEAN     NOT NULL DEFAULT true,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_aisle_per_zone UNIQUE (warehouse_zone_id, aisle_number)
);

CREATE INDEX idx_aisles_zone     ON warehouse_aisles(warehouse_zone_id);
CREATE INDEX idx_aisles_number   ON warehouse_aisles(aisle_number);

-- ─── warehouse_equipment ──────────────────────────────────────────────────────

CREATE TABLE warehouse_equipment (
    id                      VARCHAR(36) PRIMARY KEY,
    warehouse_id            VARCHAR(36) NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    equipment_type          VARCHAR(100) NOT NULL,
    equipment_name          VARCHAR(255) NOT NULL,
    serial_number           VARCHAR(255) UNIQUE,
    manufacturer            VARCHAR(255),
    model_number            VARCHAR(255),
    purchase_date           DATE,
    last_maintenance_at     TIMESTAMP WITH TIME ZONE,
    next_maintenance_due    DATE,
    maintenance_status      VARCHAR(50)  NOT NULL DEFAULT 'OPERATIONAL',
    is_active               BOOLEAN      NOT NULL DEFAULT true,
    location_zone_id        VARCHAR(36)  REFERENCES warehouse_zones(id),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(36)  NOT NULL,
    CONSTRAINT chk_maintenance_status CHECK (maintenance_status IN ('OPERATIONAL','MAINTENANCE_SCHEDULED','IN_MAINTENANCE','OUT_OF_SERVICE'))
);

CREATE INDEX idx_equipment_warehouse         ON warehouse_equipment(warehouse_id);
CREATE INDEX idx_equipment_type              ON warehouse_equipment(equipment_type);
CREATE INDEX idx_equipment_maintenance_status ON warehouse_equipment(maintenance_status);
