# Reporting Service API

**Base URL:** `/api/v1/reports`  
**Service Port:** 8011  
**Authentication:** JWT Bearer Token (required for all endpoints)  
**Authorization:** RBAC - Reporting permissions  
**Status:** Analytics Service

---

## Overview

The Reporting Service provides business intelligence, KPI dashboards, inventory analysis, and executive summaries for decision-making.

---

## Endpoints

### 1. Get Inventory Summary Report

**Endpoint:** `GET /api/v1/reports/inventory-summary`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:view`

**Query Parameters:**
- `warehouseId` (UUID, optional) - Specific warehouse only
- `date` (ISO8601Date, default: today) - Report date

**Response (200 OK):**
```json
{
  "data": {
    "reportDate": "2026-06-20",
    "inventorySummary": {
      "totalSKUs": 1234,
      "totalUnits": 450000,
      "totalInventoryValue": 45000000.00,
      "averageUnitCost": 100.00,
      "lowestPriceProduct": "Basic Widget",
      "highestPriceProduct": "Premium Widget Plus"
    },
    "warehouseBreakdown": [
      {
        "warehouseId": "W01",
        "warehouseName": "Main Warehouse",
        "totalUnits": 200000,
        "inventoryValue": 20000000.00,
        "utilizationPercentage": 70.0
      }
    ],
    "stockLevelStatus": {
      "optimalStock": 800,
      "lowStock": 250,
      "criticalStock": 50,
      "overstock": 100,
      "zeroStock": 34
    },
    "turnoverMetrics": {
      "fastMovingProducts": 150,
      "slowMovingProducts": 200,
      "averageTurnoverRate": 12.5
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Sales Performance Report

**Endpoint:** `GET /api/v1/reports/sales-performance`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:view`

**Query Parameters:**
- `fromDate` (ISO8601Date) - Start date
- `toDate` (ISO8601Date) - End date
- `period` (string) - DAILY, WEEKLY, MONTHLY

**Response (200 OK):**
```json
{
  "data": {
    "reportPeriod": {
      "fromDate": "2026-06-01",
      "toDate": "2026-06-20"
    },
    "salesMetrics": {
      "totalOrders": 850,
      "totalOrderValue": 4250000.00,
      "averageOrderValue": 5000.00,
      "ordersOnTime": 810,
      "onTimePercentage": 95.3
    },
    "topCustomers": [
      {
        "customerId": "customer-001",
        "customerName": "ABC Corporation",
        "orderCount": 50,
        "totalValue": 250000.00,
        "satisfactionRating": 4.8
      }
    ],
    "topProducts": [
      {
        "productId": "prod-001",
        "productName": "Premium Widget",
        "unitsSold": 50000,
        "totalValue": 500000.00
      }
    ],
    "dailyTrends": [
      {
        "date": "2026-06-20",
        "ordersCount": 45,
        "orderValue": 225000.00
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

### 3. Get Warehouse Utilization Report

**Endpoint:** `GET /api/v1/reports/warehouse-utilization`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:view`

**Response (200 OK):**
```json
{
  "data": {
    "reportDate": "2026-06-20",
    "warehouses": [
      {
        "warehouseId": "W01",
        "warehouseName": "Main Warehouse",
        "floorSpaceUtilization": {
          "total": 50000,
          "used": 35000,
          "available": 15000,
          "percentage": 70.0
        },
        "palletUtilization": {
          "total": 2000,
          "used": 1400,
          "available": 600,
          "percentage": 70.0
        },
        "zones": [
          {
            "zoneId": "zone-123",
            "zoneName": "Cold Storage",
            "utilizationPercentage": 65.0
          }
        ]
      }
    ],
    "recommendations": [
      {
        "priority": "MEDIUM",
        "warehouseId": "W01",
        "recommendation": "Rebalance inventory to underutilized zones"
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

### 4. Get Supplier Performance Report

**Endpoint:** `GET /api/v1/reports/supplier-performance`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:view`

**Query Parameters:**
- `fromDate` (ISO8601Date) - Start date
- `toDate` (ISO8601Date) - End date

**Response (200 OK):**
```json
{
  "data": {
    "reportPeriod": {
      "fromDate": "2026-01-01",
      "toDate": "2026-06-20"
    },
    "suppliers": [
      {
        "supplierId": "supplier-001",
        "supplierName": "Premium Widgets Ltd",
        "totalOrders": 50,
        "totalOrderValue": 250000.00,
        "onTimeDeliveryRate": 98.5,
        "qualityScore": 96.0,
        "responseTimeHours": 2.3,
        "overallRating": 4.7,
        "ranking": 1
      }
    ],
    "summaryMetrics": {
      "averageOnTimeDeliveryRate": 95.2,
      "averageQualityScore": 94.5
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 5. Create Custom Report

**Endpoint:** `POST /api/v1/reports/custom`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:create`

**Request Body:**
```json
{
  "name": "Q2 2026 Inventory Analysis",
  "description": "Quarterly inventory performance analysis",
  "type": "INVENTORY",
  "filters": {
    "fromDate": "2026-04-01",
    "toDate": "2026-06-30",
    "warehouseIds": ["W01", "W02"],
    "productCategories": ["cat-123"]
  },
  "metrics": [
    "inventory_value",
    "turnover_rate",
    "stock_levels",
    "slow_moving"
  ],
  "schedule": "MANUAL"
}
```

**Response (201 Created):**
```json
{
  "data": {
    "reportId": "report-custom-001",
    "name": "Q2 2026 Inventory Analysis",
    "status": "GENERATED",
    "createdAt": "2026-06-20T12:00:00Z",
    "fileUrl": "/api/v1/reports/report-custom-001/download"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 6. Download Report

**Endpoint:** `GET /api/v1/reports/{reportId}/download`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `report:view`

**Query Parameters:**
- `format` (string, default: "pdf") - pdf, excel, csv

**Response (200 OK):**
- Content-Type: `application/pdf` or `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="inventory-report-2026-06-20.pdf"`

---

## Data Transfer Objects

### ReportDTO
```typescript
{
  reportId: UUID;
  name: string;
  reportType: string;
  status: "GENERATED" | "PROCESSING" | "ERROR";
  generatedAt: ISO8601DateTime;
  data: Record<string, any>;
}
```

---

## Events Published

- `ReportGenerated` - Report successfully generated
- `ReportScheduled` - Scheduled report queued
- `ReportFailed` - Report generation failed

---

## Implementation Notes

1. Report generation asynchronous for large datasets
2. Reports cached for 24 hours
3. Scheduled reports generated via background jobs
4. PDF generation via reporting engine
5. Excel exports with formatting
6. All report data aggregated from analytics store
7. Real-time metrics available (within 15 min)
8. Custom reports limited to predefined metrics
9. Report access logged for audit trail
10. Email delivery of scheduled reports
