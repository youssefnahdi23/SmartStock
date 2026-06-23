# Customer Service API

**Base URL:** `/api/v1/customers`  
**Service Port:** 8006  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Customer-specific permissions  
**Status:** Core Service

---

## Overview

The Customer Service manages customer profiles, contact information, addresses, customer segments, customer satisfaction metrics, and customer communication history.

---

## Endpoints

### 1. Create Customer

**Endpoint:** `POST /api/v1/customers`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:create`

**Request Body:**
```json
{
  "name": "ABC Corporation",
  "code": "CUST-001",
  "type": "BUSINESS",
  "status": "ACTIVE",
  "taxId": "98-7654321",
  "businessLicense": "BL-2024-654321",
  "segment": "ENTERPRISE",
  "contact": {
    "email": "contact@abccorp.com",
    "phone": "+1-212-555-0100",
    "website": "https://www.abccorp.com"
  },
  "billingAddress": {
    "address": "789 Corporate Blvd, New York, NY 10002",
    "city": "New York",
    "state": "NY",
    "country": "USA",
    "zipCode": "10002"
  },
  "shippingAddress": {
    "address": "789 Corporate Blvd, New York, NY 10002",
    "city": "New York",
    "state": "NY",
    "country": "USA",
    "zipCode": "10002"
  },
  "paymentTerms": {
    "currency": "USD",
    "paymentMethod": "NET_30",
    "creditLimit": 100000.00
  },
  "primaryContact": {
    "firstName": "Robert",
    "lastName": "Procurement",
    "email": "robert.procurement@abccorp.com",
    "phone": "+1-212-555-0101"
  },
  "notes": "Long-term customer with excellent payment history"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "customer-001",
    "name": "ABC Corporation",
    "code": "CUST-001",
    "type": "BUSINESS",
    "status": "ACTIVE",
    "segment": "ENTERPRISE",
    "taxId": "98-7654321",
    "contact": {
      "email": "contact@abccorp.com",
      "phone": "+1-212-555-0100",
      "website": "https://www.abccorp.com"
    },
    "creditLimit": 100000.00,
    "creditUsed": 0,
    "creditAvailable": 100000.00,
    "totalOrders": 0,
    "totalOrderValue": 0,
    "satisfactionRating": 0,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Customer Details

**Endpoint:** `GET /api/v1/customers/{customerId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:read`

**Response (200 OK):**
```json
{
  "data": {
    "id": "customer-001",
    "name": "ABC Corporation",
    "code": "CUST-001",
    "type": "BUSINESS",
    "status": "ACTIVE",
    "segment": "ENTERPRISE",
    "taxId": "98-7654321",
    "contact": {
      "email": "contact@abccorp.com",
      "phone": "+1-212-555-0100",
      "website": "https://www.abccorp.com"
    },
    "billingAddress": {
      "address": "789 Corporate Blvd, New York, NY 10002",
      "city": "New York",
      "state": "NY",
      "country": "USA",
      "zipCode": "10002"
    },
    "paymentTerms": {
      "currency": "USD",
      "paymentMethod": "NET_30",
      "creditLimit": 100000.00,
      "creditUsed": 35000.00,
      "creditAvailable": 65000.00
    },
    "metrics": {
      "totalOrders": 125,
      "totalOrderValue": 625000.00,
      "averageOrderValue": 5000.00,
      "lastOrderDate": "2026-06-19T14:30:00Z",
      "satisfactionRating": 4.8,
      "paymentHistoryScore": 98.0,
      "repeatPurchaseRate": 95.0
    },
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

### 3. List Customers (Paginated)

**Endpoint:** `GET /api/v1/customers`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:read`

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `segment` (string) - Filter by customer segment
- `type` (string) - Filter by customer type
- `status` (string) - Filter by status
- `minRating` (number, 0-5) - Filter by minimum satisfaction rating
- `search` (string) - Search customer name or code

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "customer-001",
      "name": "ABC Corporation",
      "code": "CUST-001",
      "type": "BUSINESS",
      "segment": "ENTERPRISE",
      "status": "ACTIVE",
      "totalOrders": 125,
      "totalOrderValue": 625000.00,
      "satisfactionRating": 4.8,
      "creditAvailable": 65000.00,
      "lastOrderDate": "2026-06-19T14:30:00Z"
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

### 4. Update Customer

**Endpoint:** `PUT /api/v1/customers/{customerId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:write`

**Request Body:**
```json
{
  "name": "ABC Corporation - Updated",
  "segment": "PREMIUM",
  "paymentTerms": {
    "creditLimit": 150000.00
  },
  "primaryContact": {
    "firstName": "Robert",
    "lastName": "Director",
    "email": "robert.director@abccorp.com"
  }
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "customer-001",
    "name": "ABC Corporation - Updated",
    "segment": "PREMIUM",
    "paymentTerms": {
      "creditLimit": 150000.00
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

### 5. Get Customer Orders

**Endpoint:** `GET /api/v1/customers/{customerId}/orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:read`

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
      "orderId": "SO-2026-001",
      "orderDate": "2026-06-15",
      "dueDate": "2026-06-25",
      "status": "PROCESSING",
      "totalQuantity": 500,
      "totalValue": 5000.00,
      "products": 5,
      "shipmentStatus": "IN_TRANSIT"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 125,
    "traceId": "trace-123"
  }
}
```

---

### 6. Get Customer Metrics

**Endpoint:** `GET /api/v1/customers/{customerId}/metrics`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:report`

**Query Parameters:**
- `fromDate` (ISO8601Date) - From date for metrics
- `toDate` (ISO8601Date) - To date for metrics

**Response (200 OK):**
```json
{
  "data": {
    "customerId": "customer-001",
    "customerName": "ABC Corporation",
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "orderMetrics": {
      "totalOrders": 25,
      "averageOrderValue": 5000.00,
      "totalOrderValue": 125000.00,
      "averageOrderFrequency": "9.6 days"
    },
    "paymentMetrics": {
      "totalPaid": 125000.00,
      "paymentHistoryScore": 98.0,
      "onTimePaymentRate": 96.0,
      "averagePaymentDaysLate": 0.5
    },
    "productMetrics": {
      "uniqueProducts": 45,
      "mostOrderedProduct": {
        "productId": "prod-001",
        "productName": "Premium Widget",
        "orderedQuantity": 2500
      }
    },
    "satisfactionMetrics": {
      "satisfactionRating": 4.8,
      "netPromoterScore": 75,
      "complaintsCount": 2,
      "complaintResolutionTime": "2.5 days"
    },
    "trends": {
      "orderVolumeTrend": "INCREASING",
      "satisfactionTrend": "STABLE",
      "loyaltyTrend": "IMPROVING"
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Record Customer Satisfaction

**Endpoint:** `POST /api/v1/customers/{customerId}/satisfaction`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:feedback`

**Request Body:**
```json
{
  "orderId": "SO-2026-001",
  "rating": 5,
  "category": "PRODUCT_QUALITY",
  "feedback": "Excellent product quality and on-time delivery",
  "recommendToOthers": true
}
```

**Response (201 Created):**
```json
{
  "data": {
    "feedbackId": "feedback-123",
    "customerId": "customer-001",
    "orderId": "SO-2026-001",
    "rating": 5,
    "category": "PRODUCT_QUALITY",
    "feedback": "Excellent product quality and on-time delivery",
    "recommendToOthers": true,
    "createdAt": "2026-06-20T12:00:00Z",
    "customerSatisfactionAverage": 4.8,
    "netPromoterScore": 75
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 8. Add Customer Address

**Endpoint:** `POST /api/v1/customers/{customerId}/addresses`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:write`

**Request Body:**
```json
{
  "label": "Warehouse",
  "type": "SHIPPING",
  "address": "999 Warehouse Way, Newark, NJ 07001",
  "city": "Newark",
  "state": "NJ",
  "country": "USA",
  "zipCode": "07001",
  "isDefault": false
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "addr-123",
    "customerId": "customer-001",
    "label": "Warehouse",
    "type": "SHIPPING",
    "address": "999 Warehouse Way, Newark, NJ 07001",
    "city": "Newark",
    "state": "NJ",
    "country": "USA",
    "zipCode": "07001",
    "isDefault": false,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 9. Get Customer Addresses

**Endpoint:** `GET /api/v1/customers/{customerId}/addresses`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:read`

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "addr-123",
      "label": "Main Office",
      "type": "BILLING",
      "address": "789 Corporate Blvd, New York, NY 10002",
      "city": "New York",
      "isDefault": true
    },
    {
      "id": "addr-124",
      "label": "Warehouse",
      "type": "SHIPPING",
      "address": "999 Warehouse Way, Newark, NJ 07001",
      "city": "Newark",
      "isDefault": false
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 10. Suspend Customer

**Endpoint:** `POST /api/v1/customers/{customerId}/suspend`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:write`

**Request Body:**
```json
{
  "reason": "Exceeding credit limit and overdue payments",
  "resumeDate": "2026-07-15",
  "notes": "Customer to settle outstanding balance before resuming"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "customer-001",
    "name": "ABC Corporation",
    "status": "SUSPENDED",
    "suspendedAt": "2026-06-20T12:00:00Z",
    "resumeDate": "2026-07-15",
    "reason": "Exceeding credit limit and overdue payments"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 11. Resume Customer

**Endpoint:** `POST /api/v1/customers/{customerId}/resume`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:write`

**Response (200 OK):**
```json
{
  "data": {
    "id": "customer-001",
    "name": "ABC Corporation",
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

### 12. Get Customer Segment Analysis

**Endpoint:** `GET /api/v1/customers/analysis/segments`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `customer:report`

**Query Parameters:**
- `fromDate` (ISO8601Date) - From date for analysis
- `toDate` (ISO8601Date) - To date for analysis

**Response (200 OK):**
```json
{
  "data": {
    "reportDate": "2026-06-20",
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "segments": [
      {
        "segment": "ENTERPRISE",
        "customerCount": 25,
        "totalOrderValue": 1250000.00,
        "averageOrderValue": 50000.00,
        "averageSatisfactionRating": 4.7,
        "repeatPurchaseRate": 92.0
      },
      {
        "segment": "PREMIUM",
        "customerCount": 75,
        "totalOrderValue": 375000.00,
        "averageOrderValue": 5000.00,
        "averageSatisfactionRating": 4.5,
        "repeatPurchaseRate": 85.0
      }
    ],
    "totalCustomers": 450,
    "totalOrderValue": 2250000.00
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### CustomerDTO
```typescript
{
  id: UUID;
  name: string;
  code: string;
  type: "BUSINESS" | "INDIVIDUAL";
  status: "ACTIVE" | "INACTIVE" | "SUSPENDED";
  segment: "ENTERPRISE" | "PREMIUM" | "STANDARD" | "OCCASIONAL";
  taxId: string;
  contact: {
    email: string;
    phone: string;
    website?: string;
  };
  paymentTerms: {
    currency: string;
    paymentMethod: string;
    creditLimit: BigDecimal;
    creditUsed: BigDecimal;
  };
  totalOrders: number;
  totalOrderValue: BigDecimal;
  satisfactionRating: number;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

### CustomerMetricsDTO
```typescript
{
  customerId: UUID;
  totalOrders: number;
  totalOrderValue: BigDecimal;
  averageOrderValue: BigDecimal;
  satisfactionRating: number;
  paymentHistoryScore: number;
  repeatPurchaseRate: number;
  lastOrderDate: ISO8601DateTime;
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| CUSTOMER_NOT_FOUND | 404 | Customer does not exist |
| CUSTOMER_CODE_EXISTS | 400 | Customer code already in use |
| CREDIT_LIMIT_EXCEEDED | 400 | Credit limit exceeded |
| CUSTOMER_SUSPENDED | 400 | Customer is suspended |
| VALIDATION_FAILED | 422 | Invalid input |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |

---

## Events Published

- `CustomerCreated` - New customer created
- `CustomerUpdated` - Customer information updated
- `CustomerSuspended` - Customer suspended
- `CustomerResumed` - Customer resumed
- `CustomerSatisfactionRecorded` - Satisfaction feedback recorded
- `CustomerOrderPlaced` - Customer placed order
- `CreditLimitExceeded` - Customer exceeded credit limit
- `PaymentReceived` - Payment received from customer
- `CustomerSegmentChanged` - Customer segment changed

---

## Implementation Notes

1. Customer satisfaction ratings collected per order
2. Net Promoter Score calculated from satisfaction data
3. Credit limits prevent overselling to risky customers
4. Payment history score affects credit availability
5. Customer suspension prevents new orders but retains historical data
6. Repeat purchase rate analyzed using 90-day rolling window
7. Customer segment determined by order volume and payment history
8. Multiple shipping addresses supported per customer
9. Payment method stored securely for automated billing
10. Customer communication history maintained for CRM integration
