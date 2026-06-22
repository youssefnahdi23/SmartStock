# Database Specification: Audit Service

**Service**: Audit Service  
**Purpose**: Centralized immutable audit trail for all system operations  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Audit Service maintains an immutable, centralized audit trail of all system operations across all microservices.

### High-Level Architecture
```
audit_events (central immutable log)
├── audit_event_details (1:M)
├── audit_event_attachments (1:M)
└── audit_compliance_checks (1:M)
```

---

## 2. Tables Specification

### 2.1 `audit_events` Table
**Purpose**: Central immutable audit trail of all system events

```sql
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID UNIQUE NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    action_type VARCHAR(100) NOT NULL,
    actor_id UUID,
    actor_type VARCHAR(50),
    source_service VARCHAR(100) NOT NULL,
    source_ip INET,
    user_agent TEXT,
    correlation_id UUID NOT NULL,
    request_id UUID NOT NULL,
    trace_id VARCHAR(255),
    parent_trace_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_code VARCHAR(100),
    error_message TEXT,
    old_values JSONB,
    new_values JSONB,
    change_summary TEXT,
    business_context JSONB,
    security_classification VARCHAR(50) DEFAULT 'INTERNAL',
    is_sensitive BOOLEAN DEFAULT false,
    requires_approval BOOLEAN DEFAULT false,
    approval_status VARCHAR(50),
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    retention_policy_id UUID,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: 
- `timestamp`: Event timestamp (immutable)
- `actor_id`: User who performed the action
- `source_service`: Originating microservice
- `correlation_id`: Cross-service tracing
- `request_id`: Unique request identifier

**Immutability**: No UPDATE/DELETE allowed on this table

**Constraints**:
```sql
CONSTRAINT event_id_unique UNIQUE (event_id),
CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'AUTHORIZE', 'EXECUTE', 'APPROVE', 'REJECT', 'EXPORT')),
CONSTRAINT valid_status CHECK (status IN ('SUCCESS', 'FAILURE', 'PARTIAL', 'PENDING_APPROVAL')),
CONSTRAINT valid_security_classification CHECK (security_classification IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED'))
```

**Indexes**:
```sql
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp DESC);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_actor ON audit_events(actor_id);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_source_service ON audit_events(source_service);
CREATE INDEX idx_audit_events_correlation ON audit_events(correlation_id);
CREATE INDEX idx_audit_events_request ON audit_events(request_id);
CREATE INDEX idx_audit_events_trace ON audit_events(trace_id);
```

**Partitioning Strategy**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', timestamp))
```

**Analytics**: Complete system audit trail, compliance reporting

---

### 2.2 `audit_event_details` Table
**Purpose**: Store detailed metadata and context for audit events

```sql
CREATE TABLE audit_event_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_event_id UUID NOT NULL REFERENCES audit_events(id),
    detail_key VARCHAR(255) NOT NULL,
    detail_value TEXT,
    detail_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Indexes**:
```sql
CREATE INDEX idx_audit_details_event ON audit_event_details(audit_event_id);
CREATE INDEX idx_audit_details_key ON audit_event_details(detail_key);
```

---

### 2.3 `audit_event_attachments` Table
**Purpose**: Store file/document attachments related to audit events

```sql
CREATE TABLE audit_event_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_event_id UUID NOT NULL REFERENCES audit_events(id),
    attachment_name VARCHAR(500) NOT NULL,
    attachment_path VARCHAR(1000) NOT NULL,
    file_size INT,
    file_hash VARCHAR(255),
    content_type VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Indexes**:
```sql
CREATE INDEX idx_audit_attachments_event ON audit_event_attachments(audit_event_id);
```

---

### 2.4 `audit_compliance_checks` Table
**Purpose**: Track compliance checks and certifications

```sql
CREATE TABLE audit_compliance_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    check_date DATE NOT NULL,
    compliance_framework VARCHAR(100) NOT NULL,
    check_type VARCHAR(100) NOT NULL,
    audit_period_start DATE,
    audit_period_end DATE,
    total_events_checked INT,
    total_events_matched INT,
    compliance_status VARCHAR(50) NOT NULL,
    findings TEXT,
    remediation_required BOOLEAN DEFAULT false,
    remediation_deadline DATE,
    checked_by UUID NOT NULL,
    certified_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_compliance_check UNIQUE (check_date, compliance_framework, check_type),
    CONSTRAINT valid_compliance_status CHECK (compliance_status IN ('COMPLIANT', 'NON_COMPLIANT', 'PARTIAL_COMPLIANCE', 'PENDING'))
);
```

