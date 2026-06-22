# Purchase Order Service API

**Base URL:** `/api/v1/purchase-orders`  
**Service Port:** 8007  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Purchase order-specific permissions  
**Status:** Core Service

---

## Overview

The Purchase Order Service manages purchase orders, supplier procurement, delivery tracking, purchase receipts, and supplier order fulfillment workflows.

---

## Endpoints

### 1. Create Purchase Order

**Endpoint:** `POST /api/v1/purchase-orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:create`

**Request Body:**
```json
{
  "supplierId": "supplier-001",
  "poNumber": "PO-2026-001",
  "orderDate": "2026-06-20",
  "dueDate": "2026-06-27",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 500,
      "unitPrice": 45.00,
      "discount": 2.0,
      "notes": "Standard order"
    }
  ],
  "shippingMethod": "TRUCK",
  "expectedDeliveryDate": "2026-06-27",
  "deliveryWarehouseId": "W01",
  "paymentTerms": "NET_30",
  "notes": "Standard procurement order"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "poId": "PO-2026-001",
    "poNumber": "PO-2026-001",
    "supplierId": "supplier-001",
    "supplierName": "Premium Widgets Ltd",
    "status": "CREATED",
    "orderDate": "2026-06-20",
    "dueDate": "2026-06-27",
    "expectedDeliveryDate": "2026-06-27",
    "totalQuantity": 500,
    "totalLineAmount": 22500.00,
    "discount": 450.00,
    "tax": 1620.00,
    "totalAmount": 23670.00,
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "productName": "Premium Widget",
        "quantity": 500,
        "unitPrice": 45.00,
        "lineAmount": 22500.00
      }
    ],
    "createdBy": "user-123",
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Purchase Order

**Endpoint:** `GET /api/v1/purchase-orders/{poId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:read`

**Response (200 OK):**
```json
{
  "data": {
    "poId": "PO-2026-001",
    "poNumber": "PO-2026-001",
    "supplierId": "supplier-001",
    "supplierName": "Premium Widgets Ltd",
    "status": "CONFIRMED",
    "orderDate": "2026-06-20",
    "confirmationDate": "2026-06-20T12:30:00Z",
    "dueDate": "2026-06-27",
    "expectedDeliveryDate": "2026-06-27",
    "deliveryWarehouseId": "W01",
    "totalQuantity": 500,
    "deliveredQuantity": 0,
    "totalAmount": 23670.00,
    "paidAmount": 0,
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "productName": "Premium Widget",
        "quantity": 500,
        "receivedQuantity": 0,
        "unitPrice": 45.00,
        "status": "PENDING"
      }
    ],
    "paymentStatus": "UNPAID",
    "deliveryStatus": "NOT_RECEIVED",
    "comments": [],
    "createdBy": "user-123",
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. List Purchase Orders

**Endpoint:** `GET /api/v1/purchase-orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `status` (string) - CREATED, CONFIRMED, SHIPPED, RECEIVED, CANCELLED
- `supplierId` (UUID) - Filter by supplier
- `warehouseId` (UUID) - Filter by delivery warehouse
- `fromDate` (ISO8601Date) - From date
- `toDate` (ISO8601Date) - To date

**Response (200 OK):**
```json
{
  "data": [
    {
      "poId": "PO-2026-001",
      "poNumber": "PO-2026-001",
      "supplierName": "Premium Widgets Ltd",
      "status": "CONFIRMED",
      "orderDate": "2026-06-20",
      "dueDate": "2026-06-27",
      "totalAmount": 23670.00,
      "deliveryStatus": "NOT_RECEIVED"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 450,
    "totalPages": 23,
    "traceId": "trace-123"
  }
}
```

---

### 4. Confirm Purchase Order

**Endpoint:** `POST /api/v1/purchase-orders/{poId}/confirm`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:confirm`

**Request Body:**
```json
{
  "notes": "Order confirmed with supplier"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "poId": "PO-2026-001",
    "status": "CONFIRMED",
    "confirmationDate": "2026-06-20T12:30:00Z",
    "confirmationNumber": "CONF-2026-001"
  },
  "meta": {
    "timestamp": "2026-06-20T12:30:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Register Delivery

**Endpoint:** `POST /api/v1/purchase-orders/{poId}/delivery`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:receive`

**Request Body:**
```json
{
  "deliveryDate": "2026-06-27",
  "carrierName": "FastShip Logistics",
  "trackingNumber": "FS-2026-123456",
  "items": [
    {
      "lineId": "line-001",
      "receivedQuantity": 500,
      "damageCount": 0,
      "condition": "GOOD"
    }
  ],
  "deliveryNotes": "On-time delivery, all items in good condition"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "poId": "PO-2026-001",
    "deliveryId": "delivery-123",
    "status": "RECEIVED",
    "deliveryDate": "2026-06-27",
    "totalReceivedQuantity": 500,
    "damageCount": 0,
    "receivedAt": "2026-06-27T14:00:00Z",
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "receivedQuantity": 500,
        "damageCount": 0,
        "stockInTransactionId": "txn-001"
      }
    ]
  },
  "meta": {
    "timestamp": "2026-06-27T14:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Cancel Purchase Order

**Endpoint:** `POST /api/v1/purchase-orders/{poId}/cancel`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:write`

**Request Body:**
```json
{
  "reason": "Order quantity reduced",
  "notes": "Customer cancelled partial order"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "poId": "PO-2026-001",
    "status": "CANCELLED",
    "cancelledAt": "2026-06-20T13:00:00Z",
    "reason": "Order quantity reduced"
  },
  "meta": {
    "timestamp": "2026-06-20T13:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Record Quality Issue

**Endpoint:** `POST /api/v1/purchase-orders/{poId}/quality-issue`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `purchase-order:quality`

**Request Body:**
```json
{
  "lineId": "line-001",
  "issueType": "DEFECTIVE_UNITS",
  "quantity": 25,
  "description": "25 units with manufacturing defects",
  "severity": "HIGH",
  "proposedResolution": "RETURN_AND_REPLACE"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "issueId": "qi-123",
    "poId": "PO-2026-001",
    "lineId": "line-001",
    "issueType": "DEFECTIVE_UNITS",
    "quantity": 25,
    "severity": "HIGH",
    "status": "OPEN",
    "createdAt": "2026-06-28T10:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-28T10:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### PurchaseOrderDTO
```typescript
{
  poId: UUID;
  poNumber: string;
  supplierId: UUID;
  status: "CREATED" | "CONFIRMED" | "SHIPPED" | "RECEIVED" | "CANCELLED";
  orderDate: ISO8601Date;
  dueDate: ISO8601Date;
  totalAmount: BigDecimal;
  items: {
    lineId: UUID;
    productId: UUID;
    quantity: number;
    unitPrice: BigDecimal;
  }[];
  deliveryStatus: string;
  paymentStatus: string;
  createdBy: UUID;
  createdAt: ISO8601DateTime;
}
```

---

## Events Published

- `PurchaseOrderCreated` - New PO created
- `PurchaseOrderConfirmed` - PO confirmed with supplier
- `DeliveryRegistered` - Delivery received
- `PurchaseOrderCancelled` - PO cancelled
- `QualityIssueReported` - Quality issue on delivery
- `SupplierDeliveryPerformanceUpdated` - Delivery performance updated

---

## Implementation Notes

1. Purchase orders linked to inventory stock-in transactions
2. Delivery tracking integrates with warehouse operations
3. Quality issues tracked separately for supplier performance
4. Payment processing managed by accounting system
5. All PO changes logged for audit trail
6. Supplier lead times used for demand forecasting
7. Bulk orders may require special approval
