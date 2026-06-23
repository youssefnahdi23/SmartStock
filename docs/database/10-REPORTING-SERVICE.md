# Database Specification: Reporting Service

**Service**: Reporting Service  
**Purpose**: Store pre-aggregated reports and metrics for analytics  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Reporting Service maintains read-optimized aggregated data for reporting and analytics dashboards.

### High-Level Architecture
```
report_definitions
├── report_executions (1:M)
├── report_cache (1:M)
└── aggregated_metrics (1:M)

aggregated_metrics
├── daily_metrics (1:M)
├── monthly_metrics (1:M)
└── yearly_metrics (1:M)
```

---

## 2. Tables Specification

### 2.1 `report_definitions` Table
**Purpose**: Store report configurations and definitions

```sql
CREATE TABLE report_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_code VARCHAR(100) UNIQUE NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    description TEXT,
    report_category VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    data_source_service VARCHAR(100),
    query_sql TEXT,
    required_parameters JSONB,
    output_formats JSONB,
    refresh_frequency VARCHAR(50) DEFAULT 'DAILY',
    refresh_time TIME,
    is_scheduled BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    access_level VARCHAR(50) DEFAULT 'INTERNAL',
    owner_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT report_name_not_empty CHECK (report_name != ''),
    CONSTRAINT valid_report_type CHECK (report_type IN ('STANDARD', 'ADHOC', 'EXECUTIVE', 'OPERATIONAL', 'COMPLIANCE')),
    CONSTRAINT valid_refresh_frequency CHECK (refresh_frequency IN ('REALTIME', 'HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'ON_DEMAND'))
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: report_code, report_category, report_type, is_active
**Analytics**: Report usage patterns

---

### 2.2 `report_executions` Table
**Purpose**: Track report generation executions

```sql
CREATE TABLE report_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_definition_id UUID NOT NULL REFERENCES report_definitions(id),
    execution_status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    execution_type VARCHAR(50),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    execution_time_ms INT,
    rows_processed INT,
    rows_output INT,
    output_format VARCHAR(50),
    output_file_path VARCHAR(1000),
    file_size_bytes INT,
    error_message TEXT,
    executed_by UUID,
    execution_parameters JSONB,
    correlation_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_execution_status CHECK (execution_status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);
```

**Audit Fields**: started_at, completed_at, executed_by, created_at
**Indexes**: report_definition_id, execution_status, started_at DESC
**Partitioning**: By month on started_at
**Analytics**: Report performance metrics

---

### 2.3 `daily_metrics` Table
**Purpose**: Store daily aggregated metrics

```sql
CREATE TABLE daily_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    metric_category VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(15, 2),
    metric_count INT,
    metric_average DECIMAL(15, 4),
    metric_min DECIMAL(15, 2),
    metric_max DECIMAL(15, 2),
    dimension_1 VARCHAR(100),
    dimension_2 VARCHAR(100),
    dimension_3 VARCHAR(100),
    source_service VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_daily_metric UNIQUE (metric_date, metric_category, metric_name, dimension_1, dimension_2, dimension_3)
);
```

**Indexes**: metric_date DESC, metric_category, metric_name
**Partitioning**: By month on metric_date
**Retention**: Keep 3 years
**Analytics**: Time-series analytics

---

### 2.4 `monthly_metrics` Table
**Purpose**: Store monthly aggregated metrics

```sql
CREATE TABLE monthly_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_year INT NOT NULL,
    metric_month INT NOT NULL,
    metric_category VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(15, 2),
    metric_count INT,
    metric_average DECIMAL(15, 4),
    metric_min DECIMAL(15, 2),
    metric_max DECIMAL(15, 2),
    dimension_1 VARCHAR(100),
    dimension_2 VARCHAR(100),
    dimension_3 VARCHAR(100),
    source_service VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_monthly_metric UNIQUE (metric_year, metric_month, metric_category, metric_name, dimension_1, dimension_2, dimension_3)
);
```

**Indexes**: metric_year DESC, metric_month, metric_category
**Retention**: Keep 10 years
**Analytics**: Trend analysis

---

### 2.5 `yearly_metrics` Table
**Purpose**: Store yearly aggregated metrics

```sql
CREATE TABLE yearly_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_year INT NOT NULL,
    metric_category VARCHAR(100) NOT NULL,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(15, 2),
    metric_count INT,
    metric_average DECIMAL(15, 4),
    metric_min DECIMAL(15, 2),
    metric_max DECIMAL(15, 2),
    dimension_1 VARCHAR(100),
    dimension_2 VARCHAR(100),
    dimension_3 VARCHAR(100),
    source_service VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_yearly_metric UNIQUE (metric_year, metric_category, metric_name, dimension_1, dimension_2, dimension_3)
);
```

**Indexes**: metric_year DESC, metric_category
**Retention**: Indefinite
**Analytics**: Historical trending

---

### 2.6 `report_cache` Table
**Purpose**: Cache report execution results

```sql
CREATE TABLE report_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_definition_id UUID NOT NULL REFERENCES report_definitions(id),
    cache_key VARCHAR(500) NOT NULL,
    cache_data JSONB NOT NULL,
    cache_created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    cache_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    execution_time_ms INT,
    rows_count INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_report_cache UNIQUE (report_definition_id, cache_key)
);
```

**Indexes**: report_definition_id, cache_expires_at
**Retention**: Until cache_expires_at
**Analytics**: Cache hit rate tracking

---

### 2.7 `dashboard_definitions` Table
**Purpose**: Store dashboard configurations

```sql
CREATE TABLE dashboard_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dashboard_code VARCHAR(100) UNIQUE NOT NULL,
    dashboard_name VARCHAR(255) NOT NULL,
    description TEXT,
    dashboard_type VARCHAR(50) NOT NULL,
    target_audience VARCHAR(100),
    refresh_frequency VARCHAR(50) DEFAULT 'REALTIME',
    layout_config JSONB,
    widgets JSONB,
    is_active BOOLEAN DEFAULT true,
    owner_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT dashboard_name_not_empty CHECK (dashboard_name != ''),
    CONSTRAINT valid_dashboard_type CHECK (dashboard_type IN ('EXECUTIVE', 'OPERATIONAL', 'FINANCIAL', 'INVENTORY', 'SALES', 'CUSTOM'))
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: dashboard_code, dashboard_type, is_active

