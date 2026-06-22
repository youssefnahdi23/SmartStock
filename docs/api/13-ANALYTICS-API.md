# Analytics Service API

**Base URL:** `/api/v1/analytics`  
**Service Port:** 8013  
**Authentication:** JWT Bearer Token (required for most endpoints)  
**Status:** Analytics Service

---

## Overview

The Analytics Service provides statistical analysis, KPI calculations, trend analysis, and forecasting data for business intelligence. This is **NOT** a machine learning service—it provides pre-computed analytics and statistical foundations for future AI systems.

---

## Endpoints

### 1. Get Inventory KPIs

**Endpoint:** `GET /api/v1/analytics/kpis/inventory`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `warehouseId` (UUID, optional) - Specific warehouse
- `period` (string, default: "MONTHLY") - DAILY, WEEKLY, MONTHLY, QUARTERLY
- `months` (integer, default: 12) - Historical months to include

**Response (200 OK):**
```json
{
  "data": {
    "period": "MONTHLY",
    "kpis": {
      "inventoryTurnoverRatio": 12.5,
      "inventoryDaysOut": 29.2,
      "inventoryCarryingCost": 450000.00,
      "stockAccuracyPercentage": 98.7,
      "shrinkagePercentage": 0.3,
      "deadStockPercentage": 2.1
    },
    "historicalTrend": [
      {
        "month": "2026-05",
        "turnoverRatio": 12.3,
        "daysOut": 29.7,
        "carryingCost": 460000.00,
        "stockAccuracy": 98.5
      }
    ],
    "comparisons": {
      "monthOverMonth": {
        "turnoverRatio": 1.6,
        "daysOut": -1.7,
        "carryingCost": -2.2
      },
      "yearOverYear": {
        "turnoverRatio": 8.2,
        "daysOut": -12.5,
        "carryingCost": 5.3
      }
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get Demand Patterns

**Endpoint:** `GET /api/v1/analytics/demand-patterns`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `productId` (UUID, optional) - Specific product
- `productCategoryId` (UUID, optional) - Product category
- `months` (integer, default: 24) - Historical months to analyze

**Response (200 OK):**
```json
{
  "data": {
    "analysisWindow": {
      "fromDate": "2024-06-20",
      "toDate": "2026-06-20",
      "months": 24
    },
    "demandPatterns": [
      {
        "productId": "prod-001",
        "productName": "Premium Widget",
        "averageMonthlyDemand": 5000,
        "standardDeviation": 450,
        "minDemand": 3200,
        "maxDemand": 7800,
        "seasonalityIndex": 1.15,
        "trend": "INCREASING",
        "trendStrength": 8.2,
        "forecastNextMonth": 5200,
        "forecastConfidenceInterval": [4900, 5500]
      }
    ],
    "aggregatedMetrics": {
      "totalDemandVariability": 12.3,
      "seasonalPeak": "DECEMBER",
      "seasonalTrough": "AUGUST"
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. Get Warehouse Efficiency Metrics

**Endpoint:** `GET /api/v1/analytics/warehouse-efficiency`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `warehouseId` (UUID, optional) - Specific warehouse
- `period` (string, default: "MONTHLY") - DAILY, WEEKLY, MONTHLY

**Response (200 OK):**
```json
{
  "data": {
    "warehouses": [
      {
        "warehouseId": "W01",
        "warehouseName": "Main Warehouse",
        "metrics": {
          "spaceUtilizationRate": 72.5,
          "laborProductivity": 125.3,
          "pickAccuracyPercentage": 99.8,
          "cycleTimeMinutes": 8.5,
          "ordersProcessedPerDay": 450,
          "costPerOrder": 12.50,
          "dockDwellTimeHours": 2.3
        },
        "zones": [
          {
            "zoneName": "Cold Storage",
            "utilization": 65.0,
            "throughput": 200
          }
        ],
        "trends": {
          "utilizationTrend": "INCREASING",
          "productivityTrend": "STABLE",
          "accuracyTrend": "STABLE"
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

### 4. Get Supplier Trends

**Endpoint:** `GET /api/v1/analytics/supplier-trends`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `supplierId` (UUID, optional) - Specific supplier
- `months` (integer, default: 12) - Historical months

**Response (200 OK):**
```json
{
  "data": {
    "suppliers": [
      {
        "supplierId": "S001",
        "supplierName": "Premium Widgets Ltd",
        "metrics": {
          "onTimeDeliveryRate": 98.5,
          "qualityScore": 96.0,
          "leadTimeAverage": 14.5,
          "leadTimeStdDev": 2.1,
          "priceStability": 99.2,
          "responseTime": 2.5,
          "defectRate": 0.8
        },
        "trends": {
          "deliveryRateTrend": "IMPROVING",
          "qualityTrend": "STABLE",
          "priceChangeTrend": "DECREASING"
        },
        "risk": {
          "riskScore": 2.5,
          "riskFactors": [
            "Single source for critical component"
          ],
          "riskLevel": "LOW"
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

### 5. Get Customer Segmentation Analysis

**Endpoint:** `GET /api/v1/analytics/customer-segmentation`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `segmentationMethod` (string, default: "REVENUE") - REVENUE, FREQUENCY, RECENCY, ABC

**Response (200 OK):**
```json
{
  "data": {
    "segmentationMethod": "ABC",
    "segments": [
      {
        "segment": "A",
        "segmentName": "High-Value Customers",
        "customerCount": 25,
        "totalRevenue": 2500000.00,
        "percentageOfRevenue": 70.0,
        "averageOrderValue": 10000.00,
        "orderFrequency": 50,
        "satisfactionScore": 4.8,
        "churnRisk": "LOW"
      },
      {
        "segment": "B",
        "segmentName": "Medium-Value Customers",
        "customerCount": 75,
        "totalRevenue": 900000.00,
        "percentageOfRevenue": 25.0,
        "averageOrderValue": 3000.00,
        "orderFrequency": 15,
        "satisfactionScore": 4.4,
        "churnRisk": "MEDIUM"
      },
      {
        "segment": "C",
        "segmentName": "Low-Value Customers",
        "customerCount": 200,
        "totalRevenue": 200000.00,
        "percentageOfRevenue": 5.0,
        "averageOrderValue": 500.00,
        "orderFrequency": 3,
        "satisfactionScore": 3.8,
        "churnRisk": "HIGH"
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

### 6. Get Forecasting Data

**Endpoint:** `GET /api/v1/analytics/forecasting-data`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `forecastType` (string) - DEMAND, STOCK, REVENUE, CHURN
- `forecastHorizon` (integer, default: 12) - Forecast months ahead
- `productId` (UUID, optional) - Specific product

**Response (200 OK):**
```json
{
  "data": {
    "forecastType": "DEMAND",
    "forecastHorizon": 12,
    "baseDate": "2026-06-20",
    "forecasts": [
      {
        "month": "2026-07",
        "forecastValue": 5300,
        "lowerBound": 4900,
        "upperBound": 5700,
        "confidence": 0.95,
        "method": "EXPONENTIAL_SMOOTHING"
      }
    ],
    "forecastingMetrics": {
      "mape": 8.5,
      "modelAccuracy": 91.5,
      "lastUpdated": "2026-06-20T12:00:00Z"
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Get Anomaly Alerts

**Endpoint:** `GET /api/v1/analytics/anomalies`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `analytics:view`

**Query Parameters:**
- `days` (integer, default: 7) - Days to check
- `severity` (string, optional) - LOW, MEDIUM, HIGH, CRITICAL

**Response (200 OK):**
```json
{
  "data": {
    "anomalies": [
      {
        "anomalyId": "anom-001",
        "type": "UNUSUAL_STOCK_MOVEMENT",
        "entityType": "PRODUCT",
        "entityId": "prod-001",
        "entityName": "Premium Widget",
        "severity": "HIGH",
        "description": "Stock quantity decreased by 500 units without recorded movement",
        "detectedAt": "2026-06-20T10:00:00Z",
        "expectedValue": 1000,
        "observedValue": 500,
        "deviation": 50,
        "recommendation": "Review audit logs for unrecorded transactions"
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

### KPIDataDTO
```typescript
{
  kpiName: string;
  currentValue: number;
  previousValue: number;
  trend: "INCREASING" | "DECREASING" | "STABLE";
  trendPercentage: number;
  unit: string;
}
```

### ForecastDTO
```typescript
{
  forecastPeriod: string;
  predictedValue: number;
  confidenceInterval: [number, number];
  confidence: number; // 0.0 - 1.0
  method: string;
}
```

---

## Events Published

- `AnalyticsProcessed` - Analytics calculation completed
- `AnomalyDetected` - Anomaly detected in data
- `ForecastUpdated` - Forecasting model updated

---

## Implementation Notes

1. All analytics computed from Analytics Store (aggregated tables)
2. Real-time updates within 15-30 minutes
3. Historical data retention: minimum 3 years
4. Anomaly detection via statistical methods (z-score, IQR)
5. Forecasting uses exponential smoothing and ARIMA
6. KPI calculations standardized per business domain
7. No model training performed—purely statistical
8. Confidence intervals computed via bootstrap methods
9. All calculations logged for auditability
10. Performance optimized for real-time dashboards
11. Cache enabled for frequently accessed queries
12. Scheduled recalculation of aggregates daily
