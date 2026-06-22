# 3. Inventory Service

**Bounded Context:** Physical Inventory  
**Database:** `inventory_db` (PostgreSQL)  
**Port:** 8003  
**Team:** Inventory Operations  

---

## Purpose

The Inventory Service is the authoritative source for real-time stock levels. It tracks all stock movements, maintains balance sheets, and provides visibility into inventory across warehouses.

---

## Responsibilities

- Real-time stock tracking (on-hand, reserved, damaged)
- Stock movements (in, out, adjustments, transfers)
- Stock transaction recording (immutable)
- Quantity validation and constraints
- Low-stock alert triggering
- Stock aging analysis
- Historical tracking for audit/analytics

---

## Database Ownership

**Schema:** `inventory_db`

**Core Tables:**
```sql
stock_levels (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  on_hand INT DEFAULT 0,
  reserved INT DEFAULT 0,
  damaged INT DEFAULT 0,
  available INT GENERATED ALWAYS AS (on_hand - reserved - damaged),
  last_counted_at TIMESTAMP,
  updated_at TIMESTAMP
)

stock_transactions (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  transaction_type ENUM ('IN', 'OUT', 'ADJUST', 'TRANSFER', 'DAMAGE'),
  quantity INT NOT NULL,
  reference_id VARCHAR,
  reason_code VARCHAR,
  user_id UUID,
  created_at TIMESTAMP,
  recorded_at TIMESTAMP
)

stock_adjustments (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  quantity_change INT,
  reason TEXT,
  approved_by UUID,
  created_at TIMESTAMP
)

low_stock_alerts (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  threshold INT,
  current_level INT,
  triggered_at TIMESTAMP,
  acknowledged_at TIMESTAMP,
  acknowledged_by UUID
)

stock_history (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  warehouse_id UUID NOT NULL,
  quantity INT,
  recorded_date TIMESTAMP,
  created_at TIMESTAMP
)
```

---

## Events Published

### 1. StockIn
**When:** Inventory received  
**Payload:** productId, warehouseId, quantity, unitCost, supplierId, referenceId  
**Consumers:** Reporting, Notification, Audit, Data Export Services

### 2. StockOut
**When:** Inventory shipped/consumed  
**Payload:** productId, warehouseId, quantity, reason, referenceId  
**Consumers:** Reporting, Notification, Audit Services

### 3. InventoryAdjusted
**When:** Manual inventory correction  
**Payload:** productId, warehouseId, quantityChange, reason, userId  
**Consumers:** Reporting, Audit Services

### 4. StockTransferred
**When:** Inventory moved between warehouses  
**Payload:** productId, fromWarehouse, toWarehouse, quantity  
**Consumers:** Warehouse, Reporting, Audit Services

### 5. LowStockDetected
**When:** Stock falls below threshold  
**Payload:** productId, warehouseId, currentLevel, threshold  
**Consumers:** Notification Service

### 6. DamagedStockRecorded
**When:** Damage discovered  
**Payload:** productId, warehouseId, quantity, reason  
**Consumers:** Audit, Reporting Services

---

## Events Consumed

### From Product Service
- **ProductCreated:** Initialize stock levels (zero)
- **ProductDiscontinued:** Stop accepting stock in

### From Warehouse Service
- **StockTransferred (originated from warehouse):** Update local stock levels

### From Sales Order Service
- **SalesOrderCreated:** Reserve stock quantity
- **SalesOrderFulfilled:** Deduct reserved stock (StockOut)
- **SalesOrderCancelled:** Release reserved stock

### From Purchase Order Service
- **PurchaseOrderReceived:** Trigger StockIn event

---

## REST APIs

**Base URL:** `/api/v1/inventory`

### Stock Query
- `GET /stock/{productId}/{warehouseId}` - Get current stock level
- `GET /stock/{warehouseId}` - Get all stock in warehouse
- `GET /stock/{productId}` - Get stock across all warehouses

### Stock Movements
- `POST /stock-in` - Record inbound stock
- `POST /stock-out` - Record outbound stock
- `POST /adjustments` - Record manual adjustment
- `POST /transfer` - Transfer between warehouses

### Transactions
- `GET /transactions/{productId}` - Get transaction history
- `GET /transactions` - List all transactions (paginated)

### Low Stock Alerts
- `GET /alerts/low-stock` - List active low-stock alerts
- `PUT /alerts/{alertId}/acknowledge` - Mark alert acknowledged

### Analytics
- `GET /stock-aging/{warehouseId}` - Aged inventory report
- `GET /stock-snapshot` - Current inventory snapshot
- `GET /movement-summary` - Movement metrics

---

## Dependencies

**Synchronous Calls:**
- Product Service (validate product exists)
- Identity Service (validate user permissions)

**Event Sources:**
- Product Service
- Warehouse Service
- Sales Order Service
- Purchase Order Service

---

## Data Consistency Patterns

### Stock Reservation (Saga)

```
1. Sales Order Created → SalesOrderCreated event
2. Inventory Service subscribes:
   - Validate sufficient stock available
   - If YES: Reserve stock → StockReserved event
   - If NO: Publish StockUnavailable event
3. Sales Order Service subscribes to StockReserved:
   - Update order status
   - Proceed with fulfillment
```

### Stock Transfer Between Warehouses

```
1. Warehouse Service publishes: TransferRequested
2. Inventory Service subscribes:
   - Deduct from source warehouse
   - Add to destination warehouse
   - Publish: StockTransferred event
3. Warehouse Service subscribes to StockTransferred:
   - Update location assignments
```

---

## Future Scalability

### Caching
- Current stock levels in Redis (1-min TTL)
- Aged inventory cache (hourly refresh)

### Event Streaming
- Send all StockIn/Out events to data lake
- Enable real-time analytics dashboards

### Partitioning
- Partition by warehouse_id + product_id
- Enables independent scaling per warehouse

### Multi-Tenancy
- Add tenant_id to all tables
- Separate data by tenant

---

## Deployment Checklist

- [ ] `inventory_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Indexes on product_id, warehouse_id
- [ ] Redis caching configured
- [ ] Event subscription configured (Kafka/RabbitMQ)
- [ ] Monitoring/alerting configured
- [ ] Low-stock alert thresholds configured

