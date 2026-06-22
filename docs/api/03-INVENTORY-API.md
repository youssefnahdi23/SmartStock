# Inventory Service API

**Base URL:** `/api/v1/inventory`  
**Service Port:** 8003  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Inventory-specific permissions  
**Status:** Core Service

---

## Overview

The Inventory Service manages real-time stock levels, stock movements (in/out/transfers), stock adjustments, inventory counts, and physical inventory audits across all warehouses.

---

## Endpoints

### 1. Stock In (Receive)

**Endpoint:** `POST /api/v1/inventory/stock-in`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:write`, `stock:in`

**Request Body:**
```json
{
  "productId": "prod-001",
  "warehouseId": "W01",
  "quantity": 100,
  "unitCost": 45.00,
  "referenceType": "PURCHASE_ORDER",
  "referenceId": "PO-2026-001",
  "supplierId": "supplier-123",
  "receiveDate": "2026-06-20",
  "notes": "Received from supplier via truck"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "transactionId": "txn-123",
    "productId": "prod-001",
    "productName": "Premium Widget",
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "quantity": 100,
    "unitCost": 45.00,
    "transactionValue": 4500.00,
    "previousStockLevel": 250,
    "newStockLevel": 350,
    "transactionType": "STOCK_IN",
    "referenceType": "PURCHASE_ORDER",
    "referenceId": "PO-2026-001",
    "supplierId": "supplier-123",
    "userId": "user-123",
    "timestamp": "2026-06-20T12:00:00Z",
    "notes": "Received from supplier via truck"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `404 PRODUCT_NOT_FOUND` - Product does not exist
- `404 WAREHOUSE_NOT_FOUND` - Warehouse does not exist
- `422 VALIDATION_FAILED` - Invalid quantity or cost

---

### 2. Stock Out (Dispatch)

**Endpoint:** `POST /api/v1/inventory/stock-out`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:write`, `stock:out`

**Request Body:**
```json
{
  "productId": "prod-001",
  "warehouseId": "W01",
  "quantity": 50,
  "referenceType": "SALES_ORDER",
  "referenceId": "SO-2026-001",
  "customerId": "customer-456",
  "shippingDate": "2026-06-20",
  "notes": "Shipped to customer ABC Corp"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "transactionId": "txn-124",
    "productId": "prod-001",
    "productName": "Premium Widget",
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "quantity": 50,
    "previousStockLevel": 350,
    "newStockLevel": 300,
    "transactionType": "STOCK_OUT",
    "referenceType": "SALES_ORDER",
    "referenceId": "SO-2026-001",
    "customerId": "customer-456",
    "userId": "user-123",
    "timestamp": "2026-06-20T12:00:00Z",
    "notes": "Shipped to customer ABC Corp"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `400 INSUFFICIENT_STOCK` - Not enough stock available
- `404 PRODUCT_NOT_FOUND` - Product does not exist
- `404 WAREHOUSE_NOT_FOUND` - Warehouse does not exist

---

### 3. Stock Transfer (Between Warehouses)

**Endpoint:** `POST /api/v1/inventory/transfers`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:write`, `stock:transfer`

**Request Body:**
```json
{
  "productId": "prod-001",
  "fromWarehouseId": "W01",
  "toWarehouseId": "W02",
  "quantity": 25,
  "transferDate": "2026-06-20",
  "reason": "Rebalancing stock levels",
  "notes": "Transfer for warehouse optimization"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "transferId": "transfer-123",
    "productId": "prod-001",
    "productName": "Premium Widget",
    "fromWarehouseId": "W01",
    "fromWarehouseName": "Main Warehouse",
    "toWarehouseId": "W02",
    "toWarehouseName": "Secondary Warehouse",
    "quantity": 25,
    "fromStockBefore": 300,
    "fromStockAfter": 275,
    "toStockBefore": 150,
    "toStockAfter": 175,
    "status": "CREATED",
    "reason": "Rebalancing stock levels",
    "userId": "user-123",
    "createdAt": "2026-06-20T12:00:00Z",
    "notes": "Transfer for warehouse optimization"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 4. Adjust Stock

**Endpoint:** `POST /api/v1/inventory/adjustments`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:write`, `stock:adjust`

**Request Body:**
```json
{
  "productId": "prod-001",
  "warehouseId": "W01",
  "adjustmentQuantity": -5,
  "reason": "DAMAGE",
  "adjustmentType": "PHYSICAL_COUNT",
  "notes": "5 units damaged in shipping",
  "approverComments": "Approved based on damage report DR-001"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "adjustmentId": "adj-123",
    "productId": "prod-001",
    "productName": "Premium Widget",
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "adjustmentQuantity": -5,
    "reason": "DAMAGE",
    "adjustmentType": "PHYSICAL_COUNT",
    "previousStockLevel": 275,
    "newStockLevel": 270,
    "status": "APPROVED",
    "createdBy": "user-123",
    "approvedBy": "user-124",
    "createdAt": "2026-06-20T12:00:00Z",
    "approvedAt": "2026-06-20T12:05:00Z",
    "notes": "5 units damaged in shipping",
    "approverComments": "Approved based on damage report DR-001"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Get Stock Level

**Endpoint:** `GET /api/v1/inventory/stock/{productId}/{warehouseId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:read`

**Response (200 OK):**
```json
{
  "data": {
    "productId": "prod-001",
    "productName": "Premium Widget",
    "productSku": "WDG-001-PREM",
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "currentStockLevel": 270,
    "reservedStock": 50,
    "availableStock": 220,
    "reorderPoint": 50,
    "reorderQuantity": 100,
    "maxStock": 500,
    "unitPrice": 99.99,
    "stockValue": 26997.30,
    "lowStock": false,
    "lastMovementAt": "2026-06-20T12:05:00Z",
    "lastMovementType": "ADJUSTMENT",
    "turnoverRate": 12.5,
    "daysOnHand": 29.2
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Get Product Stock Across All Warehouses

**Endpoint:** `GET /api/v1/inventory/products/{productId}/stock`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:read`

**Response (200 OK):**
```json
{
  "data": {
    "productId": "prod-001",
    "productName": "Premium Widget",
    "productSku": "WDG-001-PREM",
    "totalStockLevel": 645,
    "totalStockValue": 64494.55,
    "warehouses": [
      {
        "warehouseId": "W01",
        "warehouseName": "Main Warehouse",
        "stockLevel": 270,
        "reservedStock": 50,
        "availableStock": 220,
        "percentage": 41.9
      },
      {
        "warehouseId": "W02",
        "warehouseName": "Secondary Warehouse",
        "stockLevel": 175,
        "reservedStock": 25,
        "availableStock": 150,
        "percentage": 27.1
      },
      {
        "warehouseId": "W03",
        "warehouseName": "Distribution Center",
        "stockLevel": 200,
        "reservedStock": 40,
        "availableStock": 160,
        "percentage": 31.0
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

### 7. Get All Stock Levels (Warehouse Inventory)

**Endpoint:** `GET /api/v1/inventory/warehouses/{warehouseId}/stock`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20, max: 100) - Results per page
- `sort` (string) - Field and direction (e.g., "productName,asc")
- `lowStockOnly` (boolean, default: false) - Show only low stock items
- `categoryId` (UUID) - Filter by product category
- `search` (string) - Search product name or SKU

**Response (200 OK):**
```json
{
  "data": [
    {
      "productId": "prod-001",
      "productName": "Premium Widget",
      "productSku": "WDG-001-PREM",
      "stockLevel": 270,
      "reservedStock": 50,
      "availableStock": 220,
      "reorderPoint": 50,
      "stockValue": 26997.30,
      "lowStock": false,
      "lastMovementAt": "2026-06-20T12:05:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 1234,
    "totalPages": 62,
    "totalStockValue": 123456.78,
    "traceId": "trace-123"
  }
}
```

---

### 8. Stock Movement History

**Endpoint:** `GET /api/v1/inventory/transactions`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20, max: 100) - Results per page
- `productId` (UUID) - Filter by product
- `warehouseId` (UUID) - Filter by warehouse
- `transactionType` (string) - STOCK_IN, STOCK_OUT, TRANSFER, ADJUSTMENT
- `fromDate` (ISO8601Date) - From date filter
- `toDate` (ISO8601Date) - To date filter
- `userId` (UUID) - Filter by user

**Response (200 OK):**
```json
{
  "data": [
    {
      "transactionId": "txn-124",
      "productId": "prod-001",
      "productName": "Premium Widget",
      "warehouseId": "W01",
      "warehouseName": "Main Warehouse",
      "quantity": 50,
      "previousBalance": 320,
      "newBalance": 270,
      "transactionType": "STOCK_OUT",
      "referenceType": "SALES_ORDER",
      "referenceId": "SO-2026-001",
      "userId": "user-123",
      "username": "john.operator",
      "timestamp": "2026-06-20T12:00:00Z",
      "notes": "Shipped to customer ABC Corp"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 5432,
    "totalPages": 272,
    "traceId": "trace-123"
  }
}
```

---

### 9. Begin Physical Inventory Count

**Endpoint:** `POST /api/v1/inventory/counts`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:count`

**Request Body:**
```json
{
  "warehouseId": "W01",
  "countType": "FULL",
  "name": "Quarterly Physical Count Q2 2026",
  "countDate": "2026-06-20",
  "countReason": "QUARTERLY",
  "expectedDuration": "3 days",
  "countTeam": ["user-123", "user-124", "user-125"]
}
```

**Response (201 Created):**
```json
{
  "data": {
    "countId": "count-123",
    "warehouseId": "W01",
    "warehouseName": "Main Warehouse",
    "countType": "FULL",
    "name": "Quarterly Physical Count Q2 2026",
    "countDate": "2026-06-20",
    "countReason": "QUARTERLY",
    "status": "IN_PROGRESS",
    "expectedDuration": "3 days",
    "countTeam": ["user-123", "user-124", "user-125"],
    "totalItemsCounted": 0,
    "totalVariances": 0,
    "createdBy": "user-120",
    "startedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 10. Record Count Item

**Endpoint:** `POST /api/v1/inventory/counts/{countId}/items`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:count`

**Request Body:**
```json
{
  "productId": "prod-001",
  "countedQuantity": 265,
  "location": "Zone-A, Shelf-3, Bin-5",
  "condition": "GOOD",
  "notes": "Minor damage on 2 units, not counted"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "countItemId": "count-item-123",
    "countId": "count-123",
    "productId": "prod-001",
    "productName": "Premium Widget",
    "productSku": "WDG-001-PREM",
    "systemQuantity": 270,
    "countedQuantity": 265,
    "variance": -5,
    "variancePercentage": -1.85,
    "location": "Zone-A, Shelf-3, Bin-5",
    "condition": "GOOD",
    "recordedBy": "user-123",
    "timestamp": "2026-06-20T13:00:00Z",
    "notes": "Minor damage on 2 units, not counted"
  },
  "meta": {
    "timestamp": "2026-06-20T13:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 11. Complete Physical Count

**Endpoint:** `POST /api/v1/inventory/counts/{countId}/complete`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `inventory:count`

**Request Body:**
```json
{
  "approverComments": "Count completed successfully. Variances reviewed and approved.",
  "autoAdjust": true
}
```

**Response (200 OK):**
```json
{
  "data": {
    "countId": "count-123",
    "warehouseId": "W01",
    "status": "COMPLETED",
    "totalItemsCounted": 1234,
    "totalVariances": 45,
    "varianceRate": 3.65,
    "adjustmentsCreated": 45,
    "completedBy": "user-120",
    "completedAt": "2026-06-22T18:00:00Z",
    "approverComments": "Count completed successfully. Variances reviewed and approved."
  },
  "meta": {
    "timestamp": "2026-06-22T18:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 12. Reserve Stock (For Sales Orders)

**Endpoint:** `POST /api/v1/inventory/reservations`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `stock:reserve`

**Request Body:**
```json
{
  "productId": "prod-001",
  "warehouseId": "W01",
  "quantity": 50,
  "orderId": "SO-2026-001",
  "reservationReason": "SALES_ORDER",
  "expiryDate": "2026-06-27"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "reservationId": "res-123",
    "productId": "prod-001",
    "warehouseId": "W01",
    "quantity": 50,
    "orderId": "SO-2026-001",
    "reservationReason": "SALES_ORDER",
    "status": "ACTIVE",
    "createdAt": "2026-06-20T12:00:00Z",
    "expiryDate": "2026-06-27",
    "availableStockAfterReservation": 170
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### StockLevelDTO
```typescript
{
  productId: UUID;
  warehouseId: UUID;
  stockLevel: number;
  reservedStock: number;
  availableStock: number;
  reorderPoint: number;
  reorderQuantity: number;
  maxStock: number;
  turnoverRate: number;
  lastMovementAt: ISO8601DateTime;
}
```

### TransactionDTO
```typescript
{
  transactionId: UUID;
  productId: UUID;
  warehouseId: UUID;
  quantity: number;
  transactionType: "STOCK_IN" | "STOCK_OUT" | "TRANSFER" | "ADJUSTMENT";
  referenceType: string;
  referenceId: string;
  previousBalance: number;
  newBalance: number;
  userId: UUID;
  timestamp: ISO8601DateTime;
  notes: string;
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| INSUFFICIENT_STOCK | 400 | Not enough stock available |
| PRODUCT_NOT_FOUND | 404 | Product does not exist |
| WAREHOUSE_NOT_FOUND | 404 | Warehouse does not exist |
| RESERVATION_EXPIRED | 400 | Reservation has expired |
| VALIDATION_FAILED | 422 | Invalid quantity or parameters |
| UNAUTHORIZED | 401 | Invalid or expired token |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |

---

## Events Published

- `StockIn` - Stock received
- `StockOut` - Stock dispatched
- `StockTransferred` - Stock moved between warehouses
- `StockAdjusted` - Stock manually adjusted
- `StockLevelReached` - Stock reached reorder point
- `CountStarted` - Physical count started
- `CountCompleted` - Physical count completed
- `VarianceDetected` - Variance found during count
- `StockReserved` - Stock reserved for order
- `ReservationExpired` - Reservation expired

---

## Implementation Notes

1. All stock transactions require audit trail with userId
2. Stock adjustments may require approval based on amount
3. Stock transfers asynchronously update both warehouses
4. Physical counts lock inventory during active count
5. Negative stock prevented (hard constraint)
6. Stock levels cached in Redis, invalidated on changes
7. All transactions include correlationId for tracing
8. Reserved stock prevents overselling
9. Stock movement history retained indefinitely for analytics
10. Turnover rates calculated quarterly for analysis
