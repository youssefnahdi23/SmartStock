# Supplier Service API

**Base URL:** `/api/v1/suppliers`  
**Service Port:** 8005  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Supplier-specific permissions  
**Status:** Core Service

---

## Overview

The Supplier Service manages supplier profiles, contact information, delivery performance metrics, supplier agreements, and supplier performance ratings.

---

## Endpoints

### 1. Create Supplier

**Endpoint:** `POST /api/v1/suppliers`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:create`

**Request Body:**
```json
{
  "name": "Premium Widgets Ltd",
  "code": "SUPP-001",
  "type": "MANUFACTURER",
  "status": "ACTIVE",
  "taxId": "12-3456789",
  "businessLicense": "BL-2025-123456",
  "contact": {
    "email": "contact@premiumwidgets.com",
    "phone": "+1-800-WIDGETS",
    "website": "https://www.premiumwidgets.com"
  },
  "primaryAddress": {
    "address": "456 Industrial Park, Los Angeles, CA 90001",
    "city": "Los Angeles",
    "state": "CA",
    "country": "USA",
    "zipCode": "90001"
  },
  "billingAddress": {
    "address": "456 Industrial Park, Los Angeles, CA 90001",
    "city": "Los Angeles",
    "state": "CA",
    "country": "USA",
    "zipCode": "90001"
  },
  "paymentTerms": {
    "currency": "USD",
    "paymentMethod": "BANK_TRANSFER",
    "termsDays": 30,
    "discountPercentage": 2.0
  },
  "primaryContact": {
    "firstName": "John",
    "lastName": "Sales",
    "email": "john.sales@premiumwidgets.com",
    "phone": "+1-800-WIDGETS-1"
  },
  "certifications": ["ISO9001", "ISO14001"],
  "notes": "Preferred supplier for premium widgets"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "supplier-001",
    "name": "Premium Widgets Ltd",
    "code": "SUPP-001",
    "type": "MANUFACTURER",
    "status": "ACTIVE",
    "taxId": "12-3456789",
    "contact": {
      "email": "contact@premiumwidgets.com",
      "phone": "+1-800-WIDGETS",
      "website": "https://www.premiumwidgets.com"
    },
    "paymentTerms": {
      "currency": "USD",
      "paymentMethod": "BANK_TRANSFER",
      "termsDays": 30,
      "discountPercentage": 2.0
    },
    "certifications": ["ISO9001", "ISO14001"],
    "performanceRating": 0,
    "totalOrders": 0,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Supplier Details

**Endpoint:** `GET /api/v1/suppliers/{supplierId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:read`

**Response (200 OK):**
```json
{
  "data": {
    "id": "supplier-001",
    "name": "Premium Widgets Ltd",
    "code": "SUPP-001",
    "type": "MANUFACTURER",
    "status": "ACTIVE",
    "taxId": "12-3456789",
    "businessLicense": "BL-2025-123456",
    "contact": {
      "email": "contact@premiumwidgets.com",
      "phone": "+1-800-WIDGETS",
      "website": "https://www.premiumwidgets.com"
    },
    "primaryAddress": {
      "address": "456 Industrial Park, Los Angeles, CA 90001",
      "city": "Los Angeles",
      "state": "CA",
      "country": "USA",
      "zipCode": "90001"
    },
    "paymentTerms": {
      "currency": "USD",
      "paymentMethod": "BANK_TRANSFER",
      "termsDays": 30,
      "discountPercentage": 2.0
    },
    "certifications": ["ISO9001", "ISO14001"],
    "performanceMetrics": {
      "rating": 4.7,
      "totalOrders": 250,
      "totalOrderValue": 1250000.00,
      "onTimeDeliveryRate": 98.5,
      "qualityScore": 96.0,
      "responseTimeHours": 2.3,
      "averageLeadTimeDays": 7.5
    },
    "products": 45,
    "lastOrderDate": "2026-06-19T14:30:00Z",
    "createdAt": "2026-06-20T12:00:00Z",
    "updatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. List Suppliers (Paginated)

**Endpoint:** `GET /api/v1/suppliers`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `type` (string) - Filter by supplier type
- `status` (string) - Filter by status
- `minRating` (number, 0-5) - Filter by minimum performance rating
- `search` (string) - Search supplier name or code

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "supplier-001",
      "name": "Premium Widgets Ltd",
      "code": "SUPP-001",
      "type": "MANUFACTURER",
      "status": "ACTIVE",
      "email": "contact@premiumwidgets.com",
      "phone": "+1-800-WIDGETS",
      "performanceRating": 4.7,
      "onTimeDeliveryRate": 98.5,
      "totalOrders": 250,
      "lastOrderDate": "2026-06-19T14:30:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 125,
    "totalPages": 7,
    "traceId": "trace-123"
  }
}
```

---

### 4. Update Supplier

**Endpoint:** `PUT /api/v1/suppliers/{supplierId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:write`

**Request Body:**
```json
{
  "name": "Premium Widgets Ltd - Updated",
  "paymentTerms": {
    "termsDays": 45,
    "discountPercentage": 3.0
  },
  "primaryContact": {
    "firstName": "Jane",
    "lastName": "Sales",
    "email": "jane.sales@premiumwidgets.com"
  }
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "supplier-001",
    "name": "Premium Widgets Ltd - Updated",
    "paymentTerms": {
      "termsDays": 45,
      "discountPercentage": 3.0
    },
    "updatedAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Get Supplier Performance Report

**Endpoint:** `GET /api/v1/suppliers/{supplierId}/performance`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:report`

**Query Parameters:**
- `fromDate` (ISO8601Date) - Start date for report
- `toDate` (ISO8601Date) - End date for report

**Response (200 OK):**
```json
{
  "data": {
    "supplierId": "supplier-001",
    "supplierName": "Premium Widgets Ltd",
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "metrics": {
      "totalOrders": 50,
      "totalOrderValue": 250000.00,
      "averageOrderValue": 5000.00,
      "onTimeDeliveryRate": 98.5,
      "onTimeDeliveries": 49,
      "lateDeliveries": 1,
      "qualityScore": 96.0,
      "defectiveUnitsCount": 400,
      "responseTimeHours": 2.3,
      "averageLeadTimeDays": 7.5,
      "minimumLeadTimeDays": 5,
      "maximumLeadTimeDays": 14,
      "overallRating": 4.7
    },
    "topProducts": [
      {
        "productId": "prod-001",
        "productName": "Premium Widget",
        "orderedQuantity": 5000,
        "totalValue": 50000.00
      }
    ],
    "trends": {
      "orderVolumeTrend": "INCREASING",
      "qualityTrend": "STABLE",
      "deliveryPerformanceTrend": "IMPROVING"
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Get Supplier Products

**Endpoint:** `GET /api/v1/suppliers/{supplierId}/products`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page

**Response (200 OK):**
```json
{
  "data": [
    {
      "productId": "prod-001",
      "productName": "Premium Widget",
      "sku": "WDG-001-PREM",
      "supplierSku": "SUPP-WDG-001",
      "unitPrice": 45.00,
      "minimumOrder": 100,
      "leadTimeDays": 7,
      "lastOrderDate": "2026-06-19T14:30:00Z",
      "totalOrdered": 5000
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 45,
    "traceId": "trace-123"
  }
}
```

---

### 7. Get Supplier Order History

**Endpoint:** `GET /api/v1/suppliers/{supplierId}/orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `status` (string) - Filter by order status
- `fromDate` (ISO8601Date) - From date
- `toDate` (ISO8601Date) - To date

**Response (200 OK):**
```json
{
  "data": [
    {
      "orderId": "PO-2026-001",
      "orderDate": "2026-06-01",
      "deliveryDate": "2026-06-10",
      "status": "DELIVERED",
      "totalQuantity": 1000,
      "totalValue": 45000.00,
      "products": 5,
      "onTime": true
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 250,
    "traceId": "trace-123"
  }
}
```

---

### 8. Suspend Supplier

**Endpoint:** `POST /api/v1/suppliers/{supplierId}/suspend`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:write`

**Request Body:**
```json
{
  "reason": "Quality issues - multiple defective batches received",
  "resumeDate": "2026-07-01",
  "notes": "Supplier to address quality control procedures before resumption"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "supplier-001",
    "name": "Premium Widgets Ltd",
    "status": "SUSPENDED",
    "suspendedAt": "2026-06-20T12:00:00Z",
    "resumeDate": "2026-07-01",
    "reason": "Quality issues - multiple defective batches received"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 9. Resume Supplier

**Endpoint:** `POST /api/v1/suppliers/{supplierId}/resume`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `supplier:write`

**Response (200 OK):**
```json
{
  "data": {
    "id": "supplier-001",
    "name": "Premium Widgets Ltd",
    "status": "ACTIVE",
    "resumedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### SupplierDTO
```typescript
{
  id: UUID;
  name: string;
  code: string;
  type: "MANUFACTURER" | "DISTRIBUTOR" | "SERVICE_PROVIDER";
  status: "ACTIVE" | "INACTIVE" | "SUSPENDED";
  taxId: string;
  contact: {
    email: string;
    phone: string;
    website: string;
  };
  paymentTerms: {
    currency: string;
    paymentMethod: string;
    termsDays: number;
    discountPercentage: number;
  };
  performanceRating: number;
  totalOrders: number;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

### SupplierPerformanceDTO
```typescript
{
  supplierId: UUID;
  totalOrders: number;
  onTimeDeliveryRate: number;
  qualityScore: number;
  responseTimeHours: number;
  averageLeadTimeDays: number;
  overallRating: number;
  trends: { orderVolumeTrend: string; qualityTrend: string };
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| SUPPLIER_NOT_FOUND | 404 | Supplier does not exist |
| SUPPLIER_CODE_EXISTS | 400 | Supplier code already in use |
| VALIDATION_FAILED | 422 | Invalid input |
| SUPPLIER_SUSPENDED | 400 | Supplier is suspended |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |

---

## Events Published

- `SupplierCreated` - New supplier created
- `SupplierUpdated` - Supplier information updated
- `SupplierSuspended` - Supplier suspended
- `SupplierResumed` - Supplier resumed
- `SupplierPerformanceUpdated` - Supplier performance metrics updated
- `SupplierDeliveryRegistered` - Delivery from supplier registered
- `SupplierQualityIssue` - Quality issue reported

---

## Implementation Notes

1. Supplier performance metrics calculated monthly
2. On-time delivery rate requires delivery confirmation
3. Quality score based on defect reports
4. Suspension prevents new orders but retains historical data
5. Payment terms used for purchase order creation
6. Certifications tracked for compliance reporting
7. Supplier contact history maintained for audit trail
8. Performance trends analyzed using 90-day rolling average
9. Preferred supplier status used for ordering recommendations
10. Supplier deactivation possible but masks historical data
