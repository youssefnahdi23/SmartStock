# Database Specification: Warehouse Service

**Service**: Warehouse Service  
**Purpose**: Warehouse master data, locations, capacity, and operations  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Warehouse Service manages warehouse master data, facilities, zones, and operational metrics.

### High-Level Architecture
```
warehouses
├── warehouse_zones (1:M)
├── warehouse_sections (1:M)
├── warehouse_equipment (1:M)
├── warehouse_operations (1:M)
└── warehouse_metrics (1:M)
```

---

## 2. Tables Specification

### 2.1 `warehouses` Table
**Purpose**: Store warehouse master data

```sql
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_code VARCHAR(100) UNIQUE NOT NULL,
    warehouse_name VARCHAR(255) NOT NULL,
    description TEXT,
    location_address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code VARCHAR(2) NOT NULL DEFAULT 'US',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(255),
    manager_id UUID,
    total_area_sqm DECIMAL(12, 2),
    total_capacity_units INT,
    current_utilization_percentage DECIMAL(5, 2) DEFAULT 0,
    warehouse_type VARCHAR(50) DEFAULT 'GENERAL',
    is_active BOOLEAN DEFAULT true,
    temperature_controlled BOOLEAN DEFAULT false,
    min_temperature DECIMAL(5, 2),
    max_temperature DECIMAL(5, 2),
    hazmat_capable BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT warehouse_name_not_empty CHECK (warehouse_name != ''),
    CONSTRAINT valid_warehouse_type CHECK (warehouse_type IN ('GENERAL', 'COLD_STORAGE', 'HAZMAT', 'DISTRIBUTION', 'FULFILLMENT'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: warehouse_code, city, warehouse_type, is_active
**Analytics**: Warehouse utilization, geographic distribution

---

### 2.2 `warehouse_zones` Table
**Purpose**: Define warehouse zones (logical divisions)

```sql
CREATE TABLE warehouse_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    zone_code VARCHAR(100) NOT NULL,
    zone_name VARCHAR(255) NOT NULL,
    zone_type VARCHAR(50) NOT NULL,
    area_sqm DECIMAL(12, 2),
    capacity_units INT,
    current_utilization INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    temperature_controlled BOOLEAN DEFAULT false,
    min_temperature DECIMAL(5, 2),
    max_temperature DECIMAL(5, 2),
    aisle_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT unique_zone_code UNIQUE (warehouse_id, zone_code),
    CONSTRAINT valid_zone_type CHECK (zone_type IN ('RECEIVING', 'STORAGE', 'PICKING', 'PACKING', 'SHIPPING', 'RETURNS', 'QUALITY'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: warehouse_id, zone_code, zone_type
**Analytics**: Zone utilization, zone type distribution

---

### 2.3 `warehouse_aisles` Table
**Purpose**: Define warehouse aisles within zones

```sql
CREATE TABLE warehouse_aisles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_zone_id UUID NOT NULL REFERENCES warehouse_zones(id) ON DELETE CASCADE,
    aisle_number INT NOT NULL,
    aisle_length_m DECIMAL(8, 2),
    shelf_count INT DEFAULT 0,
    total_bins INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_aisle_per_zone UNIQUE (warehouse_zone_id, aisle_number)
);
```

**Audit Fields**: created_at, updated_at
**Indexes**: warehouse_zone_id, aisle_number

---

### 2.4 `warehouse_shelves` Table
**Purpose**: Define warehouse shelves within aisles

```sql
CREATE TABLE warehouse_shelves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_aisle_id UUID NOT NULL REFERENCES warehouse_aisles(id) ON DELETE CASCADE,
    shelf_level INT NOT NULL,
    capacity_units INT DEFAULT 0,
    current_load_units INT DEFAULT 0,
    max_weight_kg DECIMAL(12, 2),
    current_weight_kg DECIMAL(12, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_shelf_per_aisle UNIQUE (warehouse_aisle_id, shelf_level),
    CONSTRAINT shelf_level_positive CHECK (shelf_level > 0)
);
```

**Audit Fields**: created_at, updated_at
**Indexes**: warehouse_aisle_id, shelf_level

---

### 2.5 `warehouse_bins` Table
**Purpose**: Define individual storage bins

```sql
CREATE TABLE warehouse_bins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_shelf_id UUID NOT NULL REFERENCES warehouse_shelves(id) ON DELETE CASCADE,
    bin_number INT NOT NULL,
    bin_code VARCHAR(100) UNIQUE NOT NULL,
    bin_position VARCHAR(50),
    capacity_units INT DEFAULT 0,
    current_units INT DEFAULT 0,
    max_weight_kg DECIMAL(12, 2),
    current_weight_kg DECIMAL(12, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    is_full BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_bin_per_shelf UNIQUE (warehouse_shelf_id, bin_number)
);
```

**Audit Fields**: created_at, updated_at
**Indexes**: bin_code, warehouse_shelf_id, is_full
**Analytics**: Bin utilization, capacity analysis

---

### 2.6 `warehouse_equipment` Table
**Purpose**: Track warehouse equipment and machinery

```sql
CREATE TABLE warehouse_equipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    equipment_type VARCHAR(100) NOT NULL,
    equipment_name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(255) UNIQUE,
    manufacturer VARCHAR(255),
    model_number VARCHAR(255),
    purchase_date DATE,
    last_maintenance_at TIMESTAMP WITH TIME ZONE,
    next_maintenance_due DATE,
    maintenance_status VARCHAR(50) DEFAULT 'OPERATIONAL',
    is_active BOOLEAN DEFAULT true,
    location_zone_id UUID REFERENCES warehouse_zones(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT valid_maintenance_status CHECK (maintenance_status IN ('OPERATIONAL', 'MAINTENANCE_SCHEDULED', 'IN_MAINTENANCE', 'OUT_OF_SERVICE'))
);
```

**Audit Fields**: created_at, updated_at, created_by, last_maintenance_at
**Indexes**: warehouse_id, equipment_type, maintenance_status
**Analytics**: Equipment maintenance tracking

---

### 2.7 `warehouse_staff` Table
**Purpose**: Track warehouse staff assignments

```sql
CREATE TABLE warehouse_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(100) NOT NULL,
    shift_type VARCHAR(50) DEFAULT 'DAY',
    hourly_rate DECIMAL(8, 2),
    is_active BOOLEAN DEFAULT true,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by UUID NOT NULL,
    unassigned_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_user_warehouse UNIQUE (warehouse_id, user_id) WHERE unassigned_at IS NULL
);
```

**Audit Fields**: assigned_at, assigned_by, unassigned_at, created_at, updated_at
**Indexes**: warehouse_id, user_id, role, is_active

---

### 2.8 `warehouse_operations` Table
**Purpose**: Track daily warehouse operations

```sql
CREATE TABLE warehouse_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    operation_date DATE NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    inbound_transactions INT DEFAULT 0,
    outbound_transactions INT DEFAULT 0,
    transfers_completed INT DEFAULT 0,
    cycle_counts_completed INT DEFAULT 0,
    full_inventory_count BOOLEAN DEFAULT false,
    discrepancies_found INT DEFAULT 0,
    discrepancies_resolved INT DEFAULT 0,
    staff_hours_worked DECIMAL(8, 2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: warehouse_id, operation_date, operation_type
**Analytics**: Warehouse throughput, operational efficiency

---

### 2.9 `warehouse_metrics` Table
**Purpose**: Daily warehouse performance metrics

```sql
CREATE TABLE warehouse_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    total_inventory_units INT,
    total_inventory_value DECIMAL(15, 2),
    average_bin_utilization DECIMAL(5, 2),
    average_zone_utilization DECIMAL(5, 2),
    stock_accuracy_percentage DECIMAL(5, 2),
    order_fulfillment_rate DECIMAL(5, 2),
    order_processing_time_hours DECIMAL(8, 2),
    labor_cost_per_unit DECIMAL(8, 2),
    damaged_units INT,
    shrinkage_percentage DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_metric UNIQUE (warehouse_id, metric_date)
);
```

**Audit Fields**: created_at
**Indexes**: warehouse_id, metric_date
**Analytics**: Warehouse performance trends, KPI tracking

---

### 2.10 `audit_logs` Table
**Purpose**: Immutable audit trail of warehouse changes

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'ZONE_CONFIGURED', 'CAPACITY_UPDATED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE operations
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
warehouses (1) ----→ (M) warehouse_zones ----→ (M) warehouse_aisles ----→ (M) warehouse_shelves ----→ (M) warehouse_bins
         ├──→ (M) warehouse_equipment
         ├──→ (M) warehouse_staff
         ├──→ (M) warehouse_operations
         ├──→ (M) warehouse_metrics
         └──→ (M) audit_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_warehouses_code ON warehouses(warehouse_code);
CREATE INDEX idx_warehouses_type ON warehouses(warehouse_type);
CREATE INDEX idx_zones_warehouse ON warehouse_zones(warehouse_id);
CREATE INDEX idx_bins_code ON warehouse_bins(bin_code);
CREATE INDEX idx_equipment_warehouse ON warehouse_equipment(warehouse_id);
CREATE INDEX idx_metrics_date ON warehouse_metrics(warehouse_id, metric_date DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_staff_warehouse_active ON warehouse_staff(warehouse_id, is_active);
CREATE INDEX idx_operations_daily ON warehouse_operations(warehouse_id, operation_date DESC);
```

---

## 5. Constraints & Business Rules

### Hierarchical Constraints
```sql
-- Warehouse → Zone → Aisle → Shelf → Bin hierarchy is immutable
-- Capacity cannot exceed parent container
```

### Business Rules
```sql
-- Zone utilization = SUM(aisle utilization)
-- Warehouse utilization = SUM(zone utilization) / total_capacity
-- Equipment maintenance triggers alerts
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V4.0__Initialize_warehouse_schema.sql
V4.1__Add_warehouse_hierarchy.sql
V4.2__Add_equipment_and_staff.sql
V4.3__Add_operations_and_metrics.sql
V4.4__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily warehouse utilization snapshots
- Equipment maintenance records
- Staff productivity metrics
- Zone efficiency metrics
- Bin utilization trends

### ML Feature Inputs
- Warehouse capacity forecasting
- Equipment failure prediction
- Staff shift optimization
- Zone recommendation engine
- Congestion pattern detection

### Business Intelligence
- Warehouse KPI dashboards
- Utilization trend analysis
- Equipment efficiency metrics
- Staff productivity analysis
- Operational cost analysis

---

## 8. Scalability Considerations

### Partitioning Strategy

**warehouse_metrics (Time-based)**:
```sql
PARTITION BY RANGE (metric_date)
```

**warehouse_operations (Time-based)**:
```sql
PARTITION BY RANGE (operation_date)
```

---

## 9. Monitoring & Observability

### Key Metrics
- Overall warehouse utilization %
- Zone utilization %
- Equipment availability %
- Staff utilization rate
- Order fulfillment rate

### Alerts
- Warehouse capacity threshold exceeded
- Equipment maintenance overdue
- Stock accuracy below threshold
- Zone congestion detected

---

## Summary

**Total Tables**: 10  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Hierarchy**: Warehouse → Zone → Aisle → Shelf → Bin (5 levels)  
**Analytics-Ready**: Daily operational metrics and KPIs  

