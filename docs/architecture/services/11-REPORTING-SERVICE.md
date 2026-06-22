# 11. Reporting Service

**Bounded Context:** Business Analytics  
**Database:** `reporting_db` (PostgreSQL)  
**Port:** 8011  
**Team:** Analytics & Business Intelligence  

---

## Purpose

The Reporting Service generates business intelligence, dashboards, KPIs, and reports for executives, managers, and operational staff.

---

## Responsibilities

- Inventory reports (aged, stock levels, turnover)
- Warehouse utilization reports
- Sales analytics (revenue, order volume, trends)
- Purchase analytics (supplier performance, costs)
- Customer analytics (segmentation, lifetime value)
- KPI calculations and dashboards
- Metric aggregations (real-time and historical)
- Executive dashboards

---

## Database Ownership

**Schema:** `reporting_db`

**Core Tables:**
```sql
-- Aggregated inventory snapshots (denormalized for fast queries)
inventory_snapshots (
  id UUID PRIMARY KEY,
  snapshot_date DATE,
  product_id UUID,
  warehouse_id UUID,
  on_hand INT,
  reserved INT,
  damaged INT,
  available INT,
  value DECIMAL,
  days_in_stock INT,
  created_at TIMESTAMP
)

-- Sales metrics (aggregated by day, product, customer)
sales_metrics (
  id UUID PRIMARY KEY,
  metric_date DATE,
  customer_id UUID,
  product_id UUID,
  order_count INT,
  total_revenue DECIMAL,
  total_quantity INT,
  average_order_value DECIMAL,
  created_at TIMESTAMP
)

-- Warehouse utilization
warehouse_utilization (
  id UUID PRIMARY KEY,
  metric_date DATE,
  warehouse_id UUID,
  total_capacity INT,
  used_capacity INT,
  utilization_percentage DECIMAL,
  created_at TIMESTAMP
)

-- Supplier performance aggregates
supplier_performance_summary (
  id UUID PRIMARY KEY,
  supplier_id UUID,
  metric_month DATE,
  order_count INT,
  on_time_rate DECIMAL,
  quality_score DECIMAL,
  total_spend DECIMAL,
  created_at TIMESTAMP
)

-- KPI metrics
kpi_metrics (
  id UUID PRIMARY KEY,
  kpi_name VARCHAR,
  kpi_value DECIMAL,
  metric_date DATE,
  target_value DECIMAL,
  variance DECIMAL,
  created_at TIMESTAMP
)

-- Dashboard data (cached for performance)
dashboard_cache (
  id UUID PRIMARY KEY,
  dashboard_name VARCHAR,
  cache_key VARCHAR UNIQUE,
  cache_data JSONB,
  cached_at TIMESTAMP,
  expires_at TIMESTAMP
)
```

---

## Events Consumed

### From Inventory Service
- **StockIn/StockOut/InventoryAdjusted:** Update inventory snapshots
- **LowStockDetected:** Track stock exception metrics

### From Sales Order Service
- **SalesOrderCreated:** Update sales metrics
- **SalesOrderShipped:** Track fulfillment metrics
- **OrderReturned:** Track return rates

### From Purchase Order Service
- **PurchaseOrderReceived:** Update purchase metrics

### From Warehouse Service
- **CapacityUpdated:** Update warehouse utilization
- **ShippingCompleted:** Track shipment metrics

### From Supplier Service
- **PerformanceScoreUpdated:** Update supplier metrics

### From Customer Service
- **SegmentationChanged:** Update customer segmentation metrics

---

## Events Published

### 1. DashboardUpdated
**When:** Dashboard data refreshed  
**Consumers:** Frontend (WebSocket refresh)

### 2. KPIThresholdExceeded
**When:** KPI outside acceptable range  
**Consumers:** Notification Service

---

## REST APIs

**Base URL:** `/api/v1/reporting`

### Dashboards
- `GET /dashboards` - List available dashboards
- `GET /dashboards/executive` - Executive overview
- `GET /dashboards/operations` - Warehouse operations
- `GET /dashboards/sales` - Sales metrics
- `GET /dashboards/inventory` - Inventory metrics

