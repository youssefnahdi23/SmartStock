# 13. Analytics Service

**Bounded Context:** Analytics Engine  
**Database:** `analytics_db` (PostgreSQL)  
**Port:** 8013  
**Team:** Data Science & Analytics  

---

## Purpose

The Analytics Service provides advanced data analytics, metrics calculations, trend analysis, and insights beyond operational reporting. It prepares data for future AI/ML systems.

---

## Responsibilities

- Advanced metrics calculations
- Trend analysis and forecasting
- Anomaly detection
- Cohort analysis
- Data aggregation for AI training
- Feature engineering
- Statistical analysis
- Predictive model scoring

---

## Database Ownership

**Schema:** `analytics_db`

**Core Tables:**
```sql
-- Time-series metrics (high-cardinality data)
time_series_metrics (
  id UUID PRIMARY KEY,
  metric_name VARCHAR,
  metric_date DATE,
  metric_hour HOUR,
  dimension1 VARCHAR,
  dimension2 VARCHAR,
  value DECIMAL,
  created_at TIMESTAMP,
  
  INDEX idx_metric_date (metric_name, metric_date),
  INDEX idx_dimensions (dimension1, dimension2)
)

-- Trend analysis results
trend_analysis (
  id UUID PRIMARY KEY,
  entity_type VARCHAR,
  entity_id VARCHAR,
  metric_name VARCHAR,
  trend_direction ENUM ('up', 'down', 'stable'),
  trend_strength DECIMAL,
  period_start DATE,
  period_end DATE,
  calculated_at TIMESTAMP
)

-- Anomaly detection results
anomalies (
  id UUID PRIMARY KEY,
  entity_type VARCHAR,
  entity_id VARCHAR,
  metric_name VARCHAR,
  expected_value DECIMAL,
  actual_value DECIMAL,
  deviation_percent DECIMAL,
  severity ENUM ('low', 'medium', 'high', 'critical'),
  detected_at TIMESTAMP,
  acknowledged_at TIMESTAMP,
  acknowledged_by UUID
)

-- Cohort analysis (e.g., customer cohorts)
cohorts (
  id UUID PRIMARY KEY,
  cohort_name VARCHAR,
  cohort_type VARCHAR,
  cohort_date DATE,
  entity_ids JSONB,
  cohort_size INT,
  created_at TIMESTAMP
)

cohort_performance (
  id UUID PRIMARY KEY,
  cohort_id UUID,
  metric_name VARCHAR,
  period_age INT,
  metric_value DECIMAL,
  calculated_at TIMESTAMP
)

-- Feature store (for AI/ML)
feature_store (
  id UUID PRIMARY KEY,
  feature_name VARCHAR,
  feature_version INT,
  entity_type VARCHAR,
  entity_id VARCHAR,
  feature_value DECIMAL,
  calculated_at TIMESTAMP,
  expires_at TIMESTAMP,
  
  INDEX idx_feature_lookup (entity_type, entity_id, feature_name)
)

-- Model performance tracking
model_performance (
  id UUID PRIMARY KEY,
  model_name VARCHAR,
  model_version VARCHAR,
  prediction_date DATE,
  accuracy_score DECIMAL,
  precision_score DECIMAL,
  recall_score DECIMAL,
  f1_score DECIMAL,
  evaluation_details JSONB
)
```

---

## Events Consumed

### From Data Export Service
- **ExportJobCompleted:** Trigger analytics recalculation

### From Reporting Service
- **DashboardUpdated:** Update related analytics metrics

### From All Services (via Audit)
- **All Domain Events:** Source for analysis

---

## Events Published

### 1. AnomalyDetected
**When:** Unusual pattern found  
**Consumers:** Notification Service, Reporting Service

### 2. TrendAnalysisCompleted
**When:** Trend calculation finished  
**Consumers:** Reporting Service

### 3. FeatureStoreUpdated
**When:** Features calculated for ML  
**Consumers:** (Future) ML training pipeline

---

## REST APIs

**Base URL:** `/api/v1/analytics`

### Metrics & Analytics
- `GET /metrics/{metricName}` - Get metric history
- `GET /metrics/{metricName}/forecast` - Forecast next period
- `GET /metrics/{metricName}/comparison` - Compare periods

### Trend Analysis
- `GET /trends` - List available trend analyses
- `GET /trends/{entityType}/{entityId}` - Trends for entity
- `GET /trends/top-gainers` - Trending up
- `GET /trends/top-losers` - Trending down

