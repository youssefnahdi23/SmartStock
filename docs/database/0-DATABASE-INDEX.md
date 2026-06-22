# SmartStock Database Specifications Index

**Last Updated**: 2026-06-22  
**Status**: Complete Documentation  
**Version**: 1.0  

---

## Overview

This documentation provides comprehensive database specifications for all microservices in the SmartStock AI platform. Each service has its own dedicated PostgreSQL database following the principle of data isolation and service independence.

---

## Services & Databases

| Service | Database | Purpose | Tables | Focus |
|---------|----------|---------|--------|-------|
| **Identity** | identity_db | Authentication & Authorization | 7 | Security, RBAC, Audit |
| **Product** | product_db | Product Master Data | 8 | Catalog, Barcodes, SKUs |
| **Inventory** | inventory_db | Stock Tracking & Movements | 11 | Real-time Levels, Events |
| **Warehouse** | warehouse_db | Facility Management | 10 | Operations, Locations |
| **Supplier** | supplier_db | Vendor Management | 8 | Contracts, Performance |
| **Order** | order_db | Order Management | 8 | Fulfillment, Shipments |
| **Customer** | customer_db | CRM & Relationships | 9 | Lifecycle, Segments |
| **Audit** | audit_db | Compliance & Audit Trail | 9 | Immutable Logs, Forensics |
| **Notification** | notification_db | Communication | 9 | Queue, Delivery, Templates |
| **Reporting** | reporting_db | Analytics & Reports | 8 | Metrics, Dashboards, Cache |
| **Data Export** | export_db | Data Pipeline | 7 | Exports, Lineage, Delivery |

**Total**: 11 microservices, 11 databases, 94 tables, 140+ indexes

---

## Quick Navigation

### 1. Identity Service
**File**: `1-IDENTITY-SERVICE.md`  
**Focus**: User management, authentication, role-based access control  
**Key Tables**: users, roles, permissions, user_roles, user_sessions, audit_logs  
**Data**: JWT tokens, password hashes, role assignments  
**Analytics**: Login patterns, role distribution, access certification

---

### 2. Product Service
**File**: `2-PRODUCT-SERVICE.md`  
**Focus**: Product master data and catalog  
**Key Tables**: products, categories, product_barcodes, product_skus, product_images  
**Data**: Product specifications, barcodes, QR codes, variants  
**Analytics**: Product lifecycle, catalog size, SKU performance

---

### 3. Inventory Service
**File**: `3-INVENTORY-SERVICE.md`  
**Focus**: Real-time stock tracking and movements  
**Key Tables**: inventory_levels, stock_movements, stock_in, stock_out, inventory_snapshots  
**Data**: Stock levels, movements (immutable), damaged inventory, holds  
**Analytics**: Inventory value, stock-out patterns, shrinkage analysis

---

### 4. Warehouse Service
**File**: `4-WAREHOUSE-SERVICE.md`  
**Focus**: Warehouse operations and facilities  
**Key Tables**: warehouses, zones, aisles, shelves, bins, equipment, metrics  
**Data**: Physical locations, capacity, equipment maintenance, staff  
**Analytics**: Utilization rates, equipment efficiency, operational KPIs

---

### 5. Supplier Service
**File**: `5-SUPPLIER-SERVICE.md`  
**Focus**: Supplier relationship management  
**Key Tables**: suppliers, supplier_products, supplier_deliveries, supplier_metrics  
**Data**: Vendor profiles, contracts, delivery history, performance scores  
**Analytics**: On-time delivery %, quality metrics, vendor concentration

---

### 6. Order Service
**File**: `6-ORDER-SERVICE.md`  
**Focus**: Order management and fulfillment  
**Key Tables**: orders, order_items, order_fulfillments, order_shipments, order_returns  
**Data**: Customer orders, fulfillment status, shipping, payments  
**Analytics**: Order volume, fulfillment rate, return rate, revenue tracking

---

### 7. Customer Service
**File**: `7-CUSTOMER-SERVICE.md`  
**Focus**: Customer relationship management  
**Key Tables**: customers, customer_contacts, customer_segments, customer_interactions  
**Data**: Customer profiles, addresses, preferences, communication history  
**Analytics**: Lifetime value, churn risk, segmentation, engagement metrics

---

