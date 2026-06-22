# ADR-0006: Data Export and AI Readiness Strategy - Data Lake Architecture

## Status
Accepted

## Context
SmartStock AI has a unique requirement: it is designed to evolve into an AI-powered decision platform. Phase 1 builds operational capabilities; Phase 4 adds AI/ML features. This creates a critical challenge:

**The operational system must generate high-quality data for future AI training without compromising operational performance.**

Current state problem:
- Operational databases optimized for transactional performance (OLTP)
- AI/ML requires historical time-series data (OLAP workload)
- Direct queries from AI pipelines would degrade operational system performance
- Data must be exported in AI-friendly formats (Parquet, CSV, JSON)
- Historical data must be preserved for training datasets

The Data Export Service is the critical bridge between operational systems (Phase 1) and future analytics/AI systems (Phases 2-4).

## Decision
Implement a **Multi-Layer Data Lake Architecture**:

### 1. **Data Lake Layers**

**Layer 1: Operational Layer (PostgreSQL)**
- Real-time transactional data
- One PostgreSQL per microservice
- Optimized for speed and consistency
- Data ages as it becomes historical

**Layer 2: Event Log Layer (Kafka)**
- Immutable append-only event stream
- All business events captured with context
- 7-year retention for compliance
- Source of truth for data reconstruction

**Layer 3: Raw Data Lake (MinIO/S3)**
- Object storage for structured data exports
- Immutable historical snapshots
- Organized by date: `/events/2026-06-20/inventory-events.parquet`
- Formats: Parquet (efficient), CSV (human-readable), JSON (flexible)

**Layer 4: Aggregated Analytics Store (PostgreSQL)**
- Reporting Service maintains denormalized views
- Optimized for reporting queries
- Eventual consistent with operational data
- Used by dashboards and business intelligence tools

**Layer 5: Feature Store (Future - ML Pipeline)**
- Cleaned, normalized features ready for model training
- Pre-computed features (e.g., daily demand patterns, supplier reliability scores)
- Versioned for reproducibility
- Integrated with ML training pipelines

### 2. **Data Flow Architecture**

```
Operational Services (Inventory, Product, Order, etc.)
          ↓ (emit events)
    Kafka Broker (immutable event log)
          ↓ (consume & transform)
    Data Export Service
          ├─→ MinIO/S3 (raw data lake: Parquet/CSV/JSON)
          └─→ Reporting Service Database (analytical views)
               └─→ Dashboards & Reports (BI tools)
               
    Data Lake Processor (scheduled job)
          └─→ Archive old events
          └─→ Compact small files
          └─→ Update schema metadata

    Analytics Pipeline (Phase 2)
          └─→ Consume from Data Lake
          └─→ Generate business KPIs
          └─→ Create feature store
          
    AI/ML Platform (Phase 4)
          └─→ Consume from Feature Store
          └─→ Train demand forecasting models
          └─→ Train stock optimization models
          └─→ Develop anomaly detection
```

### 3. **Data Export Service Responsibilities**

**Event Streaming to Data Lake**
- Consumes all events from Kafka
- Transforms events to structured Parquet format
- Partitions by date and event type
- Uploads to MinIO/S3 with 1-hour latency

**Snapshot Generation**
- Daily snapshots of inventory state
- Product catalog snapshots
- Warehouse utilization snapshots
- Supplier performance snapshots
- Enables "what was inventory on date X" historical queries

**Schema Management**
- Maintains data dictionary
- Documents all events and their payload fields
- Tracks schema versions for backward compatibility
- Exports schema in standardized format (Apache Arrow)

**Data Quality Monitoring**
- Validates event format and completeness
- Detects missing or null fields
- Alerts on data quality degradation
- Tracks data completeness metrics

### 4. **Event Export Format**
All events exported to Parquet with this schema:
```
event_id: UUID
event_type: STRING (e.g., "StockIn", "ProductCreated")
aggregate_id: UUID (product_id, order_id, etc.)
aggregate_type: STRING (e.g., "Inventory", "Product")
occurred_at: TIMESTAMP (when event happened in business time)
published_at: TIMESTAMP (when event published to Kafka)
user_id: UUID
correlation_id: UUID
source_service: STRING
version: INT (event schema version)
payload: STRUCT (event-specific data)
metadata: STRUCT (system metadata)

-- Partitioning: /year=2026/month=06/day=20/event_type=StockIn/
```

### 5. **Data Retention Policies**
- **Operational Databases**: Optimize after 1 year (archive to data lake)
- **Kafka Event Stream**: 7 years (compliance requirement)
- **Data Lake Raw Events**: Unlimited (immutable, low cost in S3)
- **Analytical Snapshots**: 5 years hot, 7 years cold archive
- **Feature Store**: Retain trained features for reproducibility

### 6. **Privacy and Security**
- PII data encrypted in data lake
- Access controls restrict who can export/access data
- Data governance policies document which data is shareable
- GDPR compliance: ability to delete user data (archive only, never delete original)
- Audit trail logs all data access for compliance reviews

