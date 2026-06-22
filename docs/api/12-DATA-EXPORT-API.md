# Data Export Service API

**Base URL:** `/api/v1/exports`  
**Service Port:** 8012  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Data export permissions  
**Status:** Data Platform Service

---

## Overview

The Data Export Service provides high-quality structured data exports for analytics, machine learning, and business intelligence systems. Exports are designed for AI readiness with clean, consistent schemas.

---

## Endpoints

### 1. Export Stock Movements

**Endpoint:** `POST /api/v1/exports/stock-movements`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `export:data`

**Request Body:**
```json
{
  "exportId": "exp-001",
  "fromDate": "2026-01-01",
  "toDate": "2026-06-30",
  "format": "PARQUET",
  "filters": {
    "warehouseIds": ["W01", "W02"],
    "movementTypes": ["IN", "OUT", "TRANSFER"]
  },
  "schedule": "SCHEDULED",
  "deliveryMethod": "S3",
  "s3Config": {
    "bucket": "smartstock-data-lake",
    "prefix": "stock-movements/2026-Q2/"
  }
}
```

**Response (202 Accepted):**
```json
{
  "data": {
    "exportId": "exp-001",
    "status": "QUEUED",
    "format": "PARQUET",
    "estimatedRecords": 150000,
    "estimatedSize": "25 MB",
    "deliveryMethod": "S3",
    "scheduledAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Stock Movements Schema:**
```parquet
root
 |-- eventId: string (unique identifier)
 |-- eventType: string (IN, OUT, TRANSFER, ADJUSTMENT)
 |-- timestamp: timestamp (UTC)
 |-- productId: string
 |-- warehouseId: string
 |-- quantity: integer
 |-- unitCost: decimal(10,2)
 |-- totalValue: decimal(15,2)
 |-- userId: string
 |-- reason: string
 |-- correlationId: string
 |-- metadata: struct
      |-- supplierId: string
      |-- orderId: string
      |-- customerId: string