### 8. Audit Service
**File**: `8-AUDIT-SERVICE.md`  
**Focus**: Immutable audit trail and compliance  
**Key Tables**: audit_events, audit_compliance_checks, audit_export_logs  
**Data**: All system events (immutable), approval workflows, compliance records  
**Analytics**: Event patterns, compliance status, forensic investigations

---

### 9. Notification Service
**File**: `9-NOTIFICATION-SERVICE.md`  
**Focus**: Notification delivery and communication  
**Key Tables**: notification_queue, notification_deliveries, notification_bounce_list  
**Data**: Message templates, delivery tracking, bounce/unsubscribe lists  
**Analytics**: Delivery metrics, channel performance, engagement rates

---

### 10. Reporting Service
**File**: `10-REPORTING-SERVICE.md`  
**Focus**: Aggregated metrics and analytics  
**Key Tables**: daily_metrics, monthly_metrics, yearly_metrics, report_executions  
**Data**: Pre-aggregated KPIs, report definitions, dashboard configs  
**Analytics**: Trends, KPI tracking, multi-dimensional analysis

---

### 11. Data Export Service
**File**: `11-DATA-EXPORT-SERVICE.md`  
**Focus**: Data export and pipeline management  
**Key Tables**: export_definitions, export_jobs, export_deliveries, export_data_lineage  
**Data**: Export configurations, job history, delivery tracking, data lineage  
**Analytics**: Export patterns, data quality, compliance verification

---

## Key Architectural Principles

### 1. Data Isolation
- Each service has its own PostgreSQL database
- No direct cross-service database access
- Communication via REST APIs or events only

### 2. Immutable Audit Trails
- Every service includes `audit_logs` table
- Complete history of all changes
- No delete operations on audit tables
- Supports compliance and forensic analysis

### 3. Soft Deletes
- `deleted_at` field for logical deletion
- Historical data retained for analytics
- Enables undo/recovery capabilities

### 4. Comprehensive Indexing
- Primary indexes on frequently queried columns
- Composite indexes for common query patterns
- Covering indexes for read optimization
- Estimated 12-20 indexes per database

### 5. Audit Fields on All Tables
```sql
-- Standard audit fields (all tables)
created_at TIMESTAMP WITH TIME ZONE
updated_at TIMESTAMP WITH TIME ZONE
created_by UUID
updated_by UUID
deleted_at TIMESTAMP WITH TIME ZONE (optional)
```

### 6. Event-Driven Architecture
- Every business action generates an event
- Events stored in stock_movements, order_items, audit_logs
- Immutable event records enable replay
- Events exported for analytics/AI

### 7. Analytics-First Design
- Daily, monthly, yearly aggregation levels
- Snapshots for time-series analysis
- Event streams for feature engineering
- Data export to data lake (Parquet format)

### 8. Scalability Strategies

**Partitioning**:
- Time-based partitioning (monthly) for large tables
- Hash partitioning for distributed loads
- Automatic archive of old partitions

**Replication**:
- Read replicas for analytics queries
- Hot standby for high availability
- Cross-region for disaster recovery

**Caching**:
- Redis for session cache
- Query result caching (daily/hourly TTL)
- CDN for product images

---

## Database Design Patterns

### 1. Master-Detail Pattern
```
warehouses (1) ----→ (M) warehouse_zones
          ├──→ (M) warehouse_aisles
          └──→ (M) warehouse_shelves
```

### 2. Many-to-Many Pattern
```
products (M) ----→ (M) product_categories
users (M) ----→ (M) user_roles ----→ (M) roles
roles (M) ----→ (M) role_permissions ----→ (M) permissions
```

### 3. Event Sourcing Pattern
```
stock_movements (immutable parent)
├── stock_in (event details)
├── stock_out (event details)
├── stock_transfer (event details)
└── stock_adjustments (event details)
```

### 4. Audit Log Pattern
```
-- Every table has corresponding audit_logs
-- Stores: old_values, new_values, actor_id, timestamp
-- Immutable: No UPDATE/DELETE allowed
-- Enables: Complete change history
```

### 5. Soft Delete Pattern
```
-- All entities have deleted_at field
-- Queries: WHERE deleted_at IS NULL
-- Recovery: Update deleted_at = NULL
-- Preservation: Historical data available
```