---

### 2.8 `audit_logs` Table
**Purpose**: Immutable audit trail of reporting operations

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('REPORT_RUN', 'REPORT_DOWNLOAD', 'REPORT_SCHEDULE', 'DASHBOARD_VIEW', 'METRIC_GENERATED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
report_definitions (1) ----→ (M) report_executions
                        └──→ (M) report_cache

dashboard_definitions (standalone configuration)
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_report_definitions_code ON report_definitions(report_code);
CREATE INDEX idx_report_definitions_category ON report_definitions(report_category);
CREATE INDEX idx_report_executions_status ON report_executions(execution_status);
CREATE INDEX idx_report_executions_started ON report_executions(started_at DESC);
CREATE INDEX idx_daily_metrics_date ON daily_metrics(metric_date DESC);
CREATE INDEX idx_monthly_metrics_year ON monthly_metrics(metric_year DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_daily_metrics_category_date ON daily_metrics(metric_category, metric_date DESC);
CREATE INDEX idx_report_cache_expires ON report_cache(cache_expires_at) WHERE cache_expires_at > CURRENT_TIMESTAMP;
```

---

## 5. Constraints & Business Rules

### Cache Expiration
```sql
-- Trigger: Delete expired cache entries hourly
-- Trigger: Invalidate related cache on data updates
```

### Metric Aggregation
```sql
-- Daily metrics must sum/average from operational data
-- Monthly metrics roll up from daily metrics
-- Yearly metrics roll up from monthly metrics
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V10.0__Initialize_reporting_schema.sql
V10.1__Add_metrics_tables.sql
V10.2__Add_dashboard_definitions.sql
V10.3__Add_cache_and_audit.sql
V10.4__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Report execution history
- Daily/monthly/yearly metrics
- Dashboard view events
- Cache hit/miss analytics
- Report performance metrics

### ML Feature Inputs
- Trend analysis from time-series metrics
- Anomaly detection from metric deviations
- Forecasting inputs from historical trends
- Optimal refresh frequency prediction

### Business Intelligence
- Operational dashboards (real-time)
- Executive dashboards (daily/weekly)
- Financial reports (monthly)
- Inventory reports (daily)
- Sales analytics (real-time)

---

## 8. Scalability Considerations

### Partitioning Strategy

**report_executions (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', started_at))
```

**daily_metrics (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', metric_date))
```

### Query Optimization
- Materialized views for common queries
- Pre-computed aggregations
- Read-only replicas for analytics
- Archive old metrics to cold storage

---

## 9. Performance Considerations

### Cache Strategy
- Cache frequently accessed reports
- TTL: 1 hour for daily reports, 24 hours for monthly
- Invalidate on source data change
- Use query parameter hashing for cache keys

### Aggregation Pipeline
```
Operational Data (Inventory, Order services)
  ↓
Daily Aggregations (ETL job - nightly)
  ↓
Monthly Aggregations (ETL job - monthly)
  ↓
Yearly Aggregations (ETL job - yearly)
  ↓
Dashboard/Report Cache
```

---

## 10. Monitoring & Observability

### Key Metrics
- Report generation time
- Cache hit rate
- Report execution success rate
- Query performance
- Aggregation job duration

### Alerts
- Report execution timeout
- Cache miss rate high
- Aggregation job failure
- Query latency spike

---

## Summary

**Total Tables**: 8  
**Total Indexes**: 12+  
**Audit Coverage**: 100%  
**Aggregation Levels**: Daily, Monthly, Yearly  
**Cache Strategy**: TTL-based with invalidation  
**Analytics-Ready**: Multi-dimensional metrics with drilling down  

