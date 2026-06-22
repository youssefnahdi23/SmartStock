# Sales Order Service API

**Base URL:** `/api/v1/sales-orders`  
**Service Port:** 8008  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Sales order-specific permissions  
**Status:** Core Service

---

## Overview

The Sales Order Service manages customer sales orders, fulfillment workflows, shipment tracking, and customer order lifecycle management.

---

## Endpoints

### 1. Create Sales Order

**Endpoint:** `POST /api/v1/sales-orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:create`

**Request Body:**
```json
{
  "customerId": "customer-001",
  "soNumber": "SO-2026-001",
  "orderDate": "2026-06-20",
  "dueDate": "2026-06-30",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 100,
      "unitPrice": 99.99,
      "discount": 5.0,
      "notes": "Premium quality"
    }
  ],
  "shippingAddress": {
    "address": "999 Warehouse Way, Newark, NJ 07001",
    "city": "Newark",
    "state": "NJ",
    "zipCode": "07001"
  },
  "shippingMethod": "STANDARD",
  "paymentTerms": "NET_30",
  "notes": "Rush order - premium customer"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "soId": "SO-2026-001",
    "soNumber": "SO-2026-001",
    "customerId": "customer-001",
    "customerName": "ABC Corporation",
    "status": "CREATED",
    "orderDate": "2026-06-20",
    "dueDate": "2026-06-30",
    "totalQuantity": 100,
    "totalLineAmount": 9999.00,
    "discount": 500.00,
    "tax": 720.00,
    "totalAmount": 10219.00,
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "productName": "Premium Widget",
        "quantity": 100,
        "unitPrice": 99.99,
        "reserved": true
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

### 2. Get Sales Order

**Endpoint:** `GET /api/v1/sales-orders/{soId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:read`

**Response (200 OK):**
```json
{
  "data": {
    "soId": "SO-2026-001",
    "soNumber": "SO-2026-001",
    "customerId": "customer-001",
    "customerName": "ABC Corporation",
    "status": "CONFIRMED",
    "orderDate": "2026-06-20",
    "confirmationDate": "2026-06-20T12:30:00Z",
    "dueDate": "2026-06-30",
    "totalQuantity": 100,
    "pickedQuantity": 100,
    "shippedQuantity": 0,
    "totalAmount": 10219.00,
    "paidAmount": 0,
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "quantity": 100,
        "pickedQuantity": 100,
        "status": "PICKED",
        "warehouseId": "W01"
      }
    ],
    "paymentStatus": "UNPAID",
    "fulfillmentStatus": "PICKING_COMPLETE",
    "shipments": [],
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

### 3. List Sales Orders

**Endpoint:** `GET /api/v1/sales-orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `status` (string) - CREATED, CONFIRMED, PICKING, SHIPPED, DELIVERED, CANCELLED
- `customerId` (UUID) - Filter by customer
- `warehouseId` (UUID) - Filter by picking warehouse
- `fulfillmentStatus` (string) - PENDING, PICKING, READY, SHIPPED, DELIVERED
- `fromDate` (ISO8601Date) - From date
- `toDate` (ISO8601Date) - To date

**Response (200 OK):**
```json
{
  "data": [
    {
      "soId": "SO-2026-001",
      "soNumber": "SO-2026-001",
      "customerName": "ABC Corporation",
      "status": "CONFIRMED",
      "orderDate": "2026-06-20",
      "dueDate": "2026-06-30",
      "totalAmount": 10219.00,
      "fulfillmentStatus": "PICKING_COMPLETE",
      "paymentStatus": "UNPAID"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 850,
    "totalPages": 43,
    "traceId": "trace-123"
  }
}
```

---

### 4. Confirm Sales Order

**Endpoint:** `POST /api/v1/sales-orders/{soId}/confirm`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:confirm`

**Request Body:**
```json
{
  "warehouseId": "W01",
  "notes": "Confirmed with customer"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "soId": "SO-2026-001",
    "status": "CONFIRMED",
    "confirmationDate": "2026-06-20T12:30:00Z",
    "pickingWarehouseId": "W01",
    "fulfillmentStatus": "PENDING"
  },
  "meta": {
    "timestamp": "2026-06-20T12:30:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Pick Order Items

**Endpoint:** `POST /api/v1/sales-orders/{soId}/pick`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:pick`

**Request Body:**
```json
{
  "items": [
    {
      "lineId": "line-001",
      "quantity": 100,
      "binId": "bin-123",
      "location": "Zone-A, Shelf-3, Bin-5"
    }
  ],
  "pickedBy": "user-456"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "soId": "SO-2026-001",
    "fulfillmentStatus": "PICKING_COMPLETE",
    "pickedQuantity": 100,
    "items": [
      {
        "lineId": "line-001",
        "productId": "prod-001",
        "pickedQuantity": 100,
        "location": "Zone-A, Shelf-3, Bin-5"
      }
    ],
    "pickingCompletedAt": "2026-06-21T10:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-21T10:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Create Shipment

**Endpoint:** `POST /api/v1/sales-orders/{soId}/shipments`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:ship`

**Request Body:**
```json
{
  "items": [
    {
      "lineId": "line-001",
      "quantity": 100
    }
  ],
  "carrierName": "FastShip Logistics",
  "trackingNumber": "FS-2026-123456",
  "estimatedDeliveryDate": "2026-06-25",
  "shippingMethod": "STANDARD",
  "notes": "Shipped on time"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "shipmentId": "shipment-123",
    "soId": "SO-2026-001",
    "status": "SHIPPED",
    "carrierName": "FastShip Logistics",
    "trackingNumber": "FS-2026-123456",
    "estimatedDeliveryDate": "2026-06-25",
    "shippedQuantity": 100,
    "shippedAt": "2026-06-21T14:00:00Z",
    "items": [
      {
        "lineId": "line-001",
        "quantity": 100
      }
    ]
  },
  "meta": {
    "timestamp": "2026-06-21T14:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Register Delivery

**Endpoint:** `POST /api/v1/sales-orders/{soId}/delivery/{shipmentId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:deliver`

**Request Body:**
```json
{
  "deliveryDate": "2026-06-25",
  "signedBy": "Robert Procurement",
  "deliveryNotes": "Delivered in good condition",
  "damageReports": []
}
```

**Response (200 OK):**
```json
{
  "data": {
    "shipmentId": "shipment-123",
    "soId": "SO-2026-001",
    "status": "DELIVERED",
    "deliveryDate": "2026-06-25T14:00:00Z",
    "signedBy": "Robert Procurement",
    "fulfillmentStatus": "DELIVERED"
  },
  "meta": {
    "timestamp": "2026-06-25T14:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 8. Cancel Sales Order

**Endpoint:** `POST /api/v1/sales-orders/{soId}/cancel`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:write`

**Request Body:**
```json
{
  "reason": "Customer request",
  "notes": "Customer cancelled order"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "soId": "SO-2026-001",
    "status": "CANCELLED",
    "cancellationDate": "2026-06-20T13:00:00Z",
    "reason": "Customer request"
  },
  "meta": {
    "timestamp": "2026-06-20T13:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 9. Get Order Performance Report

**Endpoint:** `GET /api/v1/sales-orders/analysis/performance`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `sales-order:report`

**Query Parameters:**
- `fromDate` (ISO8601Date) - From date
- `toDate` (ISO8601Date) - To date
- `warehouseId` (UUID) - Filter by warehouse

**Response (200 OK):**
```json
{
  "data": {
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "metrics": {
      "totalOrders": 850,
      "totalOrderValue": 4250000.00,
      "averageOrderValue": 5000.00,
      "ordersOnTime": 810,
      "onTimePercentage": 95.3,
      "averagePickingTime": "2.3 hours",
      "averageShippingTime": "3.7 days",
      "averageDeliveryTime": "2.1 days",
      "customerSatisfactionRate": 96.5
    },
    "topCustomers": [
      {
        "customerId": "customer-001",
        "customerName": "ABC Corporation",
        "orderCount": 50,
        "totalValue": 250000.00
      }
    ]
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### SalesOrderDTO
```typescript
{
  soId: UUID;
  soNumber: string;
  customerId: UUID;
  status: "CREATED" | "CONFIRMED" | "PICKING" | "SHIPPED" | "DELIVERED" | "CANCELLED";
  orderDate: ISO8601Date;
  dueDate: ISO8601Date;
  totalAmount: BigDecimal;
  items: {
    lineId: UUID;
    productId: UUID;
    quantity: number;
    unitPrice: BigDecimal;
  }[];
  fulfillmentStatus: string;
  paymentStatus: string;
  createdAt: ISO8601DateTime;
}
```

---

## Events Published

- `SalesOrderCreated` - New SO created
- `SalesOrderConfirmed` - SO confirmed
- `OrderPickingStarted` - Picking operation started
- `OrderPickingCompleted` - Picking completed
- `ShipmentCreated` - Shipment created
- `ShipmentDispatched` - Shipment dispatched
- `DeliveryCompleted` - Order delivered to customer
- `SalesOrderCancelled` - SO cancelled
- `CustomerSatisfactionRecorded` - Satisfaction feedback recorded

---

## Implementation Notes

1. Stock reserved when SO confirmed to prevent overselling
2. Picking optimized by warehouse location
3. Shipment tracking integrated with carrier APIs
4. Multiple shipments supported per SO (partial shipments)
5. Delivery confirmation triggers customer satisfaction survey
6. Order fulfillment SLA tracked for performance metrics
7. All order changes logged for audit and customer service
