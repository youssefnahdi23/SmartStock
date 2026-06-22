# Database Specification: Inventory Service

**Service**: Inventory Service  
**Purpose**: Real-time stock tracking, inventory movements, and levels  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Inventory Service manages real-time stock levels, inventory movements, adjustments, and damage tracking.

### High-Level Architecture
```
inventory_locations
├── inventory_levels (1:M)
├── stock_movements (1:M)
├── stock_adjustments (1:M)
├── inventory_holds (1:M)
└── damaged_inventory (1:M)

stock_movements (parent for all movement types)
├── stock_in
├── stock_out
├── stock_transfer
└── stock_damage
```

---

## 2. Tables Specification

### 2.1 `inventory_locations` Table
**Purpose**: Define physical warehouse locations (zone/shelf/bin hierarchy)

```sql
CREATE TABLE inventory_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id UUID NOT NULL,
    location_code VARCHAR(100) UNIQUE NOT NULL,
    zone_name VARCHAR(100) NOT NULL,
    aisle_number INT,
    shelf_number INT,
    bin_number INT,
    location_type VARCHAR(50) NOT NULL DEFAULT 'BIN',
    capacity_units INT,
    current_weight_kg DECIMAL(12, 2) DEFAULT 0,
    max_weight_kg DECIMAL(12, 2),
    is_active BOOLEAN DEFAULT true,
    is_restricted BOOLEAN DEFAULT false,
    temperature_controlled BOOLEAN DEFAULT false,
    min_temperature DECIMAL(5, 2),
    max_temperature DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: warehouse_id, location_code, zone_name, is_active
**Analytics**: Location utilization, warehouse capacity analysis

---

### 2.2 `inventory_levels` Table
**Purpose**: Real-time stock levels by product and location

```sql
CREATE TABLE inventory_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    inventory_location_id UUID NOT NULL REFERENCES inventory_locations(id),
    warehouse_id UUID NOT NULL,
    quantity_on_hand INT DEFAULT 0,
    quantity_reserved INT DEFAULT 0,
    quantity_available INT GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    quantity_damaged INT DEFAULT 0,
    unit_cost DECIMAL(12, 2) NOT NULL,
    inventory_value DECIMAL(15, 2) GENERATED ALWAYS AS (quantity_on_hand * unit_cost) STORED,
    last_counted_at TIMESTAMP WITH TIME ZONE,
    last_moved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_product_location UNIQUE (product_id, inventory_location_id),
    CONSTRAINT non_negative_quantities CHECK (quantity_on_hand >= 0 AND quantity_reserved >= 0 AND quantity_damaged >= 0)
);
```

**Audit Fields**: created_at, updated_at, last_moved_at, last_counted_at
**Generated Columns**: quantity_available, inventory_value (auto-calculated)
**Indexes**: product_id, warehouse_id, inventory_location_id, last_moved_at
**Analytics**: Real-time inventory value, stock aging

---

### 2.3 `stock_movements` Table
**Purpose**: Immutable log of all inventory movements (parent table)

```sql
CREATE TABLE stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movement_type VARCHAR(50) NOT NULL,
    product_id UUID NOT NULL,
    from_location_id UUID REFERENCES inventory_locations(id),
    to_location_id UUID REFERENCES inventory_locations(id),
    warehouse_id UUID NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_cost DECIMAL(12, 2),
    movement_total DECIMAL(15, 2),
    reference_id UUID,
    reference_type VARCHAR(100),
    movement_reason VARCHAR(200),
    notes TEXT,
    actor_id UUID NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_movement_type CHECK (movement_type IN ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'DAMAGE', 'RETURN', 'WASTE')),
    CONSTRAINT positive_quantity CHECK (quantity > 0)
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id, request_id
**Immutability**: No UPDATE/DELETE operations allowed
**Indexes**: product_id, warehouse_id, movement_type, timestamp, reference_id
**Partitioning**: By month on timestamp
**Analytics**: Complete transaction log for AI feature engineering

---

### 2.4 `stock_in` Table
**Purpose**: Detailed stock inbound movements (e.g., from suppliers)

```sql
CREATE TABLE stock_in (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_movement_id UUID NOT NULL REFERENCES stock_movements(id),
    supplier_id UUID,
    purchase_order_id UUID,
    receipt_number VARCHAR(100),
    quantity_received INT NOT NULL,
    quantity_accepted INT NOT NULL,
    quantity_rejected INT DEFAULT 0,
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    received_by UUID NOT NULL,
    inspection_status VARCHAR(50) DEFAULT 'PENDING',
    inspection_notes TEXT,
    inspected_at TIMESTAMP WITH TIME ZONE,
    inspected_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT inspection_constraint CHECK (
        CASE WHEN inspection_status = 'COMPLETED' THEN inspected_at IS NOT NULL AND inspected_by IS NOT NULL ELSE true END
    )
);
```