**Indexes**:
```sql
CREATE INDEX idx_compliance_checks_date ON audit_compliance_checks(check_date DESC);
CREATE INDEX idx_compliance_checks_framework ON audit_compliance_checks(compliance_framework);
```

**Analytics**: Compliance reporting, audit trail completeness

---

### 2.5 `audit_event_searches` Table
**Purpose**: Track audit searches for forensic analysis

```sql
CREATE TABLE audit_event_searches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    search_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    search_query JSONB NOT NULL,
    search_type VARCHAR(50),
    results_found INT,
    executed_by UUID NOT NULL,
    execution_time_ms INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Indexes**:
```sql
CREATE INDEX idx_searches_date ON audit_event_searches(search_date DESC);
CREATE INDEX idx_searches_executed_by ON audit_event_searches(executed_by);
```

**Analytics**: Search pattern analysis, audit access patterns

---

### 2.6 `audit_retention_policies` Table
**Purpose**: Define retention policies for different event types

```sql
CREATE TABLE audit_retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_name VARCHAR(255) NOT NULL,
    description TEXT,
    entity_type VARCHAR(100),
    action_type VARCHAR(100),
    event_type VARCHAR(100),
    retention_days INT NOT NULL,
    archive_after_days INT DEFAULT 365,
    delete_after_days INT DEFAULT 2555,
    compliance_requirement VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT positive_retention CHECK (retention_days > 0),
    CONSTRAINT archive_less_than_delete CHECK (archive_after_days <= delete_after_days)
);
```

**Indexes**:
```sql
CREATE INDEX idx_retention_policies_entity ON audit_retention_policies(entity_type, action_type);
```

---

### 2.7 `audit_event_approvals` Table
**Purpose**: Track approval workflows for sensitive operations

```sql
CREATE TABLE audit_event_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_event_id UUID NOT NULL REFERENCES audit_events(id),
    approval_type VARCHAR(100) NOT NULL,
    required_approvals INT,
    received_approvals INT DEFAULT 0,
    approval_status VARCHAR(50) DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED'))
);
```

**Indexes**:
```sql
CREATE INDEX idx_approvals_event ON audit_event_approvals(audit_event_id);
CREATE INDEX idx_approvals_status ON audit_event_approvals(approval_status);
```

---

### 2.8 `audit_approval_responses` Table
**Purpose**: Track individual approval responses

```sql
CREATE TABLE audit_approval_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_event_approval_id UUID NOT NULL REFERENCES audit_event_approvals(id),
    approved_by UUID NOT NULL,
    approval_response VARCHAR(50) NOT NULL,
    approval_comment TEXT,
    responded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    response_context JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_approval_response CHECK (approval_response IN ('APPROVED', 'REJECTED', 'PENDING'))
);
```

**Indexes**:
```sql
CREATE INDEX idx_approval_responses_approval ON audit_approval_responses(audit_event_approval_id);
```

---

### 2.9 `audit_export_logs` Table
**Purpose**: Track audit trail exports for compliance

```sql
CREATE TABLE audit_export_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_type VARCHAR(100) NOT NULL,
    export_format VARCHAR(50),
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE,
    total_events_exported INT,
    export_file_path VARCHAR(1000),
    export_file_hash VARCHAR(255),
    exported_by UUID NOT NULL,
    export_reason VARCHAR(200),
    export_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_export_format CHECK (export_format IN ('CSV', 'JSON', 'XML', 'PARQUET'))
);
```

**Indexes**:
```sql
CREATE INDEX idx_exports_date ON audit_export_logs(export_date DESC);
CREATE INDEX idx_exports_exported_by ON audit_export_logs(exported_by);
```

**Analytics**: Export frequency, audit trail completeness

---

## 3. Indexing Strategy

### Primary Indexes
```sql
-- Timestamp-based queries
CREATE INDEX idx_audit_events_timestamp ON audit_events(timestamp DESC);

-- Entity lookup
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);

-- Actor lookup
CREATE INDEX idx_audit_events_actor ON audit_events(actor_id);

