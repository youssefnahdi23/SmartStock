# Audit Service API

**Base URL:** `/api/v1/audit`  
**Service Port:** 8009  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Audit log read permissions  
**Status:** Critical Infrastructure Service

---

## Overview

The Audit Service provides immutable audit logging for all business operations, compliance tracking, and forensic investigation capabilities across the SmartStock AI platform.

---

## Endpoints

### 1. Get Audit Logs (Paginated)

**Endpoint:** `GET /api/v1/audit/logs`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read` (Auditor role minimum)

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 50) - Results per page
- `userId` (UUID) - Filter by user who performed action
- `entityType` (string) - Entity type (Product, Order, Inventory, etc.)
- `entityId` (UUID) - Filter by entity ID
- `actionType` (string) - CREATE, UPDATE, DELETE, VIEW, LOGIN, etc.
- `severity` (string) - INFO, WARNING, ERROR, CRITICAL
- `fromDate` (ISO8601DateTime) - From timestamp
- `toDate` (ISO8601DateTime) - To timestamp
- `search` (string) - Search in audit description

**Response (200 OK):**
```json
{
  "data": [
    {
      "auditId": "audit-123",
      "timestamp": "2026-06-20T12:00:00Z",
      "userId": "user-123",
      "username": "john.operator",
      "actionType": "STOCK_IN",
      "entityType": "INVENTORY",
      "entityId": "inv-123",
      "description": "Stock in: 100 units of Premium Widget received from supplier",
      "severity": "INFO",
      "status": "SUCCESS",
      "ipAddress": "192.168.1.100",
      "userAgent": "JavaFX-Client/1.0",
      "correlationId": "corr-123",
      "changes": {
        "productId": "prod-001",
        "quantity": 100,
        "warehouseId": "W01",
        "transactionValue": 4500.00
      }
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 50,
    "total": 125000,
    "totalPages": 2500,
    "traceId": "trace-123"
  }
}
```

---

### 2. Get User Activity Report

**Endpoint:** `GET /api/v1/audit/users/{userId}/activity`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read`

**Query Parameters:**
- `fromDate` (ISO8601DateTime) - From timestamp
- `toDate` (ISO8601DateTime) - To timestamp
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 50) - Results per page

**Response (200 OK):**
```json
{
  "data": {
    "userId": "user-123",
    "username": "john.operator",
    "email": "john@company.com",
    "reportPeriod": {
      "fromDate": "2026-06-01",
      "toDate": "2026-06-20"
    },
    "activitySummary": {
      "totalActions": 1250,
      "successfulActions": 1245,
      "failedActions": 5,
      "successRate": 99.6
    },
    "actionBreakdown": {
      "STOCK_IN": 450,
      "STOCK_OUT": 380,
      "ADJUSTMENT": 120,
      "VIEW": 250,
      "CREATE": 50
    },
    "loginHistory": {
      "totalLogins": 20,
      "lastLogin": "2026-06-20T08:00:00Z",
      "failedLoginAttempts": 0
    },
    "dataAccessSummary": {
      "totalRecordsAccessed": 2500,
      "sensitiveDataAccessed": 50,
      "exportedRecords": 100
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. Get Entity Audit Trail

**Endpoint:** `GET /api/v1/audit/entities/{entityType}/{entityId}/history`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read`

**Response (200 OK):**
```json
{
  "data": {
    "entityType": "PRODUCT",
    "entityId": "prod-001",
    "entityName": "Premium Widget",
    "history": [
      {
        "timestamp": "2026-06-20T12:00:00Z",
        "actionType": "CREATE",
        "userId": "user-100",
        "username": "admin",
        "changes": {
          "name": "Premium Widget",
          "sku": "WDG-001-PREM",
          "unitPrice": 99.99
        }
      },
      {
        "timestamp": "2026-06-20T13:00:00Z",
        "actionType": "UPDATE",
        "userId": "user-123",
        "username": "john.operator",
        "changes": {
          "unitPrice": {
            "from": 99.99,
            "to": 109.99
          }
        }
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

### 4. Search Audit Logs (Advanced)

**Endpoint:** `POST /api/v1/audit/search`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read`