## Alternatives Considered

### Option 1: Direct Operational Database Exports
Export data directly from operational databases via scheduled SQL queries

**Pros:**
- Simple to implement initially
- No additional infrastructure
- Direct access to latest data

**Cons:**
- Queries degrade operational performance
- Loss of historical versions
- Difficult to handle 7-year retention
- No immutable audit trail
- Event context lost (what caused the change)
- Difficult to export in AI-friendly formats
- Cannot replay to recover from data corruption

### Option 2: Custom ETL Service Per Microservice
Each service implements custom export logic

**Pros:**
- Service-specific optimization possible
- Clear ownership

**Cons:**
- Code duplication across services
- Inconsistent formats and quality
- Difficult to ensure schema compatibility
- Monitoring and reliability fragmented
- High maintenance burden

### Option 3: Real-Time Data Warehouse (Snowflake, BigQuery, Redshift)
Use cloud data warehouse for real-time analytics

**Pros:**
- Managed service (no operational burden)
- Excellent query performance
- Built-in ML integrations

**Cons:**
- Vendor lock-in
- High cost for continuous data streaming
- Data governance more complex with external system
- On-premise deployment not supported
- Less suitable for current budget constraints

## Consequences

### Positive
- **AI-Ready Foundation**: All business data available for future ML models
- **Time-Series Analysis**: Immutable event logs enable historical analysis and seasonality detection
- **Disaster Recovery**: Complete event log allows reconstruction of system state
- **Data Governance**: Clear separation between operational and analytical systems
- **Operational Performance**: Analytics queries don't degrade transactional performance
- **Compliance**: 7-year immutable audit trail supports regulatory requirements
- **Reproducibility**: Exact same training data available for model retraining
- **Schema Evolution**: Events versioned; old and new schemas coexist
- **Cost Efficiency**: Object storage (MinIO/S3) cheap compared to databases for long-term retention
- **Historical Analysis**: Snapshots enable "what was the state on date X" queries

### Negative
- **Additional Infrastructure**: Data Export Service and object storage required
- **Eventual Consistency**: Exported data lags operational data by hours
- **Operational Complexity**: Additional components to monitor and maintain
- **Storage Costs**: 7-year retention of events requires significant storage
- **Data Synchronization**: Discrepancies between operational and exported data possible
- **Metadata Management**: Schema versioning and governance adds overhead
- **Privacy Complexity**: Exporting PII to data lake requires encryption and access controls
- **Network Overhead**: Continuous event streaming consumes bandwidth

### Trade-offs
- **Performance vs. Data**: Accept operational overhead for comprehensive data capture
- **Consistency vs. Simplicity**: Accept eventual consistency for operational independence
- **Infrastructure Cost vs. Future AI**: Invest in data infrastructure now for AI capabilities later
- **Storage Cost vs. Compliance**: Pay for 7-year retention to meet regulatory requirements

## Future Considerations

1. **Stream Processing**: Real-time aggregation using Kafka Streams or Spark
   - Compute daily KPIs as events arrive (vs. scheduled batch jobs)
   - Enable real-time dashboards and alerts
   - Reduce latency for critical decisions

2. **Feature Store**: Dedicated feature engineering layer for ML
   - Pre-computed features (e.g., "average_daily_demand_product_123_week_1_2026")
   - Feature versioning for reproducible model training
   - Integration with ML frameworks (MLflow, Kubeflow)

3. **Data Cataloging**: Implement data catalog for discoverability
   - Document all tables, columns, and data types
   - Track data lineage (which services produce which data)
   - Support self-service analytics by business users

4. **Incremental Snapshots**: Optimize storage with delta updates
   - Instead of full snapshot daily, store only changes
   - Dramatically reduce storage requirements
   - Still maintain complete history

5. **Time-Travel Queries**: Query data as of past dates
   - "What was inventory on date X?"
   - "How have supplier metrics changed over time?"
   - Essential for trend analysis and forecasting

6. **Data Lakehouse Architecture**: Merge data lake and data warehouse
   - Combine data lake flexibility with warehouse performance
   - Use DuckDB, Delta Lake, or Iceberg for structured lake tables

7. **Monitoring and Alerting**: Track data quality metrics
   - Missing values, outliers, schema changes
   - Alert when data export lags exceeds threshold
   - Detect data quality issues early

8. **Privacy-Preserving Exports**: Differential privacy or anonymization
   - Aggregate data to prevent individual identification
   - Support sharing aggregated insights without exposing details

## Implementation Guidance

- Data Export Service written in Java/Spring Boot for consistency with other services
- Parquet format used for efficient columnar storage and ML compatibility
- MinIO deployed alongside application stack for object storage
- Data lake organized using Apache Hive partitioning (year/month/day/event_type)
- Daily reconciliation jobs verify event counts between Kafka and data lake
- Data governance policy documents required fields and retention periods
- All data access (export, deletion) requires audit logging
- PII encryption keys managed separately from data lake
- Backup strategy for data lake (offsite copies)
- Data lake metadata stored in schema registry with versioning