**Audit Fields**: received_at, received_by, inspected_at, inspected_by, created_at, updated_at
**Indexes**: stock_movement_id, supplier_id, purchase_order_id, received_at
**Analytics**: Supplier delivery performance, inbound quality metrics

---

### 2.5 `stock_out` Table
**Purpose**: Detailed stock outbound movements (e.g., to customers, orders)

```sql
CREATE TABLE stock_out (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_movement_id UUID NOT NULL REFERENCES stock_movements(id),
    order_id UUID,
    customer_id UUID,
    shipped_at TIMESTAMP WITH TIME ZONE,
    shipped_by UUID,
    delivery_date TIMESTAMP WITH TIME ZONE,
    destination_address TEXT,
    tracking_number VARCHAR(255),
    is_back_order BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: created_at, updated_at, shipped_at, shipped_by, delivery_date
**Indexes**: order_id, customer_id, shipped_at, is_back_order
**Analytics**: Sales patterns, backorder frequency

---

### 2.6 `stock_transfer` Table
**Purpose**: Internal stock transfers between locations/warehouses

```sql
CREATE TABLE stock_transfer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_movement_id UUID NOT NULL REFERENCES stock_movements(id),
    from_warehouse_id UUID NOT NULL,
    to_warehouse_id UUID NOT NULL,
    transfer_status VARCHAR(50) DEFAULT 'PENDING',
    shipped_at TIMESTAMP WITH TIME ZONE,
    shipped_by UUID,
    received_at TIMESTAMP WITH TIME ZONE,
    received_by UUID,
    expected_delivery_date TIMESTAMP WITH TIME ZONE,
    transfer_reason VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_transfer_status CHECK (transfer_status IN ('PENDING', 'SHIPPED', 'RECEIVED', 'CANCELLED'))
);
```

**Audit Fields**: created_at, updated_at, shipped_at, shipped_by, received_at, received_by
**Indexes**: from_warehouse_id, to_warehouse_id, transfer_status, shipped_at
**Analytics**: Warehouse efficiency, inter-warehouse transfer patterns

---

### 2.7 `stock_adjustments` Table
**Purpose**: Inventory adjustments (shrinkage, correction, physical count)

```sql
CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_movement_id UUID NOT NULL REFERENCES stock_movements(id),
    adjustment_type VARCHAR(50) NOT NULL,
    adjustment_reason VARCHAR(200) NOT NULL,
    adjustment_quantity INT NOT NULL,
    adjusted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    adjusted_by UUID NOT NULL,
    approval_status VARCHAR(50) DEFAULT 'PENDING',
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    approver_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_adjustment_type CHECK (adjustment_type IN ('SHRINKAGE', 'CORRECTION', 'COUNT_VARIANCE', 'WRITE_OFF', 'VARIANCE')),
    CONSTRAINT valid_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
```

**Audit Fields**: adjusted_at, adjusted_by, approved_at, approved_by, created_at, updated_at
**Indexes**: adjustment_type, adjusted_at, approval_status
**Analytics**: Shrinkage analysis, inventory variance tracking

---

### 2.8 `damaged_inventory` Table
**Purpose**: Track damaged or defective inventory

```sql
CREATE TABLE damaged_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    inventory_location_id UUID REFERENCES inventory_locations(id),
    quantity INT NOT NULL,
    damage_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    damage_reason VARCHAR(200) NOT NULL,
    reported_by UUID NOT NULL,
    damage_severity VARCHAR(50) NOT NULL,
    salvage_value DECIMAL(12, 2),
    is_resaleable BOOLEAN DEFAULT false,
    action_taken VARCHAR(200),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_damage_severity CHECK (damage_severity IN ('MINOR', 'MODERATE', 'SEVERE', 'TOTAL_LOSS')),
    CONSTRAINT positive_quantity CHECK (quantity > 0)
);
```

**Audit Fields**: damage_date, reported_by, resolved_at, resolved_by, created_at, updated_at
**Indexes**: product_id, warehouse_id, damage_date, damage_severity
**Analytics**: Damage frequency, product quality issues

---

### 2.9 `inventory_holds` Table
**Purpose**: Track reserved inventory for orders

```sql
CREATE TABLE inventory_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    inventory_location_id UUID REFERENCES inventory_locations(id),
    quantity_held INT NOT NULL,
    hold_reason VARCHAR(100) NOT NULL,
    order_id UUID,
    customer_id UUID,
    held_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    held_by UUID NOT NULL,
    release_date TIMESTAMP WITH TIME ZONE,
    released_at TIMESTAMP WITH TIME ZONE,
    released_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT positive_quantity CHECK (quantity_held > 0)
);
```

**Audit Fields**: held_at, held_by, released_at, released_by, created_at, updated_at
**Indexes**: product_id, warehouse_id, order_id, hold_reason
**Analytics**: Order fulfillment patterns, hold duration analysis

---

### 2.10 `inventory_snapshots` Table
**Purpose**: Daily inventory snapshots for analytics and reporting

```sql
CREATE TABLE inventory_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date DATE NOT NULL,
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity_on_hand INT NOT NULL,
    quantity_reserved INT NOT NULL,
    inventory_value DECIMAL(15, 2) NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_snapshot UNIQUE (snapshot_date, product_id, warehouse_id)
);
```

**Audit Fields**: created_at
**Indexes**: snapshot_date, product_id, warehouse_id
**Retention**: Keep 3 years for historical analysis
**Analytics**: Inventory trends, inventory aging analysis

---

### 2.11 `audit_logs` Table
**Purpose**: Immutable audit trail of inventory changes

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
    CONSTRAINT valid_action_type CHECK (action_type IN ('STOCK_IN', 'STOCK_OUT', 'TRANSFER', 'ADJUSTMENT', 'DAMAGE_REPORTED', 'HOLD_CREATED', 'HOLD_RELEASED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id, request_id
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite
**Analytics**: Inventory audit trail

---

## 3. Relationships & Foreign Keys

```
inventory_locations (1) ----→ (M) inventory_levels ----→ (1) products
                           ├──→ (M) stock_movements
                           └──→ (M) inventory_holds

