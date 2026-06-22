# 4. Warehouse Service

**Bounded Context:** Warehouse Operations  
**Database:** `warehouse_db` (PostgreSQL)  
**Port:** 8004  
**Team:** Warehouse Operations  

---

## Purpose

The Warehouse Service is the authoritative source for physical warehouse operations. It manages warehouse structures, locations, capacity, and movement workflows.

---

## Responsibilities

- Multi-warehouse management
- Physical location hierarchy (zones, shelves, bins)
- Capacity management and tracking
- Location assignment strategies
- Receiving workflows
- Shipping workflows
- Stock transfer coordination
- Warehouse utilization analytics

---

## Database Ownership

**Schema:** `warehouse_db`

**Core Tables:**
```sql
warehouses (
  id UUID PRIMARY KEY,
  name VARCHAR NOT NULL,
  location_address TEXT,
  manager_id UUID,
  total_capacity INT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

zones (
  id UUID PRIMARY KEY,
  warehouse_id UUID NOT NULL,
  zone_name VARCHAR,
  zone_type ENUM ('general', 'hazmat', 'cold', 'high_value'),
  created_at TIMESTAMP
)

shelves (
  id UUID PRIMARY KEY,
  zone_id UUID NOT NULL,
  shelf_number VARCHAR,
  capacity INT,
  current_usage INT,
  created_at TIMESTAMP
)

bins (
  id UUID PRIMARY KEY,
  shelf_id UUID NOT NULL,
  bin_number VARCHAR,
  capacity INT,
  current_usage INT,
  created_at TIMESTAMP
)

location_assignments (
  id UUID PRIMARY KEY,
  location_id UUID NOT NULL,
  product_id UUID NOT NULL,
  quantity INT,
  last_assigned_at TIMESTAMP
)

stock_movements (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,
  from_location_id UUID,
  to_location_id UUID,
  quantity INT,
  movement_type ENUM ('pick', 'putaway', 'transfer'),
  status ENUM ('pending', 'completed', 'cancelled'),
  created_at TIMESTAMP
)

receiving_tasks (
  id UUID PRIMARY KEY,
  purchase_order_id UUID,
  product_id UUID,
  expected_quantity INT,
  received_quantity INT,
  location_id UUID,
  status ENUM ('pending', 'in_progress', 'completed'),
  created_at TIMESTAMP
)

shipping_tasks (
  id UUID PRIMARY KEY,
  sales_order_id UUID,
  product_id UUID,
  quantity INT,
  location_id UUID,
  status ENUM ('pending', 'picked', 'packed', 'shipped'),
  created_at TIMESTAMP
)
```

---

## Events Published

### 1. WarehouseCreated
**When:** New warehouse registered  
**Consumers:** Audit Service

### 2. LocationAssigned
**When:** Product assigned to location  
**Payload:** productId, locationId, quantity  
**Consumers:** Audit, Reporting Services

### 3. CapacityUpdated
**When:** Location capacity changed  
**Consumers:** Reporting Service

### 4. ReceivingStarted
**When:** Receiving workflow initiated  
**Payload:** purchaseOrderId, expectedItems  
**Consumers:** Notification Service

### 5. ReceivingCompleted
**When:** All items received and stocked  
**Payload:** purchaseOrderId, actualItems, location  
**Consumers:** Purchase Order, Inventory, Audit Services

### 6. ShippingStarted
**When:** Picking/packing begins  
**Payload:** salesOrderId, items  
**Consumers:** Notification Service

### 7. ShippingCompleted
**When:** Order shipped  
**Payload:** salesOrderId, shippedItems, trackingInfo  
**Consumers:** Sales Order, Inventory, Notification, Audit Services

### 8. TransferRequested
**When:** Inter-warehouse transfer initiated  
**Payload:** productId, quantity, fromWarehouse, toWarehouse  
**Consumers:** Inventory Service

### 9. TransferCompleted
**When:** Transfer finalized  
**Consumers:** Inventory, Audit Services

---

## Events Consumed

### From Product Service
- **ProductCreated:** Register product types in warehouse system