### 6. Time-Series Pattern
```
daily_metrics
  ├── metric_date
  ├── metric_category
  ├── metric_name
  ├── dimensions (product, warehouse, supplier)
  └── aggregations (sum, avg, min, max)
```

---

## Migration Strategy

### Version Control
- Flyway versioning: V{service_number}.{sub_version}
- Example: V3.0 (Inventory), V8.1 (Audit)

### Migration Phases

**Phase 1**: Core tables (V*.0)
- Master tables
- Core relationships
- Initial constraints

**Phase 2**: Extended features (V*.1)
- Additional tables
- Complex relationships
- Business rules

**Phase 3**: Performance (V*.2)
- Indexes
- Composite keys
- Optimization

**Phase 4**: Analytics (V*.3)
- Snapshots
- Aggregations
- Time-series

**Phase 5**: Advanced (V*.4)
- Partitioning
- Materialized views
- Performance tuning

### Zero-Downtime Migration
1. Add new column as NULLABLE
2. Backfill data in background
3. Add constraints/default values
4. Drop old column (in separate migration)
5. Update application code between steps

---

## Security Considerations

### Authentication
- JWT tokens stored in `user_sessions`
- Password hashes using BCrypt
- No plaintext passwords stored

### Authorization
- Role-based access control (RBAC)
- Fine-grained permissions (CREATE, READ, UPDATE, DELETE)
- Access control lists (ACLs) per resource

### Encryption
- TLS in transit (HTTPS mandatory)
- At-rest encryption for sensitive data
- Encrypted backups

### Audit & Compliance
- Immutable audit logs
- Complete change history
- Compliance reporting (SOX, HIPAA, GDPR, PCI-DSS)
- Data retention policies

---

## Performance Optimization

### Query Optimization
- Covering indexes for frequent queries
- Partial indexes for filtered lookups
- Statistics updating (daily)

### Caching Strategy
- Session cache (Redis)
- Query result cache (TTL-based)
- Materialized views for aggregations

### Batch Operations
- Bulk insert: Up to 1000 records per transaction
- CSV imports: 10K+ records with parallel loading
- Archive old partitions monthly

### Connection Pooling
- Pool size: 20-50 connections
- Prepared statements: All queries
- Connection timeout: 30 seconds

---

## Monitoring & Observability

### Key Metrics Per Service
- **Throughput**: Requests per second
- **Latency**: P50, P95, P99 response times
- **Error Rate**: Failed operations percentage
- **Storage**: Database size growth
- **Connections**: Active database connections

### Common Alerts
- Query response time exceeds threshold
- Slow query detected (> 1 second)
- Connection pool exhaustion
- Disk space threshold exceeded
- Replication lag detected
- Failed backup detected

### Logging Strategy
- Structured JSON logging
- Correlation ID for request tracking
- Request ID for operation tracking
- Service name in all logs

---

## Analytics & AI Readiness

### Data Export Pipeline
```
Operational DB → Export Jobs → File Format (CSV/Parquet)
                                    ↓
                            Data Lake (MinIO)
                                    ↓
                        Data Warehouse (Analytics)
                                    ↓
                        Feature Store (ML Training)
```

### Event Streams for AI
- Stock movements → Demand forecasting
- Order patterns → Sales prediction
- Delivery times → Logistics optimization
- Customer behavior → Churn prediction
- Inventory aging → Obsolescence detection

### Feature Engineering
- RFM analysis (customer value)
- Time-series decomposition (trends, seasonality)
- Anomaly detection (outliers)
- User behavior clustering (segmentation)

---

## Compliance & Governance

### Data Retention
- Audit logs: 7 years (regulatory requirement)
- Transaction logs: 3 years (operational)
- Daily metrics: 3 years (analytics)
- Yearly metrics: 10 years (historical)

### Compliance Frameworks
- **SOX**: Financial controls, audit trails
- **HIPAA**: Health data protection (if applicable)
- **GDPR**: Right to be forgotten, data minimization
- **PCI-DSS**: Payment card data security

### Data Privacy
- PII masking in logs
- Encryption of sensitive data
- Access control enforcement
- Audit trail of data access

---

## Disaster Recovery

### Backup Strategy
- Full backup: Daily (off-peak)
- Incremental: Every 6 hours
- WAL archiving: Continuous
- Retention: 30 days full, 7 days incremental