### Inventory Reports
- `GET /reports/inventory/aged` - Aged inventory report
- `GET /reports/inventory/levels` - Current stock levels
- `GET /reports/inventory/turnover` - Stock turnover analysis
- `GET /reports/inventory/valuations` - Inventory valuation

### Sales Reports
- `GET /reports/sales/summary` - Sales summary (date range)
- `GET /reports/sales/by-customer` - Sales by customer
- `GET /reports/sales/by-product` - Sales by product
- `GET /reports/sales/trends` - Sales trends

### Warehouse Reports
- `GET /reports/warehouse/utilization` - Warehouse capacity utilization
- `GET /reports/warehouse/performance` - Efficiency metrics
- `GET /reports/warehouse/zones` - Zone-level utilization

### Supplier Reports
- `GET /reports/suppliers/performance` - On-time delivery, quality
- `GET /reports/suppliers/spending` - Supplier spending analysis
- `GET /reports/suppliers/ranking` - Top/bottom suppliers

### Customer Reports
- `GET /reports/customers/segments` - Customer segmentation
- `GET /reports/customers/lifetime-value` - Customer LTV
- `GET /reports/customers/churn-risk` - Churn prediction

### KPI Metrics
- `GET /metrics/kpi` - Current KPI values
- `GET /metrics/kpi/{kpiName}` - Specific KPI history
- `GET /metrics/kpi/status` - KPI status (green/yellow/red)

### Report Query Parameters (Standard)
- `from_date` - ISO-8601 start date
- `to_date` - ISO-8601 end date
- `warehouse_id` - Filter by warehouse (optional)
- `product_id` - Filter by product (optional)
- `customer_id` - Filter by customer (optional)
- `format` - Output format (json, csv, pdf)
- `page` - Page number (0-indexed)
- `size` - Records per page

---

## Dependencies

**Event Sources:**
- Inventory Service
- Sales Order Service
- Purchase Order Service
- Warehouse Service
- Supplier Service
- Customer Service

**No Synchronous Dependencies:** Read-only consumer

---

## Data Consistency Patterns

### Eventual Consistency

```
1. Event published by source service (e.g., StockIn)
2. Reporting Service receives event asynchronously
3. Reporting Service updates aggregates:
   - Update inventory_snapshots table
   - Recalculate affected KPIs
   - Invalidate dashboard cache
   ↓
4. Dashboard cache rebuilds on next request
5. User sees updated metrics
```

### KPI Calculation Example

```
Daily at midnight (UTC):
1. Calculate inventory on-hand from inventory_snapshots
2. Calculate current period sales from sales_metrics
3. Calculate warehouse utilization from warehouse_utilization
4. Evaluate KPIs against targets:
   - Stock turnover rate
   - Warehouse utilization %
   - Order fulfillment rate
   - Days inventory outstanding
5. Store results in kpi_metrics
6. If any KPI threshold exceeded → Publish KPIThresholdExceeded
```

---

## Future Scalability

### Real-Time Dashboards
- Subscribe to events via WebSocket
- Push updates to client in real-time
- No page refresh needed

### Advanced Analytics
- Predictive analytics (demand forecasting)
- Anomaly detection in sales/inventory
- Root cause analysis

### Data Warehouse Integration
- Export aggregates to data warehouse (Snowflake, BigQuery)
- Enable external BI tools (Tableau, Power BI)

### Machine Learning
- Stock optimization models
- Demand forecasting models
- Supplier risk models

---

## Deployment Checklist

- [ ] `reporting_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured (Kafka/RabbitMQ)
- [ ] Aggregate recalculation jobs scheduled
- [ ] KPI definitions configured
- [ ] Dashboard cache TTL configured
- [ ] Report generation jobs scheduled
- [ ] Monitoring/alerting configured
- [ ] Redis caching configured (dashboard cache)
- [ ] PDF generation library configured (optional)

