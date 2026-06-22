# 12. Data Export Service

**Bounded Context:** Data Platform  
**Database:** `data_export_db` (PostgreSQL)  
**Port:** 8012  
**Team:** Data Engineering  

---

## Purpose

The Data Export Service is responsible for exporting operational and event data to the data lake in various formats, enabling analytics, AI training, and compliance data retrieval.

---

## Responsibilities

- Export operational data to data lake (MinIO/S3)
- Event streaming to object storage
- Data format transformation (Parquet, CSV, JSON)
- Schema management and versioning
- Data quality monitoring
- Compliance data exports (GDPR subject access requests)
- Incremental/batch export scheduling
- Data retention policy enforcement

---

## Database Ownership

**Schema:** `data_export_db`

**Core Tables:**
```sql
export_jobs (
  id UUID PRIMARY KEY,
  job_name VARCHAR NOT NULL,
  job_type ENUM ('inventory_snapshot', 'stock_movements', 'sales_orders', 'events', 'compliance'),
  status ENUM ('scheduled', 'running', 'completed', 'failed'),
  source_entity VARCHAR,
  destination_path VARCHAR,
  export_format ENUM ('csv', 'json', 'parquet'),
  scheduled_at TIMESTAMP,
  started_at TIMESTAMP,
  completed_at TIMESTAMP,
  record_count INT,
  error_message TEXT
)

export_schemas (
  id UUID PRIMARY KEY,
  entity_name VARCHAR UNIQUE NOT NULL,
  schema_version INT,
  schema_definition JSONB,
  created_at TIMESTAMP,
  effective_from TIMESTAMP
)

export_audit_log (
  id UUID PRIMARY KEY,
  export_job_id UUID,
  action VARCHAR,
  details JSONB,
  created_at TIMESTAMP
)

data_quality_checks (
  id UUID PRIMARY KEY,
  export_job_id UUID,
  check_name VARCHAR,
  status ENUM ('passed', 'failed', 'warning'),
  details JSONB,
  checked_at TIMESTAMP
)

compliance_exports (
  id UUID PRIMARY KEY,
  export_type ENUM ('gdpr_sar', 'sox_report', 'general'),
  requester_id VARCHAR,
  entity_type VARCHAR,
  entity_id VARCHAR,
  status ENUM ('pending', 'generated', 'delivered', 'expired'),
  generated_at TIMESTAMP,
  expires_at TIMESTAMP,
  download_count INT
)
```

---

## Events Consumed

### From All Services
- **All Domain Events:** Archive to event log (Parquet format)

### From Audit Service
- **AuditEventRecorded:** Export to data lake

---

## Events Published

### 1. ExportJobCompleted
**When:** Export job finishes  
**Consumers:** Audit Service, Notification Service

### 2. ExportJobFailed
**When:** Export job encounters error  
**Consumers:** Notification Service (alert data team)

### 3. ComplianceExportGenerated
**When:** GDPR/SOX export ready  
**Consumers:** Notification Service

---

## REST APIs

**Base URL:** `/api/v1/data-export`

### Export Jobs
- `GET /jobs` - List export jobs (paginated)
- `POST /jobs` - Schedule new export job (admin only)
- `GET /jobs/{jobId}` - Get job status
- `POST /jobs/{jobId}/retry` - Retry failed job

### Scheduled Exports
- `GET /schedules` - List scheduled exports
- `POST /schedules` - Create recurring export
- `PUT /schedules/{scheduleId}` - Update schedule
- `DELETE /schedules/{scheduleId}` - Cancel schedule

### Schemas
- `GET /schemas` - List available export schemas
- `GET /schemas/{schemaName}` - Get schema definition
- `GET /schemas/{schemaName}/versions` - Schema version history

### Data Lake Access
- `GET /data/{entityType}` - List exported data (read from MinIO)
- `POST /data/{entityType}/download` - Download dataset
- `GET /data/query` - Query exported data (with filters)

### Compliance Exports
- `POST /compliance/gdpr-sar` - Create GDPR Subject Access Request
- `GET /compliance/exports` - List compliance exports
- `GET /compliance/exports/{exportId}` - Get export details
- `POST /compliance/exports/{exportId}/download` - Download compliance export

### Data Quality
- `GET /quality/checks` - List data quality check results
- `GET /quality/checks/{jobId}` - Checks for specific job

---

## Dependencies

**Event Sources:** All services (via audit/event bus)

**External Dependencies:**
- MinIO/S3 (data lake object storage)
- Parquet library (Apache Arrow)
- Data quality library (Great Expectations)

---

## Implementation Pattern

### Daily Event Export (Batch)

```
1. Scheduled job: 02:00 UTC daily
   ↓
2. Query all events from last 24h from Audit Service
   ↓
3. Transform to Parquet format:
   - Cast types according to export_schemas
   - Validate data quality
   - Add metadata (export_date, record_count, etc.)
   ↓
4. Upload to MinIO:
   - Path: /events/daily/{date}.parquet
   - Retention: 7 years
   ↓
5. Update export_jobs table
   ↓
6. If success: Publish ExportJobCompleted
   If error: Publish ExportJobFailed → Notification Service
```

### Stock Levels Snapshot (Weekly)

```
1. Scheduled job: Every Sunday 03:00 UTC
   ↓
2. Query current stock levels from Inventory Service
   ↓
3. Transform and export:
   - CSV format for easier import to data warehouse
   - Include warehouse, product, quantities
   ↓
4. Upload to MinIO:
   - Path: /inventory-snapshots/weekly/{date}.csv
   ↓
5. Archive old snapshots (>7 years) to cold storage
```

### GDPR Subject Access Request (On-Demand)

```
1. User/admin requests GDPR export
   ↓
2. Data Export Service queries:
   - All events involving user_id
   - All audit logs for user
   - Personal data from various services (names, emails)
   ↓
3. Compile into PDF + CSV
   ↓
4. Encrypt and upload to MinIO
   ↓
5. Generate download link (expires after 7 days)
   ↓
6. Publish ComplianceExportGenerated
   ↓
7. Notification Service sends link to user
```

---

## Data Lake Structure

```
MinIO Bucket: smartstock-datalake

/events/
  /daily/
    2026-06-20.parquet
    2026-06-21.parquet
    
/inventory-snapshots/
  /weekly/
    2026-06-15.csv
    
/sales-orders/
  /monthly/
    2026-06.parquet
    
/compliance-exports/
  /gdpr/
    2026-06-20-user-123-sar.pdf
    
/archive/
  (old data >3 months from hot storage)
```

---

## Future Scalability

### Real-Time Data Pipelines
- Stream events to Apache Kafka → Spark Streaming
- Enable real-time feature store updates
- Push to feature store for ML models

### Advanced Data Transforms
- Data cleaning and normalization
- Feature engineering for ML
- Time-series aggregations

### Data Warehouse Integration
- Automatic exports to Snowflake/BigQuery
- Integration with BI tools (Tableau, Power BI)

### Machine Learning Pipeline
- Export training data to data lake
- Auto-generate feature sets
- Version datasets for reproducibility

---

## Deployment Checklist

- [ ] `data_export_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] MinIO bucket created and configured
- [ ] Event subscription configured (Kafka/RabbitMQ)
- [ ] Export job scheduler configured
- [ ] Parquet/CSV libraries installed
- [ ] Data quality checks configured (Great Expectations)
- [ ] Retention policies defined and enforced
- [ ] Encryption configured (at rest)
- [ ] Monitoring/alerting configured
- [ ] GDPR export template created

