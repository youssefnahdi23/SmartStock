# Database Specification: Data Export Service

**Service**: Data Export Service  
**Purpose**: Manage data exports for analytics, BI tools, and AI systems  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Data Export Service manages data export jobs, formats, schedules, and delivery to external systems.

### High-Level Architecture
```
export_definitions
├── export_schedules (1:M)
├── export_jobs (1:M)
├── export_deliveries (1:M)
└── export_data_lineage (1:M)
```

---

## 2. Tables Specification

### 2.1 `export_definitions` Table
**Purpose**: Store export job definitions

```sql
CREATE TABLE export_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_code VARCHAR(100) UNIQUE NOT NULL,
    export_name VARCHAR(255) NOT NULL,
    description TEXT,
    export_type VARCHAR(50) NOT NULL,
    source_service VARCHAR(100) NOT NULL,
    data_source_type VARCHAR(100),
    source_query TEXT,
    export_format VARCHAR(50) NOT NULL,
    compression_enabled BOOLEAN DEFAULT false,
    encryption_enabled BOOLEAN DEFAULT true,
    partition_strategy VARCHAR(50),
    row_limit INT,
    incremental_export BOOLEAN DEFAULT false,
    incremental_column VARCHAR(255),
    destination_type VARCHAR(50) NOT NULL,
    destination_config JSONB,
    schedule_enabled BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    access_level VARCHAR(50) DEFAULT 'INTERNAL',
    owner_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT export_name_not_empty CHECK (export_name != ''),
    CONSTRAINT valid_export_type CHECK (export_type IN ('SNAPSHOT', 'INCREMENTAL', 'DELTA', 'STREAM')),
    CONSTRAINT valid_export_format CHECK (export_format IN ('CSV', 'JSON', 'PARQUET', 'ARROW', 'ORC', 'XML')),
    CONSTRAINT valid_destination_type CHECK (destination_type IN ('S3', 'MINIO', 'SFTP', 'HTTP', 'BIGQUERY', 'DATABRICKS', 'SNOWFLAKE', 'POSTGRES'))
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: export_code, export_type, source_service, destination_type, is_active
**Analytics**: Export usage patterns

---

### 2.2 `export_schedules` Table
**Purpose**: Store export job schedules

```sql
CREATE TABLE export_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_definition_id UUID NOT NULL UNIQUE REFERENCES export_definitions(id) ON DELETE CASCADE,
    schedule_type VARCHAR(50) NOT NULL DEFAULT 'DAILY',
    cron_expression VARCHAR(255),
    timezone VARCHAR(100) DEFAULT 'UTC',
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    end_date DATE,
    next_execution_at TIMESTAMP WITH TIME ZONE,
    last_execution_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_schedule_type CHECK (schedule_type IN ('HOURLY', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM', 'ON_DEMAND'))
);
```

**Audit Fields**: created_at, updated_at, last_execution_at
**Indexes**: export_definition_id, schedule_type, next_execution_at
**Analytics**: Schedule execution history

---

### 2.3 `export_jobs` Table
**Purpose**: Track export job executions

```sql
CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_definition_id UUID NOT NULL REFERENCES export_definitions(id),
    job_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    execution_time_ms INT,
    rows_exported INT DEFAULT 0,
    rows_filtered INT DEFAULT 0,
    data_size_bytes INT,
    file_path VARCHAR(1000),
    file_hash VARCHAR(255),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    job_trigger_type VARCHAR(50),
    triggered_by UUID,
    export_parameters JSONB,
    correlation_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_job_status CHECK (job_status IN ('PENDING', 'RUNNING', 'EXTRACTING', 'TRANSFORMING', 'COMPRESSING', 'ENCRYPTING', 'UPLOADING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);
```

**Audit Fields**: started_at, completed_at, triggered_by, created_at
**Indexes**: export_definition_id, job_status, started_at DESC
**Partitioning**: By month on started_at
**Analytics**: Job performance metrics

---

### 2.4 `export_deliveries` Table
**Purpose**: Track data deliveries to destinations

```sql
CREATE TABLE export_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_job_id UUID NOT NULL REFERENCES export_jobs(id),
    destination_type VARCHAR(50) NOT NULL,
    destination_endpoint VARCHAR(500) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    delivery_started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delivery_completed_at TIMESTAMP WITH TIME ZONE,
    delivery_time_ms INT,
    bytes_transferred INT,
    remote_file_path VARCHAR(1000),
    remote_file_id VARCHAR(255),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    validation_status VARCHAR(50),
    validation_errors TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_delivery_status CHECK (delivery_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'VALIDATED', 'CORRUPTED'))
);
```

**Audit Fields**: delivery_started_at, delivery_completed_at, created_at
**Indexes**: export_job_id, delivery_status, delivery_completed_at
**Analytics**: Delivery success rate

---

### 2.5 `export_data_lineage` Table
**Purpose**: Track data lineage and version history

```sql
CREATE TABLE export_data_lineage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_job_id UUID NOT NULL REFERENCES export_jobs(id),
    source_system VARCHAR(100) NOT NULL,
    source_entity VARCHAR(100),
    source_query_fingerprint VARCHAR(255),
    data_version VARCHAR(50),
    extraction_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    record_count INT,
    hash_value VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Indexes**: export_job_id, source_system, extraction_timestamp
**Analytics**: Data provenance tracking

---

### 2.6 `export_monitoring` Table
**Purpose**: Track export performance and metrics

```sql
CREATE TABLE export_monitoring (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_definition_id UUID NOT NULL REFERENCES export_definitions(id),
    monitoring_date DATE NOT NULL,
    total_jobs_run INT,
    successful_jobs INT,
    failed_jobs INT,
    total_rows_exported INT,
    average_job_duration_ms INT,
    total_data_size_mb DECIMAL(12, 2),
    average_file_size_mb DECIMAL(12, 2),
    delivery_success_rate DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_export_monitoring UNIQUE (export_definition_id, monitoring_date)
);
```

**Indexes**: export_definition_id, monitoring_date DESC
**Analytics**: Export health metrics

---

### 2.7 `export_audit_logs` Table
**Purpose**: Immutable audit trail of export operations

```sql
CREATE TABLE export_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    data_sensitivity_level VARCHAR(50),
    export_recipient VARCHAR(255),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('EXPORT_CREATED', 'EXPORT_EXECUTED', 'EXPORT_DELIVERED', 'EXPORT_DELETED', 'EXPORT_DOWNLOADED', 'EXPORT_VERIFIED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE
**Indexes**: entity_type, entity_id, timestamp, data_sensitivity_level
**Retention**: Indefinite
**Analytics**: Compliance and data governance

---

## 3. Relationships & Foreign Keys

```
export_definitions (1) ----→ (1) export_schedules
                        ├──→ (M) export_jobs
                        ├──→ (M) export_monitoring
                        └──→ (M) export_audit_logs

export_jobs (1) ----→ (M) export_deliveries
          └──→ (M) export_data_lineage
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_export_definitions_code ON export_definitions(export_code);
CREATE INDEX idx_export_definitions_type ON export_definitions(export_type);
CREATE INDEX idx_export_definitions_source ON export_definitions(source_service);
CREATE INDEX idx_export_jobs_definition ON export_jobs(export_definition_id);
CREATE INDEX idx_export_jobs_status ON export_jobs(job_status);
CREATE INDEX idx_export_jobs_started ON export_jobs(started_at DESC);
CREATE INDEX idx_export_deliveries_status ON export_deliveries(delivery_status);
CREATE INDEX idx_export_monitoring_date ON export_monitoring(export_definition_id, monitoring_date DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_export_jobs_definition_status ON export_jobs(export_definition_id, job_status);
CREATE INDEX idx_export_deliveries_job_status ON export_deliveries(export_job_id, delivery_status);
```

---

## 5. Constraints & Business Rules

### Export Job Lifecycle
```sql
-- PENDING → RUNNING → (EXTRACTING, TRANSFORMING, COMPRESSING, ENCRYPTING, UPLOADING) → COMPLETED
-- Failed jobs can be retried
-- Cancelled jobs cannot be rerun
```

### Data Quality
```sql
-- Validate row counts match between export and destination
-- Hash file after delivery to verify integrity
-- Track data lineage for audit purposes
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V11.0__Initialize_export_schema.sql
V11.1__Add_schedules_and_jobs.sql
V11.2__Add_deliveries_and_lineage.sql
V11.3__Add_monitoring.sql
V11.4__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Export job execution history
- Data delivery metrics
- Export format distribution
- Destination system analytics
- Data volume trends

### ML Feature Inputs
- Export job performance prediction
- Optimal export schedule prediction
- Anomaly detection in export volumes
- Predictive capacity planning

### Business Intelligence
- Export health dashboard
- Job performance metrics
- Delivery success rates
- Data volume analysis
- Export cost attribution

### Data Governance
- Complete data lineage
- Export audit trail
- Data sensitivity tracking
- Compliance verification
- Access certification

---

## 8. Scalability Considerations

### Partitioning Strategy

**export_jobs (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', started_at))
```

**export_deliveries (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', delivery_started_at))
```

---

## 9. Monitoring & Observability

### Key Metrics
- Jobs scheduled vs. executed
- Job success rate
- Delivery success rate
- Average job duration
- Data volume exported
- File compression ratio

### Alerts
- Job failure detected
- Delivery failure spike
- Data validation failure
- Schedule missed
- Performance degradation

---

## 10. External Integration Points

### Supported Destinations
- **S3/MinIO**: Object storage for data lake
- **SFTP**: Secure file transfer
- **BigQuery**: Google Analytics destination
- **Databricks**: Data analytics platform
- **Snowflake**: Cloud data warehouse
- **PostgreSQL**: Direct to analytics DB

### Protocol Handling
- S3/MinIO: AWS SDK with presigned URLs
- SFTP: JSCH library with key-based auth
- BigQuery: Google Cloud API with service account
- Snowflake: JDBC connection with private key

---

## 11. Data Formats

### CSV
- Encoding: UTF-8
- Delimiter: Comma
- Quote char: Double quote
- Line endings: LF

### Parquet
- Compression: Snappy
- Version: v2
- Timestamps: ISO 8601
- Optimal for columnar analytics

### JSON Lines
- One JSON object per line
- UTF-8 encoding
- Suitable for streaming

---

## Summary

**Total Tables**: 7  
**Total Indexes**: 12+  
**Audit Coverage**: 100%  
**Export Formats**: CSV, JSON, Parquet, Arrow, ORC, XML  
**Destinations**: 6+ external systems  
**Analytics-Ready**: Complete export history and lineage tracking  

