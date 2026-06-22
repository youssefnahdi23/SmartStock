# 7. Purchase Order Service

**Bounded Context:** Purchase Workflows  
**Database:** `purchase_db` (PostgreSQL)  
**Port:** 8007  
**Team:** Procurement  

---

## Purpose

The Purchase Order Service manages the complete purchasing lifecycle from creation through receipt and invoicing.

---

## Responsibilities

- Purchase order creation and management
- PO line item management
- Supplier order tracking
- Delivery tracking and updates
- Invoice reconciliation
- Payment status tracking
- Return/credit memo management

---

## Database Ownership

**Schema:** `purchase_db`

**Core Tables:**
```sql
purchase_orders (
  id UUID PRIMARY KEY,
  po_number VARCHAR UNIQUE NOT NULL,
  supplier_id UUID NOT NULL,
  order_date DATE,
  expected_delivery_date DATE,
  status ENUM ('draft', 'submitted', 'confirmed', 'received', 'invoiced', 'paid', 'cancelled'),
  total_amount DECIMAL,
  created_at TIMESTAMP
)

po_line_items (
  id UUID PRIMARY KEY,
  purchase_order_id UUID NOT NULL,
  product_id UUID NOT NULL,
  quantity INT,
  unit_price DECIMAL,
  line_total DECIMAL,
  received_quantity INT
)

delivery_tracking (
  id UUID PRIMARY KEY,
  purchase_order_id UUID NOT NULL,
  tracking_number VARCHAR,
  carrier VARCHAR,
  estimated_arrival DATE,
  actual_arrival DATE,
  status ENUM ('pending', 'in_transit', 'delivered', 'delayed')
)

invoices (
  id UUID PRIMARY KEY,
  purchase_order_id UUID NOT NULL,
  invoice_number VARCHAR,
  supplier_id UUID,
  invoice_date DATE,
  due_date DATE,
  total_amount DECIMAL,
  status ENUM ('received', 'verified', 'matched', 'paid', 'disputed'),
  created_at TIMESTAMP
)

invoice_line_items (
  id UUID PRIMARY KEY,
  invoice_id UUID NOT NULL,
  po_line_item_id UUID,
  quantity INT,
  unit_price DECIMAL,
  line_total DECIMAL
)

payments (
  id UUID PRIMARY KEY,
  invoice_id UUID NOT NULL,
  amount DECIMAL,
  payment_method VARCHAR,
  payment_date DATE,
  reference_number VARCHAR,
  status ENUM ('pending', 'approved', 'completed', 'cancelled')
)
```

---

## Events Published

### 1. PurchaseOrderCreated
**When:** PO submitted  
**Payload:** poId, supplierId, items, totalAmount  
**Consumers:** Supplier, Warehouse, Audit Services

### 2. PurchaseOrderConfirmed
**When:** Supplier confirms order  
**Consumers:** Warehouse (create receiving task), Notification

### 3. DeliveryScheduled
**When:** Delivery details known  
**Consumers:** Warehouse Service

### 4. PartialDeliveryReceived
**When:** Partial shipment received  
**Consumers:** Inventory, Warehouse Services

### 5. PurchaseOrderReceived
**When:** All items received  
**Consumers:** Inventory, Supplier, Audit Services

### 6. InvoiceReceived
**When:** Supplier invoice received  
**Consumers:** Audit Service

### 7. InvoiceMatched
**When:** Invoice matches PO  
**Consumers:** Payment processing

### 8. PaymentInitiated
**When:** Payment sent to supplier  
**Consumers:** Audit, Supplier Services

---

## Events Consumed

### From Warehouse Service
- **ReceivingCompleted:** Update PO delivery status, publish PurchaseOrderReceived

### From Supplier Service
- **SupplierUpdated:** Validate supplier still active

---

## REST APIs

**Base URL:** `/api/v1/purchase-orders`

### Purchase Orders
- `GET /purchase-orders` - List POs (paginated)
- `POST /purchase-orders` - Create PO (draft)
- `GET /purchase-orders/{poId}` - Get PO details
- `PUT /purchase-orders/{poId}` - Update PO
- `POST /purchase-orders/{poId}/submit` - Submit PO
- `POST /purchase-orders/{poId}/cancel` - Cancel PO

### Line Items
- `GET /purchase-orders/{poId}/lines` - Get line items
- `POST /purchase-orders/{poId}/lines` - Add line item
- `PUT /purchase-orders/{poId}/lines/{lineId}` - Update line item
- `DELETE /purchase-orders/{poId}/lines/{lineId}` - Remove line item

### Delivery Tracking
- `GET /purchase-orders/{poId}/delivery` - Delivery status
- `PUT /purchase-orders/{poId}/delivery` - Update delivery info

### Invoicing
- `GET /purchase-orders/{poId}/invoices` - Related invoices
- `POST /purchase-orders/{poId}/invoices` - Record invoice
- `GET /purchase-orders/{poId}/invoices/{invoiceId}` - Invoice details

### Payments
- `GET /purchase-orders/{poId}/payments` - Payment history
- `POST /purchase-orders/{poId}/payments` - Record payment

---

## Dependencies

**Synchronous Calls:**
- Supplier Service (validate supplier)
- Product Service (validate product)
- Identity Service (validate user permissions)

**Event Sources:**
- Warehouse Service
- Supplier Service

---

## Data Consistency Patterns

### 3-Way Invoice Matching (Saga)

```
1. Purchase Order Created
   ↓
2. Order Received (from Warehouse)
   ↓
3. Invoice Received (manual entry)
   ↓
4. PO Service matches:
   - PO quantity = Received quantity ✓
   - Invoice quantity = Received quantity ✓
   - Invoice amount = PO amount ✓
   ↓
5. If all match: PublishInvoiceMatched → Payment authorized
6. If mismatch: PublishInvoiceMismatch → Alert accountant
```

---

## Future Scalability

### Supplier Integration
- Direct API integration with suppliers
- EDI/API for PO submission
- Real-time delivery tracking (GPS)

### Automation
- Auto-generate POs based on stock levels
- Three-way matching automation
- Automated payment processing

### Analytics
- Supplier performance scorecards
- Cost analytics
- Procurement trend analysis

---

## Deployment Checklist

- [ ] `purchase_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured (from Warehouse)
- [ ] Invoice matching rules configured
- [ ] Payment processing configured
- [ ] Monitoring/alerting configured
- [ ] Supplier API integrations (if any) configured