```

---

### 2. Export Historical Inventory Snapshots

**Endpoint:** `POST /api/v1/exports/inventory-snapshots`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `export:data`

**Request Body:**
```json
{
  "exportId": "exp-002",
  "frequency": "DAILY",
  "fromDate": "2026-04-01",
  "toDate": "2026-06-30",
  "format": "PARQUET",
  "deliveryMethod": "S3",
  "s3Config": {
    "bucket": "smartstock-data-lake",
    "prefix": "inventory-snapshots/daily/"
  }
}
```

**Response (202 Accepted):**
```json
{
  "data": {
    "exportId": "exp-002",
    "status": "QUEUED",
    "format": "PARQUET",
    "frequency": "DAILY",
    "estimatedSnapshots": 91,
    "estimatedSize": "150 MB"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Inventory Snapshots Schema:**
```parquet
root
 |-- snapshotDate: date
 |-- productId: string
 |-- warehouseId: string
 |-- currentStock: integer
 |-- reservedStock: integer
 |-- availableStock: integer
 |-- unitCost: decimal(10,2)
 |-- totalValue: decimal(15,2)
 |-- turnoverRate: decimal(5,2)
 |-- daysInInventory: integer
 |-- status: string (OPTIMAL, LOW, CRITICAL, OVERSTOCK)
```

---

### 3. Export Sales and Purchase Orders

**Endpoint:** `POST /api/v1/exports/orders`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `export:data`

**Request Body:**
```json
{
  "exportId": "exp-003",
  "orderTypes": ["SALES", "PURCHASE"],
  "fromDate": "2026-01-01",
  "toDate": "2026-06-30",
  "format": "CSV",
  "includeLineItems": true,
  "includeTimestamps": true,
  "deliveryMethod": "EMAIL",
  "emailConfig": {
    "recipients": ["datateam@company.com"]
  }
}
```

**Response (202 Accepted):**
```json
{
  "data": {
    "exportId": "exp-003",
    "status": "PROCESSING",
    "format": "CSV",
    "estimatedRecords": 5000,
    "estimatedSize": "8 MB"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 4. Export Supplier Performance Data

**Endpoint:** `POST /api/v1/exports/supplier-data`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `export:data`

**Request Body:**
```json
{
  "exportId": "exp-004",
  "fromDate": "2026-01-01",
  "toDate": "2026-06-30",
  "format": "PARQUET",
  "includeDeliveryHistory": true,
  "includeQualityMetrics": true,
  "includeFinancialData": false,
  "deliveryMethod": "S3",
  "s3Config": {
    "bucket": "smartstock-data-lake",
    "prefix": "suppliers/performance/"
  }
}
```

**Response (202 Accepted):**
```json
{
  "data": {
    "exportId": "exp-004",
    "status": "QUEUED",
    "format": "PARQUET",
    "estimatedRecords": 500,
    "estimatedSize": "5 MB"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Get Export Status

**Endpoint:** `GET /api/v1/exports/{exportId}`  
**Authentication:** Required (JWT Bearer Token)

**Response (200 OK):**
```json
{
  "data": {
    "exportId": "exp-001",
    "status": "COMPLETED",
    "format": "PARQUET",
    "recordsExported": 150000,
    "actualSize": "24 MB",
    "completedAt": "2026-06-20T13:30:00Z",
    "s3Location": "s3://smartstock-data-lake/stock-movements/2026-Q2/export-001.parquet",
    "dataQualityReport": {
      "validRecords": 150000,
      "nullRecords": 0,
      "duplicateRecords": 0,
      "dataCompleteness": 100.0
    }
  },
  "meta": {
    "timestamp": "2026-06-20T14:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Get Export History

**Endpoint:** `GET /api/v1/exports/history`  
**Authentication:** Required (JWT Bearer Token)

**Query Parameters:**
- `page` (integer, default: 0)
- `size` (integer, default: 20)
- `status` (string, optional) - QUEUED, PROCESSING, COMPLETED, FAILED
- `format` (string, optional) - PARQUET, CSV, JSON

**Response (200 OK):**
```json
{
  "data": [
    {
      "exportId": "exp-001",
      "status": "COMPLETED",
      "format": "PARQUET",
      "recordsExported": 150000,
      "createdAt": "2026-06-20T12:00:00Z",
      "completedAt": "2026-06-20T13:30:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T14:00:00Z",
    "page": 0,
    "total": 45,
    "traceId": "trace-123"
  }
}
```

---

### 7. Schedule Recurring Export

**Endpoint:** `POST /api/v1/exports/schedule`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `export:schedule`

**Request Body:**
```json
{
  "name": "Daily Stock Movements Export",
  "exportType": "STOCK_MOVEMENTS",
  "schedule": "DAILY",
  "scheduleTime": "02:00",
  "timezone": "UTC",
  "format": "PARQUET",
  "retentionDays": 90,
  "deliveryMethod": "S3",
  "s3Config": {
    "bucket": "smartstock-data-lake",
    "prefix": "stock-movements/daily/"
  },
  "active": true
}
```

**Response (201 Created):**
```json
{
  "data": {
    "scheduleId": "sched-001",
    "name": "Daily Stock Movements Export",
    "schedule": "DAILY",
    "nextRun": "2026-06-21T02:00:00Z",
    "status": "ACTIVE"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Quality Assurance

Every export includes:
1. Schema validation
2. Data type validation
3. Null value checking
4. Duplicate detection
5. Referential integrity verification
6. Timestamp consistency checks

---

## Events Published

- `ExportScheduled` - Export scheduled successfully
- `ExportStarted` - Export processing began
- `ExportCompleted` - Export finished successfully
- `ExportFailed` - Export failed with error
- `DataDelivered` - Data delivered to target system

---

## Implementation Notes

1. Exports stored in MinIO-compatible object storage
2. Parquet format optimized for ML/analytics pipelines
3. Data partitioned by date for efficient querying
4. Quality assurance report generated per export
5. Schema versioning for backward compatibility
6. Incremental exports supported (delta only)
7. Data masking available for PII (future)
8. Retention policies configurable per export type
9. Export history retained indefinitely
10. Audit trail logs all export operations
11. Multi-format support: Parquet, CSV, JSON
12. Compression enabled by default (Snappy)
13. Data pipeline triggers available for scheduled exports