stock_movements (parent)
├── stock_in (1:1)
├── stock_out (1:1)
├── stock_transfer (1:1)
└── stock_adjustments (1:M)

products → damaged_inventory
inventory_snapshots (time-series analytics)
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_inventory_levels_product ON inventory_levels(product_id);
CREATE INDEX idx_inventory_levels_warehouse ON inventory_levels(warehouse_id);
CREATE INDEX idx_stock_movements_product ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_type_timestamp ON stock_movements(movement_type, timestamp DESC);
CREATE INDEX idx_stock_movements_reference ON stock_movements(reference_id, reference_type);
CREATE INDEX idx_inventory_snapshots_date ON inventory_snapshots(snapshot_date DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_inventory_product_warehouse ON inventory_levels(product_id, warehouse_id);
CREATE INDEX idx_stock_in_received_date ON stock_in(received_at DESC) WHERE inspection_status = 'COMPLETED';
CREATE INDEX idx_inventory_holds_active ON inventory_holds(product_id, warehouse_id) WHERE released_at IS NULL;
```

---

## 5. Constraints & Business Rules

### Real-Time Consistency
```sql
-- Trigger: Update inventory_levels when stock_movements inserted
-- Trigger: Validate available quantity before hold/reservation
-- Trigger: Update last_moved_at on movement
```

### Constraints
```sql
ALTER TABLE inventory_levels ADD CONSTRAINT ck_non_negative CHECK (quantity_on_hand >= 0);
ALTER TABLE stock_movements ADD CONSTRAINT ck_positive_qty CHECK (quantity > 0);
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V3.0__Initialize_inventory_schema.sql
V3.1__Add_stock_movements_tables.sql
V3.2__Add_damaged_and_holds.sql
V3.3__Add_snapshots_for_analytics.sql
V3.4__Add_performance_indexes.sql
V3.5__Add_partitioning.sql
```

### Performance Optimization
- Enable table partitioning on stock_movements by month
- Create materialized views for daily reporting
- Archive old snapshots to separate schema

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily inventory snapshots
- Stock movement transactions
- Inventory value trends
- Warehouse utilization metrics
- Product stock-out frequency

### ML Feature Inputs
- Demand patterns from stock_out movements
- Seasonality detection from snapshots
- Lead time analysis from stock_in dates
- Stock-out prediction signals
- Warehouse optimization metrics

### Business Intelligence
- Real-time inventory dashboard
- Stock aging analysis
- Inventory turnover rates
- Carrying cost analysis
- Shrinkage analysis

---

## 8. Scalability Considerations

### Partitioning Strategy

**stock_movements (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', timestamp))
```

**inventory_snapshots (Time-based)**:
```sql
PARTITION BY RANGE (snapshot_date)
```

---

## 9. Monitoring & Observability

### Key Metrics
- Real-time inventory value
- Stock level accuracy
- Movement throughput
- Damaged inventory percentage
- Hold duration average

### Alerts
- Stock-out conditions
- Inventory variance detected
- Damaged inventory spike
- Hold expiration approaching
- Negative inventory detected (data quality)

---

## Summary

**Total Tables**: 11  
**Total Indexes**: 20+  
**Audit Coverage**: 100%  
**Real-Time**: inventory_levels (ACID transactions)  
**Analytics-Ready**: Complete transaction history with snapshots  