### Recovery Procedures
- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 15 minutes
- Point-in-time recovery: Available 30 days
- Cross-region replication for DR

### High Availability
- Read replicas: 2-3 standby replicas
- Load balancing: Round-robin
- Automatic failover: 5-minute activation
- Read-only replica for analytics

---

## Cost Optimization

### Storage Optimization
- Partition old data → Archive to cold storage
- Compression: GZIP for CSV, Snappy for Parquet
- Deduplication: Remove duplicate audit entries
- Cleanup: Delete expired cache entries

### Performance vs. Cost
- Index selection: Balance query speed vs. insert overhead
- Materialized views: Precompute expensive aggregations
- Caching: Reduce database query load
- Partitioning: Improve query efficiency

---

## Standards & Conventions

### Naming Conventions
**Tables**: `snake_case` (e.g., `user_sessions`)  
**Columns**: `snake_case` (e.g., `created_at`)  
**Indexes**: `idx_table_column` (e.g., `idx_users_email`)  
**Constraints**: `ck_table_rule` (e.g., `ck_users_email_valid`)  

### Data Types
**IDs**: UUID (distributed systems)  
**Timestamps**: TIMESTAMP WITH TIME ZONE  
**Money**: DECIMAL(15, 2) (not FLOAT)  
**Status**: VARCHAR with CHECK constraint  
**JSON**: JSONB (for flexible attributes)  

### NULL Handling
- `created_at`: NOT NULL (always)
- `deleted_at`: NULL (until deleted)
- `updated_by`: NOT NULL (audit requirement)
- `optional_field`: NULL by default

---

## Testing & Validation

### Unit Tests
- Data type validation
- Constraint validation
- Default value testing
- NULL handling tests

### Integration Tests
- Foreign key relationships
- Cascade delete behavior
- Transaction rollback scenarios
- Audit trail completeness

### Performance Tests
- Index effectiveness
- Query execution plans
- Connection pool behavior
- Load testing (1000s of concurrent users)

### Compliance Tests
- Audit log immutability
- Data retention policies
- Access control enforcement
- Encryption verification

---

## Future Enhancements

### Planned Features
1. **Graph Database**: For complex relationships (Q4 2026)
2. **Elasticsearch**: Full-text search across services (Q1 2027)
3. **Time-Series DB**: InfluxDB for metrics (Q2 2027)
4. **Data Warehouse**: BigQuery integration (Q3 2027)
5. **ML Pipeline**: Databricks for model training (Q4 2027)

### Evolution Path
```
Year 1 (2026): Operational Excellence
  - Optimize queries
  - Improve monitoring
  - Strengthen security

Year 2 (2027): Analytics Platform
  - Data warehouse
  - Business intelligence
  - Self-service BI

Year 3 (2028): AI/ML Platform
  - Predictive analytics
  - Recommendation engine
  - Autonomous optimization
```

---

## Support & Documentation

### For Developers
- Query templates: Common patterns documented
- Troubleshooting guide: Common issues
- Performance tuning: Index strategies
- Backup/restore procedures: Operational runbooks

### For Data Engineers
- ETL pipeline specs: Data transformation rules
- Data quality checks: Validation rules
- Export formats: Parquet/CSV specifications
- Lineage tracking: Data provenance

### For Operations
- Capacity planning: Growth projections
- Scaling strategies: Horizontal/vertical scaling
- Monitoring setup: Prometheus/Grafana configs
- Incident response: Runbooks for common issues

---

## Contact & Escalation

**Database Architecture Owner**: Architecture Team  
**Service-Specific Questions**: Individual service teams  
**Security/Compliance Issues**: Security team  
**Performance Issues**: Database team  

---

## Summary

- **Total Services**: 11
- **Total Databases**: 11
- **Total Tables**: 94
- **Total Indexes**: 140+
- **Audit Coverage**: 100%
- **Analytics Ready**: Yes
- **AI/ML Ready**: Yes
- **Compliance**: SOX, HIPAA, GDPR, PCI-DSS
- **High Availability**: Yes
- **Disaster Recovery**: Yes

This comprehensive database specification ensures SmartStock AI is built on a solid, scalable, secure, and analytics-ready foundation.