### From Purchase Order Service
- **PurchaseOrderCreated:** Create receiving task

### From Sales Order Service
- **SalesOrderCreated:** Create shipping task
- **SalesOrderCancelled:** Cancel shipping task

### From Inventory Service
- **StockTransferred:** Confirm transfer completion

---

## REST APIs

**Base URL:** `/api/v1/warehouses`

### Warehouse Management
- `GET /warehouses` - List all warehouses
- `POST /warehouses` - Create warehouse
- `GET /warehouses/{warehouseId}` - Get warehouse details
- `PUT /warehouses/{warehouseId}` - Update warehouse

### Location Hierarchy
- `GET /warehouses/{warehouseId}/zones` - List zones
- `POST /warehouses/{warehouseId}/zones` - Create zone
- `GET /warehouses/{warehouseId}/zones/{zoneId}/shelves` - List shelves
- `POST /warehouses/{warehouseId}/zones/{zoneId}/shelves` - Create shelf
- `GET /warehouses/{warehouseId}/zones/{zoneId}/shelves/{shelfId}/bins` - List bins
- `POST /warehouses/{warehouseId}/zones/{zoneId}/shelves/{shelfId}/bins` - Create bin

### Capacity Management
- `GET /warehouses/{warehouseId}/capacity` - Get utilization
- `GET /locations/{locationId}/capacity` - Get location capacity
- `PUT /locations/{locationId}/capacity` - Update capacity

### Receiving
- `GET /warehouses/{warehouseId}/receiving-tasks` - List receiving tasks
- `POST /receiving-tasks/{taskId}/start` - Begin receiving
- `POST /receiving-tasks/{taskId}/confirm-item` - Confirm received item
- `POST /receiving-tasks/{taskId}/complete` - Complete receiving

### Shipping
- `GET /warehouses/{warehouseId}/shipping-tasks` - List shipping tasks
- `POST /shipping-tasks/{taskId}/pick` - Begin picking
- `POST /shipping-tasks/{taskId}/pack` - Pack items
- `POST /shipping-tasks/{taskId}/ship` - Ship order

### Stock Movements
- `GET /movements` - List all movements
- `POST /transfers` - Request inter-warehouse transfer

---

## Dependencies

**Synchronous Calls:**
- Product Service (validate product)
- Inventory Service (query stock levels)
- Identity Service (validate permissions)

**Event Sources:**
- Product Service
- Purchase Order Service
- Sales Order Service
- Inventory Service

---

## Data Consistency Patterns

### Receiving Workflow (Saga)

```
1. Purchase Order Created → PurchaseOrderCreated event
2. Warehouse Service creates ReceivingTask
3. Operator scans items via desktop app
4. Warehouse Service publishes ReceivingCompleted
5. Inventory Service subscribes → StockIn event
6. Purchase Order Service subscribes → Order status updated
```

### Shipping Workflow (Saga)

```
1. Sales Order Created → SalesOrderCreated event
2. Warehouse Service creates ShippingTask
3. Operator picks items via desktop app
4. Items confirmed in system
5. Warehouse Service publishes ShippingCompleted
6. Inventory Service subscribes → StockOut event
7. Sales Order Service subscribes → Order status updated
8. Notification Service alerts customer → shipment
```

---

## Future Scalability

### Caching
- Warehouse structure in Redis (30-min TTL)
- Location availability cache

### Location Assignment Strategy
- Optimize for pick efficiency (closest location)
- Optimize for space utilization (best-fit algorithm)
- Zone preferences (hazmat, cold storage rules)

### Mobile Integration
- Real-time task updates to mobile devices
- Barcode scanning for pick/pack operations
- Voice-guided picking (future)

### Analytics
- Heatmaps of high-traffic zones
- Utilization trends
- Picking efficiency metrics

---

## Deployment Checklist

- [ ] `warehouse_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Warehouses and zones configured
- [ ] Receiving/shipping task queues configured
- [ ] Event subscription configured
- [ ] Mobile app connectivity tested
- [ ] Monitoring/alerting configured
- [ ] Barcode scanning configured

