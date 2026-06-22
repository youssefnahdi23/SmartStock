# 8. Sales Order Service

**Bounded Context:** Sales Workflows  
**Database:** `sales_db` (PostgreSQL)  
**Port:** 8008  
**Team:** Sales Operations  

---

## Purpose

The Sales Order Service manages the complete sales lifecycle from order creation through fulfillment and shipment.

---

## Responsibilities

- Sales order creation and management
- Order line items
- Fulfillment status tracking
- Shipping management
- Return processing
- Order history and reporting
- Credit limit validation

---

## Database Ownership

**Schema:** `sales_db`

**Core Tables:**
```sql
sales_orders (
  id UUID PRIMARY KEY,
  order_number VARCHAR UNIQUE NOT NULL,
  customer_id UUID NOT NULL,
  order_date DATE,
  requested_delivery_date DATE,
  status ENUM ('draft', 'confirmed', 'reserved', 'picking', 'shipped', 'delivered', 'returned', 'cancelled'),
  total_amount DECIMAL,
  created_at TIMESTAMP
)

order_line_items (
  id UUID PRIMARY KEY,
  sales_order_id UUID NOT NULL,
  product_id UUID NOT NULL,
  quantity INT,
  unit_price DECIMAL,
  line_total DECIMAL,
  status ENUM ('pending', 'reserved', 'fulfilled', 'shipped', 'returned')
)

shipments (
  id UUID PRIMARY KEY,
  sales_order_id UUID NOT NULL,
  tracking_number VARCHAR,
  carrier VARCHAR,
  shipped_date DATE,
  estimated_delivery DATE,
  actual_delivery DATE,
  status ENUM ('pending', 'shipped', 'in_transit', 'delivered', 'exception')
)

returns (
  id UUID PRIMARY KEY,
  sales_order_id UUID NOT NULL,
  return_number VARCHAR,
  product_id UUID,
  quantity INT,
  reason VARCHAR,
  status ENUM ('initiated', 'received', 'inspected', 'approved', 'credited'),
  created_at TIMESTAMP
)

return_line_items (
  id UUID PRIMARY KEY,
  return_id UUID NOT NULL,
  order_line_item_id UUID,
  quantity INT,
  refund_amount DECIMAL
)
```

---

## Events Published

### 1. SalesOrderCreated
**When:** Order submitted  
**Payload:** orderId, customerId, items, totalAmount  
**Consumers:** Inventory (reserve), Warehouse (create picking task), Audit Services

### 2. SalesOrderConfirmed
**When:** Credit check passes and inventory reserved  
**Consumers:** Customer, Notification Services

### 3. StockReserved
**When:** Inventory reserved for order  
**Consumers:** Notification (low stock alerts), Reporting

### 4. PickingStarted
**When:** Warehouse begins picking  
**Consumers:** Notification Service

### 5. OrderPacked
**When:** Order packed and ready to ship  
**Consumers:** Notification Service

### 6. OrderShipped
**When:** Order leaves warehouse  
**Payload:** orderId, customerId, trackingNumber, carrier  
**Consumers:** Customer, Inventory (deduct stock), Audit, Reporting Services

### 7. OrderDelivered
**When:** Delivery confirmed  
**Consumers:** Customer, Notification Services

### 8. ReturnInitiated
**When:** Customer initiates return  
**Consumers:** Warehouse (create return receiving task), Audit Services

### 9. ReturnApproved
**When:** Return is valid and approved  
**Consumers:** Inventory (add back stock), Accounting (refund)

---

## Events Consumed

### From Inventory Service
- **StockReserved:** Proceed with fulfillment workflow
- **StockUnavailable:** Cancel order or backorder

### From Warehouse Service
- **ShippingCompleted:** Publish OrderShipped event

### From Customer Service
- **CreditLimitUpdated:** Validate order total against updated limit

---

## REST APIs

**Base URL:** `/api/v1/sales-orders`

