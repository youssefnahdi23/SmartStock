# SmartStock AI - Service Documentation Index

**Location:** `/docs/architecture/services/`

This directory contains comprehensive documentation for all 13 microservices in the SmartStock AI platform.

---

## Quick Reference

| Service | Port | Purpose | Database |
|---------|------|---------|----------|
| [Identity Service](01-IDENTITY-SERVICE.md) | 8001 | Authentication & Authorization | identity_db |
| [Product Service](02-PRODUCT-SERVICE.md) | 8002 | Product Master Data | product_db |
| [Inventory Service](03-INVENTORY-SERVICE.md) | 8003 | Stock Levels & Movements | inventory_db |
| [Warehouse Service](04-WAREHOUSE-SERVICE.md) | 8004 | Warehouse Operations & Logistics | warehouse_db |
| [Supplier Service](05-SUPPLIER-SERVICE.md) | 8005 | Supplier Management | supplier_db |
| [Customer Service](06-CUSTOMER-SERVICE.md) | 8006 | Customer Management | customer_db |
| [Purchase Order Service](07-PURCHASE-ORDER-SERVICE.md) | 8007 | Purchase Workflows | purchase_db |
| [Sales Order Service](08-SALES-ORDER-SERVICE.md) | 8008 | Sales Workflows | sales_db |
| [Audit Service](09-AUDIT-SERVICE.md) | 8009 | Immutable Event Log & Compliance | audit_db |
| [Notification Service](10-NOTIFICATION-SERVICE.md) | 8010 | Alerts & Communications | notification_db |
| [Reporting Service](11-REPORTING-SERVICE.md) | 8011 | Business Analytics & KPIs | reporting_db |
| [Data Export Service](12-DATA-EXPORT-SERVICE.md) | 8012 | Data Lake & Exports | data_export_db |
| [Analytics Service](13-ANALYTICS-SERVICE.md) | 8013 | Advanced Analytics & ML Prep | analytics_db |

---

## Service by Category

### Core Identity & Access
- [Identity Service](01-IDENTITY-SERVICE.md) - Authentication, authorization, JWT

### Product & Inventory Management
- [Product Service](02-PRODUCT-SERVICE.md) - Product master data
- [Inventory Service](03-INVENTORY-SERVICE.md) - Stock tracking
- [Warehouse Service](04-WAREHOUSE-SERVICE.md) - Physical operations

### Supply Chain & Procurement
- [Supplier Service](05-SUPPLIER-SERVICE.md) - Supplier management
- [Purchase Order Service](07-PURCHASE-ORDER-SERVICE.md) - Purchasing

### Sales & Customer
- [Customer Service](06-CUSTOMER-SERVICE.md) - Customer management
- [Sales Order Service](08-SALES-ORDER-SERVICE.md) - Sales workflows

### Operations & Compliance
- [Audit Service](09-AUDIT-SERVICE.md) - Compliance & audit trails
- [Notification Service](10-NOTIFICATION-SERVICE.md) - Alerts & communication

### Analytics & Data
- [Reporting Service](11-REPORTING-SERVICE.md) - Dashboards & KPIs
- [Data Export Service](12-DATA-EXPORT-SERVICE.md) - Data lake exports
- [Analytics Service](13-ANALYTICS-SERVICE.md) - Advanced analytics

---

## Reading Guide

### For Developers Implementing Services

1. **Start here:** [SERVICE_CATALOG.md](../SERVICE_CATALOG.md) - Overview & communication patterns
2. **Read your service:** Click your service link above
3. **Understand events:** Review "Events Consumed" and "Events Published" sections
4. **Check dependencies:** Review "Dependencies" section
5. **See examples:** Review "Data Consistency Patterns" section
6. **Check deployment:** Follow "Deployment Checklist"

### For Architects Planning Integration

1. **Read:** [SERVICE_CATALOG.md](../SERVICE_CATALOG.md) - Architecture principles
2. **Review:** "Communication Architecture" section for sync/async patterns
3. **Check:** "Data Flow" examples to understand event orchestration
4. **Study:** "Saga Pattern" (Distributed Transactions) in relevant services:
   - [Sales Order Service](08-SALES-ORDER-SERVICE.md) - Order fulfillment saga
   - [Purchase Order Service](07-PURCHASE-ORDER-SERVICE.md) - 3-way invoice matching saga
   - [Warehouse Service](04-WAREHOUSE-SERVICE.md) - Receiving/shipping workflows

### For Operations/DevOps Teams

1. **Infrastructure:** See docker-compose.yml for 13 services + infrastructure
2. **Deployment:** Each service has "Deployment Checklist" section
3. **Monitoring:** Each service lists key metrics to monitor
4. **Scaling:** Each service has "Future Scalability" section with considerations

### For Data/Analytics Teams

1. **Data Lake:** [Data Export Service](12-DATA-EXPORT-SERVICE.md) - Export formats & paths
2. **Analytics:** [Analytics Service](13-ANALYTICS-SERVICE.md) - Metrics, trends, anomalies
3. **Reporting:** [Reporting Service](11-REPORTING-SERVICE.md) - Dashboards & KPIs
4. **Audit Trail:** [Audit Service](09-AUDIT-SERVICE.md) - Immutable event log
5. **Features:** [Analytics Service](13-ANALYTICS-SERVICE.md) - Feature store for ML

---

## Event Flow Diagrams

### Stock Receiving (Purchase Order)

```
1. Supplier delivers goods
   ↓
2. Warehouse Operator uses JavaFX app
   ↓
3. Scans items: POST /warehouses/stock-in
   ↓
4. Warehouse Service:
   - Creates ReceivingTask
   - Publishes ReceivingCompleted event
   ↓
5. Subscribers:
   ├→ Inventory Service: Publish StockIn
   ├→ Purchase Order Service: Update PO status
   ├→ Notification Service: Alert operators
   ├→ Audit Service: Record event
   └→ Data Export Service: Stream to data lake
```