-- Service lookup
CREATE INDEX idx_audit_events_service ON audit_events(source_service);

-- Tracing
CREATE INDEX idx_audit_events_correlation ON audit_events(correlation_id);
```

### Composite Indexes
```sql
-- Time-range + entity queries
CREATE INDEX idx_audit_entity_time ON audit_events(entity_type, entity_id, timestamp DESC);

-- Service + action + time
CREATE INDEX idx_audit_service_action_time ON audit_events(source_service, action_type, timestamp DESC);
```

---

## 4. Constraints & Business Rules

### Immutability
```sql
-- No UPDATE or DELETE operations allowed on audit_events
-- Changes are tracked as new events
-- All changes to retention/compliance are audited
```

### Data Integrity
```sql
-- Every event must have: timestamp, entity_type, action_type, source_service
-- correlation_id enables cross-service tracing
-- request_id links related operations
```

---

## 5. Archival & Retention Strategy

### Retention Lifecycle
```
Active (0-365 days)
  ↓
Archived (365-2555 days) - cold storage
  ↓
Deleted (after 2555 days, per policy)
```

### Backup Strategy
- Daily backups of immutable audit tables
- Replicate to audit data warehouse
- Cross-region replication for DR

---

## 6. Migration Strategy

### Flyway Versioning
```
V8.0__Initialize_audit_schema.sql
V8.1__Add_compliance_and_retention.sql
V8.2__Add_approvals_and_exports.sql
V8.3__Add_performance_indexes.sql
V8.4__Add_partitioning.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily audit event snapshot
- Compliance certification records
- Event approval workflows
- Export audit trail
- Event detail analytics

### ML Feature Inputs
- Anomaly detection in audit events
- Pattern recognition for security threats
- User behavior baselining
- Access pattern analysis
- Risk scoring for entities

### Compliance & Forensics
- Complete audit trail reconstruction
- Timeline analysis for incidents
- Access certification reports
- Approval workflow traceability
- Change tracking for compliance

### Security Analytics
- Failed operation patterns
- Sensitive data access patterns
- Unauthorized operation attempts
- Security event frequency
- Actor risk profiling

---

## 8. Security & Compliance

### Data Protection
- Audit logs encrypted at rest
- TLS for all audit log transmission
- Immutable storage (WORM - Write Once Read Many)
- Cryptographic hashing of events

### Access Control
- Limited audit log access (need approval)
- Service account has read-only access
- All audit log access is itself audited
- Approval workflows for sensitive queries

### Compliance
- Supports SOX, HIPAA, GDPR, PCI-DSS
- Configurable retention policies
- Evidence export for audits
- Compliance certification tracking

---

## 9. Scalability Considerations

### Partitioning Strategy

**audit_events (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', timestamp))
-- Monthly partitions for easy archival
```

### Performance Optimization
- Archive old partitions to separate storage
- Materialized views for common queries
- Pre-aggregated compliance reports
- Read-only replicas for analytics

---

## 10. Monitoring & Observability

### Key Metrics
- Events per second
- Average query response time
- Audit log volume growth
- Compliance check status
- Export request volume

### Alerts
- Audit log write lag
- Compliance violations
- Unusual event volume spike
- Audit trail gap detected
- Approval timeout exceeded

---

## 11. Query Patterns

### Common Queries
```sql
-- Find all events for entity
SELECT * FROM audit_events 
WHERE entity_type = 'ORDER' AND entity_id = ? 
ORDER BY timestamp DESC;

-- Find events by actor
SELECT * FROM audit_events 
WHERE actor_id = ? 
AND timestamp >= ? AND timestamp <= ?
ORDER BY timestamp DESC;

-- Find failed operations
SELECT * FROM audit_events 
WHERE status = 'FAILURE' 
AND timestamp >= DATE_TRUNC('day', NOW())
ORDER BY timestamp DESC;

-- Compliance report
SELECT action_type, COUNT(*) 
FROM audit_events 
WHERE timestamp >= ? AND timestamp < ?
GROUP BY action_type;
```

---

## Summary

**Total Tables**: 9  
**Total Indexes**: 15+  
**Immutability**: 100% (write-once)  
**Partitioning**: Monthly on timestamp  
**Retention**: Configurable (default 7 years)  
**Compliance-Ready**: Full audit trail with approval workflows  
**Analytics-Ready**: Complete event history for security and forensics  