**Request Body:**
```json
{
  "filters": {
    "actionTypes": ["STOCK_OUT", "DELETE"],
    "entityTypes": ["PRODUCT", "INVENTORY"],
    "severities": ["WARNING", "ERROR", "CRITICAL"],
    "status": "FAILURE"
  },
  "timeRange": {
    "fromDate": "2026-06-01T00:00:00Z",
    "toDate": "2026-06-20T23:59:59Z"
  },
  "userIds": ["user-123", "user-124"],
  "page": 0,
  "size": 50
}
```

**Response (200 OK):**
```json
{
  "data": [
    {
      "auditId": "audit-456",
      "timestamp": "2026-06-15T14:30:00Z",
      "userId": "user-123",
      "username": "john.operator",
      "actionType": "STOCK_OUT",
      "entityType": "INVENTORY",
      "entityId": "inv-456",
      "description": "Stock out failed: Insufficient stock",
      "severity": "ERROR",
      "status": "FAILURE",
      "errorMessage": "Inventory insufficient for requested quantity"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "total": 125,
    "traceId": "trace-123"
  }
}
```

---

### 5. Get Compliance Report

**Endpoint:** `GET /api/v1/audit/compliance`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read`

**Query Parameters:**
- `fromDate` (ISO8601DateTime) - From date
- `toDate` (ISO8601DateTime) - To date
- `complianceType` (string) - SARBANES-OXLEY, GDPR, SOC2, etc.

**Response (200 OK):**
```json
{
  "data": {
    "reportDate": "2026-06-20",
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "complianceStatus": "COMPLIANT",
    "audits": {
      "totalAuditLogs": 125000,
      "criticalEvents": 12,
      "warningEvents": 450,
      "unauthorizedAccessAttempts": 5,
      "dataModificationAttempts": 0
    },
    "userAccessControl": {
      "usersWithExcessivePrivileges": 0,
      "usersWithoutMultiFactor": 15,
      "inactiveUsers": 8
    },
    "dataIntegrity": {
      "failedDataValidations": 2,
      "unauthorizedDataAccess": 0,
      "inconsistencies": 0
    },
    "recommendations": [
      {
        "priority": "HIGH",
        "recommendation": "Enable MFA for 15 users",
        "status": "OPEN"
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

### 6. Export Audit Report

**Endpoint:** `GET /api/v1/audit/export`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `audit:read`

**Query Parameters:**
- `fromDate` (ISO8601DateTime) - From date
- `toDate` (ISO8601DateTime) - To date
- `format` (string, default: "csv") - csv, excel, json

**Response (200 OK):**
- Content-Type: `text/csv` or `application/json`
- Content-Disposition: `attachment; filename="audit-report-2026-06-20.csv"`

---

## Data Transfer Objects

### AuditLogDTO
```typescript
{
  auditId: UUID;
  timestamp: ISO8601DateTime;
  userId: UUID;
  username: string;
  actionType: string;
  entityType: string;
  entityId: UUID;
  description: string;
  severity: "INFO" | "WARNING" | "ERROR" | "CRITICAL";
  status: "SUCCESS" | "FAILURE";
  ipAddress: string;
  changes: Record<string, any>;
  correlationId: string;
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| UNAUTHORIZED | 401 | Invalid or expired token |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks audit read permissions |
| VALIDATION_FAILED | 422 | Invalid search criteria |

---

## Implementation Notes

1. All audit logs immutable - no deletion or modification
2. Minimum 7 years retention for compliance
3. Audit logs encrypted at rest
4. Failed actions logged with error details
5. Sensitive data (passwords, tokens) never logged
6. Correlation IDs enable tracing across service calls
7. User actions tracked with IP address and user agent
8. Batch operations tracked at summary and detail level
9. Export filtered by user permissions
10. Real-time alerting for critical/suspicious events