### Sales Order Fulfillment

```
1. Customer places order: POST /sales-orders
   ↓
2. Sales Order Service:
   - Validate customer credit
   - Publish SalesOrderCreated
   ↓
3. Inventory Service receives:
   - Reserve stock
   - Publish StockReserved
   ↓
4. Warehouse Service receives:
   - Create picking task
   ↓
5. Operator picks/packs: JavaFX app
   ↓
6. Warehouse Service publishes:
   - ShippingCompleted
   ↓
7. Sales Order Service receives:
   - Publish OrderShipped
   ↓
8. Inventory Service receives:
   - Publish StockOut (deduct reserved)
   ↓
9. Notification Service alerts customer
```

---

## Key Architectural Patterns

### 1. Eventual Consistency

Services update local caches from events, not direct database access.

**Example:** Product Service updates product name → Published event → Inventory Service receives → Updates local cache

### 2. Saga Pattern (Distributed Transactions)

Long-running workflows coordinated through events, not transactions.

**Example:** Order fulfillment involves 5 services, coordinated via events

### 3. Event Sourcing

All important actions generate immutable events stored in Audit Service.

**Enables:** Audit trails, compliance reporting, replaying history

### 4. CQRS (Command Query Responsibility Segregation)

- **Commands:** Create/update operations go through service APIs
- **Queries:** Complex queries read from Reporting Service (denormalized views)

### 5. Anti-Corruption Layer

When integrating with external systems (e.g., suppliers), translate their formats to SmartStock domain model.

---

## Database Per Service

Each service owns its database (no shared tables between services).

```
identity_db
  → PostgreSQL instance 1

product_db, inventory_db, warehouse_db, supplier_db, customer_db, 
purchase_db, sales_db, notification_db, reporting_db, 
data_export_db, analytics_db, audit_db
  → PostgreSQL instances 2-4 (distributed)
```

**Rationale:** Independent scaling, deployment, and team ownership.

---

## REST API Base Paths

All services follow the same versioning pattern:

```
/api/v1/{resource}      - Current stable API
/api/v2/{resource}      - Future major version
```

**Examples:**
- `/api/v1/products`
- `/api/v1/inventory/stock/{productId}/{warehouseId}`
- `/api/v1/sales-orders`

---

## Event Message Format (Standard)

All events follow the same schema (see ADR-0002):

```json
{
  "eventId": "UUID",
  "eventType": "ProductCreated|StockIn|OrderShipped|...",
  "aggregateId": "entity-id",
  "aggregateType": "Product|Inventory|Order",
  "timestamp": "ISO-8601",
  "version": 1,
  "userId": "user-id",
  "correlationId": "request-id",
  "payload": { "domain-specific": "data" },
  "metadata": {
    "source": "service-name",
    "environment": "production"
  }
}
```

---

## Deployment Architecture

### Development

```
docker-compose up
- 13 services on localhost:8001-8013
- PostgreSQL, Kafka, Redis, MinIO, Observability stack
- All environment variables in .env
```

### Production

```
Kubernetes cluster
- Each service: multi-replica deployment
- PostgreSQL: managed database (RDS, Cloud SQL, etc.)
- Kafka: managed event broker (Confluent Cloud, AWS MSK, etc.)
- MinIO: object storage (S3, GCS, etc.)
- Prometheus/Grafana: observability
- Istio/ingress: API Gateway
```

---

## Health Checks & Monitoring

All services include:

- `/actuator/health` - Spring Boot health endpoint
- Prometheus metrics at `/actuator/prometheus`
- Loki logs integration
- Tempo distributed tracing

---

## Future Evolution

### Phase 2: Analytics Platform
- Enhanced Reporting Service
- Advanced dashboards
- Historical analytics

### Phase 3: Data Platform
- Data warehouse integration
- ML-ready data exports
- Feature store implementation

### Phase 4: AI Platform
- Demand forecasting
- Inventory optimization
- Supplier risk prediction
- Customer churn prediction

---

## Common Questions

**Q: How do services communicate?**  
A: Synchronously via REST (minimized), asynchronously via events (primary).

**Q: What if a service is down?**  
A: Async events are queued (Kafka), retried automatically. UI requests fail gracefully with 503.

**Q: How do I trace a business flow?**  
A: Use correlation_id in logs. Tempo provides distributed tracing.

**Q: Can I query another service's data?**  
A: No direct database access. Use REST APIs or subscribe to events.

**Q: How does data consistency work?**  
A: Eventual consistency. Services react to events, update local caches.

**Q: How do I add a new service?**  
A: Follow the template in any service doc. Add to docker-compose.yml. Submit PR.

---

## Related Documentation

- **Architecture Decisions:** `/docs/decisions/` (17 ADRs)
- **Code Standards:** `/docs/standards/` (style guides, patterns)
- **Database Design:** `/docs/database/` (schema patterns)
- **API Specifications:** `/docs/api/` (OpenAPI definitions)
- **Deployment:** `/docs/deployment/` (K8s manifests, CI/CD)
- **Observability:** `/docs/observability/` (monitoring, logging, tracing)

---

## Getting Help

- **Architecture Questions:** Refer to ADRs in `/docs/decisions/`
- **Service Implementation:** Read service documentation + ADR-0012 (DDD)
- **Event Integration:** Check ADR-0002 (Communication Model)
- **Deployment:** See docker-compose.yml + `/docs/deployment/`
- **Code Examples:** Look at "Data Consistency Patterns" in each service doc

