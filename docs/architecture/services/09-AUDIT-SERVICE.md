# 9. Audit Service

**Bounded Context:** Audit & Compliance  
**Database:** `audit_db` (PostgreSQL)  
**Port:** 8009  
**Team:** Compliance & Security  

---

## Purpose

The Audit Service maintains the immutable, append-only event log of all business events. It is the compliance engine for the platform, enabling audit trails, regulatory reporting, and data recovery.

---

## Responsibilities

- Immutable event log (append-only, no deletions)
- Compliance audit trails for regulatory requirements
- Data access tracking (who accessed what)
- Change tracking (who changed what, when)
- Compliance reporting (GDPR, SOX, data retention)
- Event storage and retrieval
- Data retention policy enforcement
- Event replay capability (for disaster recovery)

---

## Database Ownership

**Schema:** `audit_db`

**Core Tables:**
```sql
audit_events (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL,
  event_type VARCHAR NOT NULL,
  aggregate_id UUID NOT NULL,
  aggregate_type VARCHAR,
  user_id UUID,
  timestamp TIMESTAMP NOT NULL,
  correlation_id VARCHAR,
  payload JSONB,
  metadata JSONB,
  source_service VARCHAR,
  environment VARCHAR,
  created_at TIMESTAMP NOT NULL,
  
  -- Indexes on frequently queried columns
  INDEX idx_event_type (event_type),
  INDEX idx_aggregate_id (aggregate_id),
  INDEX idx_user_id (user_id),
  INDEX idx_timestamp (timestamp),
  INDEX idx_aggregate_type (aggregate_type)
)

data_access_logs (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  resource_type VARCHAR,
  resource_id VARCHAR,
  action VARCHAR,
  access_timestamp TIMESTAMP NOT NULL,
  result ENUM ('allowed', 'denied'),
  reason_if_denied TEXT
)

change_logs (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  entity_type VARCHAR,
  entity_id VARCHAR,
  change_type ENUM ('create', 'update', 'delete'),
  before_state JSONB,
  after_state JSONB,
  changed_fields VARCHAR,
  timestamp TIMESTAMP NOT NULL
)

compliance_reports (
  id UUID PRIMARY KEY,
  report_type VARCHAR,
  generated_at TIMESTAMP,
  period_start DATE,
  period_end DATE,
  report_data JSONB,
  generated_by UUID
)

data_retention_policy (
  id UUID PRIMARY KEY,
  entity_type VARCHAR,
  retention_days INT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

archived_events (
  id UUID PRIMARY KEY,
  original_event_id UUID,
  event_type VARCHAR,
  aggregate_id UUID,
  archived_at TIMESTAMP,
  archived_location VARCHAR,
  retention_expires_at TIMESTAMP
)
```

---

## Events Published

### 1. AuditEventRecorded
**When:** Event received and logged  
**Consumers:** Data Export Service (archive old events)

### 2. ComplianceReportGenerated
**When:** Periodic compliance report created  
**Consumers:** Notification Service

### 3. DataRetentionExecuted
**When:** Old data archived/deleted per policy  
**Consumers:** Logging (archive complete)

### 4. AccessDenied
**When:** Unauthorized access attempt  
**Consumers:** Notification Service, Security monitoring

---

## Events Consumed

### From All Services
- **All Domain Events:** Subscribe to all event types for centralized audit log
  - UserCreated, UserActivated, UserDeactivated
  - ProductCreated, ProductUpdated, ProductDiscontinued
  - StockIn, StockOut, InventoryAdjusted
  - PurchaseOrderCreated, PurchaseOrderReceived
  - SalesOrderCreated, SalesOrderShipped
  - WarehouseTransferCompleted
  - Any other business event

### From Identity Service
- **LoginAttempted:** Track login attempts for security monitoring

---

## REST APIs

**Base URL:** `/api/v1/audit`

### Event Retrieval
- `GET /events` - Query audit events (paginated, filtered)
- `GET /events/{eventId}` - Get specific event
- `GET /events/by-aggregate/{aggregateId}` - Events for entity
- `GET /events/by-user/{userId}` - Events initiated by user
- `GET /events/by-type/{eventType}` - Events of specific type

### Query Parameters
- `from_date` - ISO-8601 start date
- `to_date` - ISO-8601 end date
- `user_id` - Filter by user
- `event_type` - Filter by event type
- `aggregate_type` - Filter by entity type
- `page` - Page number (0-indexed)
- `size` - Records per page (max 1000)

### Access Logs
- `GET /access-logs` - Access audit trail
- `GET /access-logs/denied` - Denied access attempts

### Change Logs
- `GET /change-logs` - Change history (who changed what)
- `GET /change-logs/{entityId}` - History for entity

### Compliance Reporting
- `GET /reports` - List available reports
- `POST /reports/generate` - Generate compliance report (async)
- `GET /reports/{reportId}` - Get report details
- `POST /reports/{reportId}/export` - Export report

### Retention Policies
- `GET /retention-policies` - List data retention policies
- `POST /retention-policies` - Create/update policy
- `GET /retention-policies/apply` - Apply retention policies (cleanup old data)

---

## Dependencies

**Event Sources:** ALL services

**No Synchronous Dependencies:** Audit Service is write-only consumer

---

## Data Consistency Patterns

### Event Immutability

Once an event is recorded:
- **No updates allowed** (immutable append-only log)
- **No deletions allowed** (except by compliance/legal order)
- **Retention periods** (7 years minimum for compliance)
- **Archive policy** (old events moved to cold storage)

### Compliance Snapshot

```
Daily Compliance Report (automated):
1. Query all events from 24h ago
2. Aggregate by type, user, entity
3. Generate report summary
4. Store in compliance_reports table
5. Publish ComplianceReportGenerated event
6. Notification Service emails compliance officer
```

---

## Future Scalability

### Event Partitioning
- Partition audit_events by date (monthly or quarterly)
- Archive old partitions to object storage (MinIO)

### Long-Term Storage
- Move archived events to data lake (Parquet format)
- Keep hot data in PostgreSQL (last 3 months)
- Keep warm data in MinIO (3 months - 7 years)

### Analytics Integration
- Export audit events to data warehouse
- Enable forensic analysis queries
- Generate audit dashboards

### Compliance Automation
- Auto-generate GDPR reports (data subject access requests)
- Auto-generate SOX compliance reports
- Auto-enforce data retention policies

---

## Deployment Checklist

- [ ] `audit_db` PostgreSQL created with large storage
- [ ] Database migrations applied
- [ ] Immutability constraints verified (no update triggers)
- [ ] Event subscription configured (all services)
- [ ] Retention policies defined and enforced
- [ ] Archival strategy configured (cold storage location)
- [ ] Compliance report generation scheduled
- [ ] Monitoring/alerting configured
- [ ] Access control to audit logs strict (audit admins only)
- [ ] Encryption configured (at rest and in transit)

