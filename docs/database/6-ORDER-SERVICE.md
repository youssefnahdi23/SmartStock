# Database Specification: Order Service

**Service**: Order Service  
**Purpose**: Order management, fulfillment, and tracking  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Order Service manages customer orders, line items, fulfillment, and order tracking.

### High-Level Architecture
```
orders
├── order_items (1:M)
├── order_fulfillments (1:M)
├── order_shipments (1:M)
└── order_payments (1:M)
```

---

## 2. Tables Specification

### 2.1 `orders` Table
**Purpose**: Store customer orders

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(100) UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    order_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requested_delivery_date DATE,
    promised_delivery_date DATE,
    actual_delivery_date DATE,
    order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    fulfillment_status VARCHAR(50) DEFAULT 'NOT_STARTED',
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    total_items INT DEFAULT 0,
    subtotal DECIMAL(15, 2) DEFAULT 0,
    tax_amount DECIMAL(15, 2) DEFAULT 0,
    shipping_amount DECIMAL(15, 2) DEFAULT 0,
    discount_amount DECIMAL(15, 2) DEFAULT 0,
    total_amount DECIMAL(15, 2) DEFAULT 0,
    currency_code VARCHAR(3) DEFAULT 'USD',
    shipping_address TEXT NOT NULL,
    billing_address TEXT,
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(255),
    order_notes TEXT,
    is_back_order BOOLEAN DEFAULT false,
    back_order_reason VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    CONSTRAINT order_number_not_empty CHECK (order_number != ''),
    CONSTRAINT valid_order_status CHECK (order_status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    CONSTRAINT valid_fulfillment_status CHECK (fulfillment_status IN ('NOT_STARTED', 'IN_PROGRESS', 'PARTIAL', 'COMPLETE', 'CANCELLED')),
    CONSTRAINT valid_payment_status CHECK (payment_status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED')),
    CONSTRAINT non_negative_amounts CHECK (subtotal >= 0 AND tax_amount >= 0 AND shipping_amount >= 0)
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: order_number, customer_id, order_date, order_status, fulfillment_status, payment_status
**Analytics**: Order volume, revenue tracking, fulfillment metrics

---

### 2.2 `order_items` Table
**Purpose**: Store line items for each order

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_sku VARCHAR(255),
    product_name VARCHAR(500),
    quantity_ordered INT NOT NULL,
    quantity_allocated INT DEFAULT 0,
    quantity_fulfilled INT DEFAULT 0,
    quantity_backordered INT DEFAULT 0,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_total DECIMAL(15, 2) NOT NULL,
    item_discount DECIMAL(15, 2) DEFAULT 0,
    item_tax DECIMAL(15, 2) DEFAULT 0,
    item_status VARCHAR(50) DEFAULT 'PENDING',
    fulfillment_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT positive_quantity CHECK (quantity_ordered > 0),
    CONSTRAINT positive_unit_price CHECK (unit_price > 0),
    CONSTRAINT valid_item_status CHECK (item_status IN ('PENDING', 'ALLOCATED', 'FULFILLED', 'BACKORDERED', 'CANCELLED'))
);
```

**Audit Fields**: created_at, updated_at, fulfillment_date
**Indexes**: order_id, product_id, item_status
**Analytics**: Item-level revenue, demand by product

---

### 2.3 `order_allocations` Table
**Purpose**: Track inventory allocation to orders

```sql
CREATE TABLE order_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    inventory_location_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity_allocated INT NOT NULL,
    allocation_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    allocated_by UUID NOT NULL,
    fulfilled_at TIMESTAMP WITH TIME ZONE,
    fulfilled_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT positive_quantity CHECK (quantity_allocated > 0)
);
```

**Audit Fields**: allocation_date, allocated_by, fulfilled_at, fulfilled_by, created_at, updated_at
**Indexes**: order_item_id, inventory_location_id, warehouse_id, allocation_date
**Analytics**: Warehouse fulfillment patterns

---

### 2.4 `order_fulfillments` Table
**Purpose**: Track fulfillment documents (pick lists, packing slips)

```sql
CREATE TABLE order_fulfillments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    fulfillment_number VARCHAR(100) UNIQUE NOT NULL,
    fulfillment_type VARCHAR(50) NOT NULL DEFAULT 'PICK_AND_PACK',
    warehouse_id UUID NOT NULL,
    fulfillment_status VARCHAR(50) DEFAULT 'PENDING',
    total_items INT,
    items_picked INT DEFAULT 0,
    items_packed INT DEFAULT 0,
    items_shipped INT DEFAULT 0,
    pick_date TIMESTAMP WITH TIME ZONE,
    pick_started_at TIMESTAMP WITH TIME ZONE,
    pick_completed_at TIMESTAMP WITH TIME ZONE,
    picked_by UUID,
    pack_started_at TIMESTAMP WITH TIME ZONE,
    pack_completed_at TIMESTAMP WITH TIME ZONE,
    packed_by UUID,
    quality_checked_at TIMESTAMP WITH TIME ZONE,
    quality_checked_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_fulfillment_status CHECK (fulfillment_status IN ('PENDING', 'PICKING', 'PACKING', 'QUALITY_CHECK', 'READY_TO_SHIP', 'SHIPPED', 'CANCELLED'))
);
```

**Audit Fields**: created_at, updated_at, pick_date, pack_started_at, pick_completed_at
**Indexes**: order_id, fulfillment_number, warehouse_id, fulfillment_status
**Analytics**: Fulfillment cycle time, warehouse efficiency

---

### 2.5 `order_shipments` Table
**Purpose**: Track order shipments

```sql
CREATE TABLE order_shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    fulfillment_id UUID REFERENCES order_fulfillments(id),
    shipment_number VARCHAR(100) UNIQUE NOT NULL,
    carrier_id UUID,
    carrier_name VARCHAR(255) NOT NULL,
    tracking_number VARCHAR(255),
    shipping_method VARCHAR(100),
    weight_kg DECIMAL(10, 3),
    dimensions_cm VARCHAR(50),
    ship_date DATE NOT NULL,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    shipment_status VARCHAR(50) DEFAULT 'PENDING',
    shipped_by UUID,
    signed_by VARCHAR(255),
    delivery_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_shipment_status CHECK (shipment_status IN ('PENDING', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED', 'RETURNED'))
);
```

**Audit Fields**: created_at, updated_at, ship_date, actual_delivery_date
**Indexes**: order_id, tracking_number, ship_date, shipment_status
**Analytics**: Shipping metrics, delivery performance

---

### 2.6 `order_payments` Table
**Purpose**: Track order payment transactions

```sql
CREATE TABLE order_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency_code VARCHAR(3) DEFAULT 'USD',
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    payment_reference VARCHAR(255),
    transaction_id VARCHAR(255),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    authorization_code VARCHAR(100),
    processor_response JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT positive_amount CHECK (amount > 0),
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'CHECK', 'CASH', 'PURCHASE_ORDER')),
    CONSTRAINT valid_payment_status CHECK (payment_status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'DECLINED', 'FAILED', 'REFUNDED', 'CHARGEBACK'))
);
```

**Audit Fields**: payment_date, created_at, updated_at
**Indexes**: order_id, payment_date, payment_status, transaction_id
**Analytics**: Payment patterns, revenue recognition

---

### 2.7 `order_returns` Table
**Purpose**: Track order returns

```sql
CREATE TABLE order_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    return_number VARCHAR(100) UNIQUE NOT NULL,
    return_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    requested_by UUID NOT NULL,
    return_reason VARCHAR(200) NOT NULL,
    return_status VARCHAR(50) DEFAULT 'INITIATED',
    items_returned INT,
    refund_amount DECIMAL(15, 2),
    refund_status VARCHAR(50) DEFAULT 'PENDING',
    refund_processed_at TIMESTAMP WITH TIME ZONE,
    restocking_fee DECIMAL(15, 2) DEFAULT 0,
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(255),
    received_at_warehouse TIMESTAMP WITH TIME ZONE,
    quality_check_passed BOOLEAN,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_return_status CHECK (return_status IN ('INITIATED', 'AUTHORIZED', 'SHIPPED', 'RECEIVED', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT valid_refund_status CHECK (refund_status IN ('PENDING', 'PROCESSED', 'FAILED', 'CANCELLED'))
);
```

**Audit Fields**: return_date, requested_by, received_at_warehouse, refund_processed_at, created_at, updated_at
**Indexes**: order_id, return_number, return_date, return_status
**Analytics**: Return rate, return reasons, customer satisfaction

---

### 2.8 `audit_logs` Table
**Purpose**: Immutable audit trail of order changes

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
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'CONFIRM', 'SHIP', 'DELIVER', 'CANCEL', 'RETURN', 'REFUND'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE operations
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
orders (1) ----→ (M) order_items
      ├──→ (M) order_allocations
      ├──→ (M) order_fulfillments
      ├──→ (M) order_shipments
      ├──→ (M) order_payments
      ├──→ (M) order_returns
      └──→ (M) audit_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_date ON orders(order_date DESC);
CREATE INDEX idx_orders_status ON orders(order_status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_shipments_tracking ON order_shipments(tracking_number);
CREATE INDEX idx_returns_number ON order_returns(return_number);
```

### Composite Indexes
```sql
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date DESC);
CREATE INDEX idx_orders_status_date ON orders(order_status, order_date DESC);
CREATE INDEX idx_fulfillments_warehouse_status ON order_fulfillments(warehouse_id, fulfillment_status);
```

---

## 5. Constraints & Business Rules

### Order Integrity
```sql
-- Order total_amount = subtotal + tax + shipping - discount
-- Order status progression: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
-- Cannot fulfill more than ordered quantity
```

### Financial Constraints
```sql
-- Total payments <= order total_amount
-- Refund cannot exceed original payment
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V6.0__Initialize_order_schema.sql
V6.1__Add_fulfillment_and_shipments.sql
V6.2__Add_payments_and_returns.sql
V6.3__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily order summary
- Order fulfillment metrics
- Shipping and delivery data
- Payment transactions
- Return and refund data

### ML Feature Inputs
- Customer order patterns
- Product demand forecasting
- Delivery time prediction
- Return probability prediction
- Customer lifetime value

### Business Intelligence
- Orders dashboard (volume, revenue, status)
- Fulfillment efficiency metrics
- Shipping performance tracking
- Payment success rates
- Return analysis (rate, reasons)
- Customer order frequency

---

## 8. Scalability Considerations

### Partitioning Strategy

**orders (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', order_date))
```

**order_payments (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', payment_date))
```

---

## 9. Monitoring & Observability

### Key Metrics
- Total orders (daily)
- Order revenue (daily)
- Average order value
- Fulfillment rate
- Return rate
- Payment success rate

### Alerts
- High return rate on product
- Payment decline spike
- Unfulfilled order aging
- Shipment delay detected

---

## Summary

**Total Tables**: 8  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Order Lifecycle**: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED  
**Analytics-Ready**: Complete transaction history and fulfillment metrics  

