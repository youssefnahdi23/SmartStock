# SmartStock AI - Microservices Catalog

**Version:** 1.0  
**Last Updated:** June 2026  
**Status:** Foundation Phase (Phase 1)

---

## Table of Contents

1. [Overview](#overview)
2. [Service Directory](#service-directory)
3. [Communication Architecture](#communication-architecture)
4. [Data Flow](#data-flow)
5. [Deployment Units](#deployment-units)

---

## Overview

SmartStock AI consists of **13 independent microservices** organized around business domains (Bounded Contexts in Domain-Driven Design). Each service:

- **Owns its database** (no cross-service database sharing)
- **Owns its business logic** (single responsibility per domain)
- **Publishes domain events** (other services consume asynchronously)
- **Exposes REST APIs** (for synchronous queries when needed)
- **Is independently deployable** (can scale and update independently)

### Guiding Principles

| Principle | Implementation |
|-----------|-----------------|
| **Data Ownership** | One service owns the source of truth for each concept |
| **Eventual Consistency** | Services update local caches from events, not direct DB access |
| **Event-Driven** | Business events are the primary integration mechanism |
| **API-First** | All service communication via well-defined contracts |
| **AI-Ready** | All events captured for future analytics/ML pipelines |

---

## Service Directory

| # | Service | Bounded Context | Database | Port | Purpose |
|---|---------|-----------------|----------|------|---------|
| 1 | [Identity Service](services/01-IDENTITY-SERVICE.md) | Identity & Access | identity_db | 8001 | User authentication, authorization, roles |
| 2 | [Product Service](services/02-PRODUCT-SERVICE.md) | Product Catalog | product_db | 8002 | Product master data, SKUs, pricing |
| 3 | [Inventory Service](services/03-INVENTORY-SERVICE.md) | Physical Inventory | inventory_db | 8003 | Stock levels, movements, transactions |
| 4 | [Warehouse Service](services/04-WAREHOUSE-SERVICE.md) | Warehouse Operations | warehouse_db | 8004 | Locations, capacity, transfers |
| 5 | [Supplier Service](services/05-SUPPLIER-SERVICE.md) | Supplier Management | supplier_db | 8005 | Supplier profiles, performance |
| 6 | [Customer Service](services/06-CUSTOMER-SERVICE.md) | Customer Management | customer_db | 8006 | Customer profiles, segmentation |
| 7 | [Purchase Order Service](services/07-PURCHASE-ORDER-SERVICE.md) | Purchase Workflows | purchase_db | 8007 | POs, receiving, supplier invoices |
| 8 | [Sales Order Service](services/08-SALES-ORDER-SERVICE.md) | Sales Workflows | sales_db | 8008 | Sales orders, fulfillment, shipping |
| 9 | [Audit Service](services/09-AUDIT-SERVICE.md) | Audit & Compliance | audit_db | 8009 | Immutable event log, compliance |
| 10 | [Notification Service](services/10-NOTIFICATION-SERVICE.md) | Notifications & Alerts | notification_db | 8010 | Alerts, email, subscriptions |
| 11 | [Reporting Service](services/11-REPORTING-SERVICE.md) | Business Analytics | reporting_db | 8011 | Reports, dashboards, KPIs |
| 12 | [Data Export Service](services/12-DATA-EXPORT-SERVICE.md) | Data Platform | data_export_db | 8012 | Data lake exports, transformations |
| 13 | [Analytics Service](services/13-ANALYTICS-SERVICE.md) | Analytics Engine | analytics_db | 8013 | Aggregations, metrics, calculations |

---

## Communication Architecture

### Synchronous Communication (REST APIs)

Used for:
- Request-response patterns
- Real-time queries
- User-facing operations

**Minimized dependencies:**
```
Product Service → Identity Service (validate permissions)
Inventory Service → Product Service (validate product exists)
Warehouse Service → Identity Service (validate permissions)
Reporting Service → Inventory Service (read-only queries)
Data Export Service → Audit Service (read event logs)
```

**Response Time Target:** <200ms per request

### Asynchronous Communication (Event-Driven)

Used for:
- Business events published across bounded contexts
- Eventual consistency updates
- AI/analytics data collection
- Decoupled service updates

**Event Standard Format:**
```json
{
  "eventId": "UUID",
  "eventType": "ProductCreated|StockIn|OrderPlaced|...",
  "aggregateId": "entity-id",
  "aggregateType": "Product|Inventory|Order",
  "timestamp": "ISO-8601",
  "version": 1,
  "userId": "user-id",
  "correlationId": "request-id",
  "payload": { /* domain-specific data */ },
  "metadata": {
    "source": "service-name",
    "environment": "production"
  }
}
```

**Event Broker:** Kafka (primary), RabbitMQ (alternative)

**Event Durability:**
- 7-year retention for compliance
- Replayable for disaster recovery
- Dead-letter queues for failed processing

---

## Data Flow

### Example 1: Stock Receiving (Purchase Order)

```
1. Warehouse Operator scans goods using JavaFX Desktop App
   ↓
2. App calls: POST /api/v1/warehouse/stock-in
   ↓
3. Warehouse Service validates
   ↓
4. Warehouse Service publishes event: StockIn
   ↓
   ├→ Inventory Service subscribes → updates stock levels
   ├→ Notification Service subscribes → sends low-stock alerts
   ├→ Reporting Service subscribes → updates dashboards
   ├→ Audit Service subscribes → immutable audit log
   └→ Data Export Service subscribes → streams to data lake
```

### Example 2: Sales Order Fulfillment (Saga Pattern)

```
1. Customer places order via REST API
   ↓
2. Sales Order Service publishes: SalesOrderCreated
   ↓
3. Inventory Service subscribes:
   - Reserves stock
   - Publishes: StockReserved
   ↓
4. Warehouse Service subscribes:
   - Assigns pick location
   - Publishes: PickAssigned
   ↓
5. Fulfillment Service subscribes:
   - Packs order
   - Publishes: OrderPacked
   ↓
6. Sales Order Service subscribes (from chain above):
   - Updates order status → SalesOrderFulfilled
   ↓
7. Notification Service alerts customer: Order shipped
```

---

## Deployment Units

### Production Deployment

Each service is independently deployable:

```
smartstock-identity-service:8001
smartstock-product-service:8002
smartstock-inventory-service:8003
smartstock-warehouse-service:8004
smartstock-supplier-service:8005
smartstock-customer-service:8006
smartstock-purchase-service:8007
smartstock-sales-service:8008
smartstock-audit-service:8009
smartstock-notification-service:8010
smartstock-reporting-service:8011
smartstock-data-export-service:8012
smartstock-analytics-service:8013
```

### Infrastructure Services

```
PostgreSQL Cluster (4 instances)
├── identity_db
├── product_db
├── inventory_db
└── warehouse_db (shared with supplier_db, customer_db, etc. in larger deployments)

Kafka Cluster (event broker)
Redis Cluster (caching, sessions)
MinIO (data lake)
Prometheus + Grafana (observability)
Loki (centralized logging)
Tempo (distributed tracing)
```

### Horizontal Scaling

Each service can scale independently:

```
Inventory Service (handles stock changes) → 3 instances
Sales Order Service (handles order traffic) → 2 instances
Data Export Service (batch processing) → 1 instance
```

---

## Next Steps

1. **Read Service Details:** Browse the [services directory](services/) for detailed documentation on each microservice
2. **Service Index:** See [services/README.md](services/README.md) for service categorization and reading guide
3. **Review API Contracts:** See `/docs/api/` for REST endpoint specifications
4. **Understand Events:** See `/docs/api/` for event schema definitions
5. **Check Deployment:** See `/docs/deployment/` for deployment procedures

---

## Governance

- **Architecture Decisions:** See `/docs/decisions/` (17 ADRs)
- **Code Standards:** See `/docs/standards/` (style guides, patterns)
- **Database Design:** See `/docs/database/` (schema patterns)
- **Observability:** See `/docs/observability/` (monitoring, logging, tracing)