### Sales Orders
- `GET /sales-orders` - List orders (paginated)
- `POST /sales-orders` - Create order (draft)
- `GET /sales-orders/{orderId}` - Get order details
- `PUT /sales-orders/{orderId}` - Update order
- `POST /sales-orders/{orderId}/confirm` - Confirm order (triggers credit check + reserve)
- `POST /sales-orders/{orderId}/cancel` - Cancel order

### Line Items
- `GET /sales-orders/{orderId}/lines` - Get line items
- `POST /sales-orders/{orderId}/lines` - Add line item
- `PUT /sales-orders/{orderId}/lines/{lineId}` - Update line item
- `DELETE /sales-orders/{orderId}/lines/{lineId}` - Remove line item

### Fulfillment
- `GET /sales-orders/{orderId}/fulfillment` - Fulfillment status
- `GET /sales-orders/{orderId}/picking-tasks` - Related warehouse tasks

### Shipping
- `GET /sales-orders/{orderId}/shipments` - Shipment details
- `PUT /sales-orders/{orderId}/shipments/{shipmentId}` - Update shipment (tracking)
- `POST /sales-orders/{orderId}/shipments/{shipmentId}/confirm` - Confirm shipment

### Returns
- `GET /sales-orders/{orderId}/returns` - Return history
- `POST /sales-orders/{orderId}/returns` - Initiate return
- `GET /sales-orders/{orderId}/returns/{returnId}` - Return details
- `POST /sales-orders/{orderId}/returns/{returnId}/approve` - Approve return

### Analytics
- `GET /sales-orders/by-customer/{customerId}` - Customer order history
- `GET /sales-orders/pending-delivery` - Orders awaiting delivery
- `GET /sales-orders/statistics` - Sales metrics

---

## Dependencies

**Synchronous Calls:**
- Customer Service (validate customer, check credit)
- Product Service (validate product)
- Inventory Service (check stock availability before reserving)
- Identity Service (validate user permissions)

**Event Sources:**
- Inventory Service
- Warehouse Service
- Customer Service

---

## Data Consistency Patterns

### Order Fulfillment Saga

```
1. SalesOrderCreated event
   ↓
2. Sales Order Service calls:
   - Customer Service: Validate customer + credit
   - Inventory Service: Reserve stock
   ↓
3. If credit fails or stock unavailable:
   - Publish SalesOrderFailed
   - Release any partial reserves
   ↓
4. If all OK:
   - Publish SalesOrderConfirmed
   - Inventory Service receives: Reserve stock
   - Warehouse Service receives: Create picking task
   ↓
5. Warehouse picks/packs/ships
   - Publish ShippingCompleted
   - Sales Order Service receives:
     - Update order status
     - Publish OrderShipped
   ↓
6. Inventory Service receives OrderShipped:
   - Deduct reserved stock (StockOut)
```

### Return Process (Saga)

```
1. Customer initiates return
   - Sales Order publishes ReturnInitiated
   ↓
2. Warehouse receives return
   - Warehouse publishes ReturnReceived
   ↓
3. Sales Order receives:
   - Updates return status
   - Publishes ReturnApproved (if valid) or ReturnRejected
   ↓
4. Inventory receives ReturnApproved:
   - Adds stock back (StockIn)
   ↓
5. Accounting receives ReturnApproved:
   - Processes refund to customer
```

---

## Future Scalability

### Order Status Projection
- Cache order statuses in Redis for real-time dashboards
- Project order state across services

### Backorder Handling
- Implement backorder queue
- Auto-fulfill when stock becomes available
- Notify customer of backorder status

### Advanced Fulfillment
- Multi-warehouse order fulfillment (split shipments)
- Partial fulfillment with backorder
- Express shipping options

### Analytics
- Order fulfillment time analysis
- Return rate analysis
- Customer order patterns
- Revenue forecasting

---

## Deployment Checklist

- [ ] `sales_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured (Inventory, Warehouse, Customer)
- [ ] Credit limit check logic configured
- [ ] Return approval workflow configured
- [ ] Shipping carrier integrations (if any) configured
- [ ] Monitoring/alerting configured
- [ ] Order SLA monitoring configured