### Anomaly Detection
- `GET /anomalies` - List detected anomalies
- `GET /anomalies/active` - Current anomalies
- `GET /anomalies/{anomalyId}` - Anomaly details
- `PUT /anomalies/{anomalyId}/acknowledge` - Mark as reviewed

### Cohort Analysis
- `GET /cohorts` - List cohorts
- `POST /cohorts` - Create cohort
- `GET /cohorts/{cohortId}/performance` - Cohort metrics over time
- `GET /cohorts/{cohortId}/retention` - Retention curve

### Feature Store (AI/ML)
- `GET /features` - List available features
- `GET /features/{entityType}/{entityId}` - Entity features
- `POST /features/refresh` - Trigger feature recalculation
- `GET /features/schema` - Feature definitions

### Model Monitoring
- `GET /models` - Deployed models
- `GET /models/{modelName}/performance` - Model accuracy metrics
- `GET /models/{modelName}/drift` - Data drift detection

---

## Dependencies

**Event Sources:**
- Data Export Service
- Reporting Service
- All services (via audit events)

**External Dependencies:**
- NumPy/Pandas (data processing)
- Scikit-Learn (ML metrics, anomaly detection)
- Prophet/ARIMA (time-series forecasting)
- Feature stores (Tecton, Feast - future)

---

## Implementation Pattern

### Daily Analytics Pipeline

```
1. Scheduled: 04:00 UTC daily
   ↓
2. Load data from data lake (events, snapshots)
   ↓
3. Calculate time-series metrics:
   - Daily stock movement volumes
   - Order throughput
   - Warehouse utilization
   ↓
4. Run anomaly detection:
   - Compare to 30-day rolling average
   - Flag outliers (>2σ deviation)
   - Publish AnomalyDetected if needed
   ↓
5. Calculate trends:
   - 7-day trend
   - 30-day trend
   - YoY comparison (when available)
   ↓
6. Update feature store:
   - Inventory turnover rate
   - Demand volatility
   - Supplier reliability score
   ↓
7. Store results in analytics_db
```

### Forecasting Example

```
Request: GET /metrics/daily-orders/forecast?days=30

Process:
1. Load last 365 days of order data
2. Fit Prophet model with:
   - Trend component
   - Weekly seasonality
   - Yearly seasonality (when available)
3. Generate 30-day forecast
4. Include confidence intervals (80%, 95%)
5. Return forecast with visualization data
```

### Anomaly Detection Example

```
Scheduled: Every 6 hours

For each warehouse:
1. Get last 30 days of stock movements
2. Calculate rolling average
3. For latest day:
   - If actual > avg + 2σ → 'High movement anomaly'
   - If actual < avg - 2σ → 'Low movement anomaly'
4. Severity calculation:
   - Deviation % × historical frequency
5. Store in anomalies table
6. If severity >= 'high' → Publish AnomalyDetected
7. Notification Service alerts operations
```

---

## Future Scalability

### AI/ML Integration
- Export features to ML feature store
- Score predictions from trained models
- Update predictions daily (demand, churn, etc.)

### Real-Time Scoring
- Stream events → real-time feature calculation
- Score against deployed models in real-time
- Return predictions to operational systems

### Advanced Analytics
- Causal inference (what-if analysis)
- Optimization (optimal stock levels)
- Recommendation engines

### Advanced Anomaly Detection
- Isolation Forest for multivariate anomalies
- LSTM autoencoders for temporal patterns
- Correlation-based anomaly detection

---

## Deployment Checklist

- [ ] `analytics_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured
- [ ] Data science libraries installed (NumPy, Pandas, Scikit-Learn)
- [ ] Daily analytics pipeline scheduled
- [ ] Anomaly detection thresholds configured
- [ ] Forecast model trained and deployed
- [ ] Feature store schema defined
- [ ] Monitoring/alerting configured
- [ ] Model performance tracking enabled

---

## Integration with Future AI Systems

### Phase 2 - Analytics Platform
1. Analytics Service produces clean, aggregated data
2. Data Export Service exports to data lake
3. Analytics engineers build dashboards
4. Business gains insights

### Phase 3 - Data Platform
1. Historical events available for replay
2. Training datasets exported to Parquet
3. Data quality validated
4. Ready for ML training

### Phase 4 - AI Platform
1. ML models trained on clean historical data
2. Features from feature store
3. Models deployed alongside Analytics Service
4. System makes predictions and recommendations

