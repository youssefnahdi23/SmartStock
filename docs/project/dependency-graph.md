# SmartStock AI — Dependency Graph

**Document Status:** Authoritative Planning Document  
**Generated:** 2026-06-24  
**Source:** ADR-0001, ADR-0002, ADR-0003, ADR-0004, ADR-0008, ADR-0011, ADR-0012, ADR-0015, Event Catalog, API Catalog, Architecture Service Docs  
**DO NOT MODIFY** without a corresponding ADR change or architectural decision.

---

## 1. Service Dependency Matrix — Synchronous REST Calls

This table shows which services make synchronous REST calls to which other services. Per ADR-0002, synchronous calls are minimized; async events are preferred. Per ADR-0013, all REST calls between services use Resilience4j circuit breakers with timeout.

| Caller Service | Called Service | Purpose | Circuit Breaker | ADR Reference |
|---|---|---|---|---|
| API Gateway | Identity Service | JWT token validation (GET /auth/validate) | Yes — all requests pass through | ADR-0008 |
| Product Service | Identity Service | Permission validation for admin-only operations | Yes | ADR-0005, ADR-0012 |
| Warehouse Service | Identity Service | Permission validation for warehouse-specific operations | Yes | ADR-0005, ADR-0012 |
| Inventory Service | Product Service | Validate productId exists before stock operation | Yes | ADR-0003, ADR-0012 |
| Inventory Service | Warehouse Service | Validate warehouseId exists before stock operation | Yes | ADR-0003, ADR-0012 |
| Purchase Order Service | Supplier Service | Validate supplierId before PO creation | Yes | ADR-0012 |
| Purchase Order Service | Product Service | Validate productId on PO line items | Yes | ADR-0012 |
| Sales Order Service | Customer Service | Validate customerId before order creation | Yes | ADR-0012 |
| Sales Order Service | Product Service | Validate productId on SO line items; retrieve price | Yes | ADR-0012 |
| Reporting Service | Inventory Service | Read-only stock level queries (cacheable) | Yes | ADR-0012 |
| Data Export Service | Audit Service | Read event logs for export | Yes | ADR-0012 |
| Analytics Service | Reporting Service | Read aggregated metrics | Yes | ADR-0012 |

### Notes on REST Dependency Constraints

1. **API Gateway → Identity Service:** This is the only truly synchronous hard dependency in the gateway hot path. If Identity Service is down, the gateway returns 503. Mitigated by circuit breaker: if Identity Service is down, cached positive validations remain valid for 5 minutes (Redis-backed cache on gateway).

2. **Inventory → Product + Warehouse:** These validations happen on every stock operation. Both are cache-aside with Redis (product:{id} TTL 5 min, warehouse:{id} TTL 5 min), so DB pressure is low. Circuit breaker fallback: return cached data if service is down.

3. **No service calls Inventory Service synchronously in the hot path** (except Reporting, which is read-only and can tolerate eventual consistency).

4. **No circular synchronous dependencies exist** (verified below in Section 7).

---

## 2. Event Dependency Matrix — Kafka Producers and Consumers

### 2a. Event Producers (what each service publishes)

| Service | Topic | Events Published |
|---|---|---|
| Identity Service | events.identity | UserCreated, UserAuthenticated |
| Product Service | events.product | ProductCreated, ProductUpdated, ProductDeleted |
| Inventory Service | events.inventory | StockIn, StockOut, StockMoved, StockAdjusted, LowStockThresholdReached |
| Warehouse Service | events.warehouse | WarehouseCreated, WarehouseCapacityUpdated, ZoneCreated |
| Supplier Service | events.supplier | SupplierCreated, SupplierPerformanceUpdated, SupplierDeliveryRegistered |
| Customer Service | events.customer | CustomerCreated, CustomerUpdated |
| Purchase Order Service | events.order | PurchaseOrderCreated, PurchaseOrderReceived, PurchaseOrderCancelled |
| Sales Order Service | events.order | SalesOrderCreated, SalesOrderConfirmed, SalesOrderFulfilled, SalesOrderShipped, SalesOrderCancelled |
| Audit Service | (none — consumer only) | — |
| Notification Service | (none — consumer only) | — |
| Reporting Service | (none — consumer only) | — |
| Data Export Service | events.export | DataExportRequested, DataExportCompleted |
| Analytics Service | events.analytics | InventorySnapshotCreated, DailyAnalyticsReportGenerated |

### 2b. Event Consumers (what each service subscribes to)

| Service | Topics Consumed | Events Handled | Purpose |
|---|---|---|---|
| Inventory Service | events.product | ProductCreated | Initialize inventory_level record with qty=0 |
| Inventory Service | events.order | SalesOrderCreated | Reserve stock (create InventoryHold) |
| Inventory Service | events.order | SalesOrderFulfilled | Deduct stock (convert hold to StockOut movement) |
| Inventory Service | events.order | SalesOrderCancelled | Release hold (restore quantityAvailable) |
| Inventory Service | events.order | PurchaseOrderReceived | Stock-in received goods |
| Sales Order Service | events.inventory | StockReserved | Confirm order (transition PENDING→CONFIRMED) |
| Sales Order Service | events.inventory | StockReservationFailed | Cancel order (publish SalesOrderCancelled) |
| Warehouse Service | events.inventory | StockMoved | Update bin occupancy on transfer |
| Warehouse Service | events.order | SalesOrderFulfilled | Publish LocationAllocated for picked orders |
| Audit Service | events.identity | ALL events | Append to immutable audit_events table |
| Audit Service | events.product | ALL events | Append to audit_events |
| Audit Service | events.inventory | ALL events | Append to audit_events |
| Audit Service | events.warehouse | ALL events | Append to audit_events |
| Audit Service | events.supplier | ALL events | Append to audit_events |
| Audit Service | events.customer | ALL events | Append to audit_events |
| Audit Service | events.order | ALL events | Append to audit_events |
| Notification Service | events.inventory | LowStockThresholdReached | Send low-stock alert email |
| Notification Service | events.order | SalesOrderShipped | Send shipping confirmation to customer |
| Notification Service | events.order | SalesOrderCancelled | Send cancellation notification |
| Reporting Service | events.inventory | StockIn, StockOut, StockAdjusted | Update DailyMetric stock values |
| Reporting Service | events.order | SalesOrderCreated, SalesOrderFulfilled | Update DailyMetric order counts |
| Reporting Service | events.supplier | SupplierDeliveryRegistered | Update supplier performance metrics |
| Reporting Service | events.product | ProductCreated, ProductUpdated | Update product catalog metrics |
| Analytics Service | events.inventory | StockIn, StockOut | Compute InventorySnapshot aggregations |
| Analytics Service | events.order | SalesOrderFulfilled | Compute order fulfillment analytics |
| Data Export Service | (reads Audit REST) | — | Pulls audit_events via REST for Parquet export |

### 2c. Saga Event Flow — Sales Order

```
Sales Order Service
  PUBLISHES: SalesOrderCreated (events.order)
    |
    v
Inventory Service (consumer: events.order)
  PUBLISHES: StockReserved OR StockReservationFailed (events.inventory)
    |
    v
[If StockReserved]:
Warehouse Service (consumer: events.inventory)
  PUBLISHES: LocationAllocated OR AllocationFailed (events.warehouse)
    |
    v
[If LocationAllocated]:
Sales Order Service (consumer: events.warehouse)
  PUBLISHES: SalesOrderConfirmed (events.order)
    |
    v
Notification Service (consumer: events.order)
  Sends confirmation email

[If StockReservationFailed]:
Sales Order Service (consumer: events.inventory)
  PUBLISHES: SalesOrderCancelled (events.order)
  Notification Service sends cancellation email

[If AllocationFailed]:
Inventory Service (consumer: events.warehouse → compensate)
  PUBLISHES: InventoryHoldReleased (events.inventory)
Sales Order Service (consumer: events.inventory)
  PUBLISHES: SalesOrderCancelled (events.order)
```

### 2d. Saga Event Flow — Purchase Order

```
Purchase Order Service
  PUBLISHES: PurchaseOrderCreated (events.order)
    |
    v
Supplier Service (consumer: events.order)
  Records expected delivery (local only, no further event)
    |
    v
[On goods receipt — triggered by API call to Purchase Order Service]:
Purchase Order Service
  PUBLISHES: PurchaseOrderReceived (events.order)
    |
    v
Inventory Service (consumer: events.order)
  Executes stock-in, PUBLISHES StockIn (events.inventory)
    |
    v
[All standard StockIn consumers]:
  Reporting Service → update metrics
  Audit Service → audit log
  Notification Service → (if configurable threshold) notify manager
```

---

## 3. Database Dependency Isolation

Per ADR-0003, each service owns exactly one PostgreSQL database. No service may query another service's database directly. The following table confirms isolation:

| Service | Database | Port (dev) | Owner | Cross-DB FK? |
|---|---|---|---|---|
| Identity Service | smartstock_identity | 5432 | Identity Service | No |
| Product Service | smartstock_product | 5433 | Product Service | No |
| Inventory Service | smartstock_inventory | 5434 | Inventory Service | No |
| Warehouse Service | smartstock_warehouse | 5435 | Warehouse Service | No |
| Supplier Service | smartstock_supplier | 5436 | Supplier Service | No |
| Customer Service | smartstock_customer | 5437 | Customer Service | No |
| Purchase Order Service | smartstock_purchase | 5438 | Purchase Order Service | No |
| Sales Order Service | smartstack_sales | 5439 | Sales Order Service | No |
| Audit Service | smartstack_audit | 5440 | Audit Service | No |
| Notification Service | smartstack_notification | 5441 | Notification Service | No |
| Reporting Service | smartstack_reporting | 5442 | Reporting Service | No |
| Data Export Service | smartstack_export | 5443 | Data Export Service | No |
| Analytics Service | smartstack_analytics | 5444 | Analytics Service | No |

**Isolation Enforcement Mechanisms:**

1. Each service's `application.yml` configures `spring.datasource.url` pointing only to its own database; there is no second datasource configured in any service.
2. Network-level isolation in Kubernetes: NetworkPolicy restricts each service's pod to only accept connections from its own namespace; database credentials are service-specific (different username/password per DB).
3. IDs from other services stored as plain VARCHAR(36) or UUID with no database foreign key constraint. Application-level validation via REST calls before insert.

**[CONSISTENCY ISSUE CI-005]** The database spec index document (0-DATABASE-INDEX.md) states 11 services and 11 databases but the SERVICE_CATALOG.md and ADR-0001 describe 13 services (Purchase Order and Sales Order are separate). The database index was written before the order services were clarified as separate. Resolution: 13 services, 13 databases. The database spec for `6-ORDER-SERVICE.md` covers only one combined service — it must be split into purchase-order schema and sales-order schema during M3.

---

## 4. Infrastructure Dependencies

| Service | PostgreSQL | Kafka | Redis | MinIO | SMTP |
|---|---|---|---|---|---|
| Identity Service | Yes (identity_db) | Producer (events.identity) | Yes (rate limiting, refresh token blacklist) | No | No |
| Product Service | Yes (product_db) | Producer (events.product) | Yes (product catalog cache) | No | No |
| Inventory Service | Yes (inventory_db) | Producer (events.inventory) + Consumer | Yes (distributed lock, snapshot cache) | No | No |
| Warehouse Service | Yes (warehouse_db) | Producer (events.warehouse) + Consumer | Yes (warehouse list cache) | No | No |
| Supplier Service | Yes (supplier_db) | Producer (events.supplier) | Yes (supplier list cache) | No | No |
| Customer Service | Yes (customer_db) | Producer (events.customer) | Yes (customer cache) | No | No |
| Purchase Order Service | Yes (purchase_db) | Producer (events.order) | No | No | No |
| Sales Order Service | Yes (sales_db) | Producer + Consumer (events.order, events.inventory, events.warehouse) | No | No | No |
| API Gateway | No | No | Yes (rate limiting, JWT cache) | No | No |
| Audit Service | Yes (audit_db) | Consumer (all topics) | No | No | No |
| Notification Service | Yes (notification_db) | Consumer (events.inventory, events.order) | No | No | Yes (SMTP) |
| Reporting Service | Yes (reporting_db) | Consumer (all events topics) | No | No | No |
| Data Export Service | Yes (export_db) | Producer (events.export) | No | Yes (write Parquet/CSV) | No |
| Analytics Service | Yes (analytics_db) | Consumer (events.inventory, events.order) | No | No | No |

**Infrastructure Service Availability Requirements:**

- **Kafka:** Required by all 13 services (directly or indirectly). Single point of infra failure in dev (single-node Kafka in docker-compose). Production requires 3-node cluster (ADR-0004).
- **Redis:** Required by Identity Service (JWT blacklist), API Gateway (rate limiting, JWT cache), Inventory Service (distributed lock). If Redis goes down: API Gateway rate limiting disabled (fallback); Inventory concurrent modification protection disabled (fallback to DB lock). Services remain functional in degraded mode per ADR-0011.
- **PostgreSQL:** Each service's database is local to that service. One database failure affects only that service and its consumers (via Kafka lag).
- **MinIO:** Required only by Data Export Service. Other services unaffected if MinIO is down.
- **SMTP:** Required only by Notification Service. Failed email delivery is queued and retried; does not block other services.

---

## 5. Implementation Order DAG (Text-Based)

This directed acyclic graph shows which services (or components) must be fully implemented before another can be started. Arrows mean "must complete before."

```
[M0: Common Module + Identity Domain Models + Docker Compose Infra]
                    |
                    v
[M1: Identity Service (auth endpoints, JWT, RBAC)]
                    |
                    v
[M1: API Gateway (JWT filter, rate limiting, routing)]
                    |
              +-----+-----+
              |           |
              v           v
[M2: Product  ]   [M2: Warehouse Service]
[Service      ]         |
              |         |
              |   [M2: Supplier  ]   [M2: Customer Service]
              |   [Service       ]         |
              |         |                  |
              v         v                  v
[M3: Purchase Order Service] [M3: Sales Order Service]
              |                  |
              |                  |
              +--------+---------+
                       |
                       v
         [M4: Inventory Service (full)]
                       |
                       |
              +--------+--------+
              |                 |
              v                 v
[M4: Notification Service]  [M5: Audit Service]
              |                 |
              v                 v
              +--------+---------+
                       |
                       v
    [M5: Observability Wiring (all services)]
                       |
              +--------+--------+
              |        |        |
              v        v        v
[M6: Reporting][M6: Data Export][M6: Analytics]
              |
              v
    [M7: JavaFX Desktop Client]
              |
              v
    [M8: Kubernetes + Hardening]
```

### DAG Dependency Rules

1. **API Gateway** has a runtime dependency on Identity Service (for token validation). Identity Service must be deployed before API Gateway can validate tokens.

2. **Product Service** has no runtime dependency on other services (it is a pure producer).

3. **Warehouse Service** has no runtime dependency on other services (pure producer).

4. **Supplier Service** has no runtime dependency on other services (pure producer).

5. **Customer Service** has no runtime dependency on other services (pure producer).

6. **Purchase Order Service** has REST dependency on Supplier Service (validate supplierId) and Product Service (validate productId).

7. **Sales Order Service** has REST dependency on Customer Service (validate customerId) and Product Service (validate productId).

8. **Inventory Service** has REST dependency on Product Service (validate productId) and Warehouse Service (validate warehouseId). It is also a Kafka consumer of events.order (from M3 services) and events.product.

9. **Notification Service** has no REST dependencies on other services. It only consumes Kafka events. But it must be implemented after Inventory Service because the most critical notification trigger is LowStockThresholdReached.

10. **Audit Service** has no REST dependencies on operational services. It subscribes to all Kafka topics. Must be implemented after all topics are producing events (M4).

11. **Reporting Service** has one REST dependency on Inventory Service (for real-time stock levels query) but primarily uses Kafka for data. Must be implemented after all event producers are live (M4).

12. **Data Export Service** has REST dependency on Audit Service (reads event log). Must be implemented after Audit Service.

13. **Analytics Service** has REST dependency on Reporting Service (reads aggregated metrics). Must be after Reporting Service.

14. **JavaFX Desktop Client** calls all 13 service REST APIs. Must be implemented after all services are stable.

15. **Kubernetes/Helm** packages all services. Must be done last.

---

## 6. Cross-Cutting Concerns

### 6a. Common Library (`services/common`)

All services depend on `services/common` (Maven dependency `smartstock-common:1.0.0`). Changes to `common` must be backward-compatible or a version bump and coordinated update across all services is required.

**Current contents of common (verified from source):**
- `ApiResponse<T>` — standard response wrapper
- `DomainEvent` — abstract base for all domain events

**Planned additions (M1):**
- Exception hierarchy (BusinessException, ResourceNotFoundException, ValidationException, ConflictException)
- GlobalExceptionHandler
- CorrelationIdFilter
- JwtTokenProvider
- PageRequest / PageResponse<T>

**Risk:** Any change to `DomainEvent` structure (e.g., adding a required field) propagates to all services. The `DomainEvent.timestamp` type change (LocalDateTime → Instant, tracked as CI-003) must be done in M1 before any other service is implemented.

### 6b. Security Cross-Cutting

Per ADR-0008, the API Gateway is the primary authentication enforcement point. Each downstream service:
- Receives `X-User-Id`, `X-User-Roles`, `X-User-Warehouses` headers from the gateway
- May perform additional authorization checks (e.g., inventory-service checks if user has access to the specific warehouseId)
- Does NOT re-validate JWT signature (the gateway already did this)
- Does expose `/actuator/health` without authentication (permitted at gateway level)
- Does NOT expose `/actuator` management endpoints publicly

### 6c. Observability Cross-Cutting

Per ADR-0009, every service must:
- Expose `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`
- Expose `/actuator/prometheus` (Micrometer Prometheus endpoint)
- Include `correlationId` in all log lines (via MDC, propagated by CorrelationIdFilter)
- Propagate OpenTelemetry trace context in all HTTP client calls and Kafka messages
- Use JSON structured logging via logstash-logback-encoder

These are enforced by the common module's `CorrelationIdFilter` and by the standard `application.yml` template from which all services are bootstrapped.

### 6d. Configuration Cross-Cutting

Per ADR-0017, every service:
- Reads all secrets from environment variables (never hardcoded in application.yml)
- Has `application.yml` (defaults), `application-docker.yml` (Compose overrides), `application-prod.yml` (production overrides) profiles
- Uses `spring.profiles.active=${SPRING_PROFILE:dev}` to select profile
- Validates configuration on startup via `@ConfigurationProperties @Validated`
- Fails fast if required env vars are missing

---

## 7. Circular Dependency Analysis

A circular dependency exists when Service A depends on Service B and Service B (directly or transitively) depends on Service A.

### Analysis of All REST Dependencies

```
API Gateway → Identity Service (one-way)
Product Service → Identity Service (one-way)
Warehouse Service → Identity Service (one-way)
Inventory Service → Product Service (one-way)
Inventory Service → Warehouse Service (one-way)
Purchase Order Service → Supplier Service (one-way)
Purchase Order Service → Product Service (one-way)
Sales Order Service → Customer Service (one-way)
Sales Order Service → Product Service (one-way)
Reporting Service → Inventory Service (one-way)
Data Export Service → Audit Service (one-way)
Analytics Service → Reporting Service (one-way)
```

**Conclusion: No circular REST dependencies exist.** All dependencies form a strict directed acyclic graph. The deepest chain is:

```
Analytics Service → Reporting Service → Inventory Service → Product Service → Identity Service
```
5 hops maximum. No service creates a cycle.

### Analysis of Kafka Event Dependencies (Saga)

The Sales Order Saga creates a circular event flow pattern:
- Sales Order Service publishes to events.order
- Inventory Service consumes events.order and publishes to events.inventory
- Sales Order Service consumes events.inventory

This is intentional choreography (ADR-0015) and is NOT a circular dependency — it is an event response chain. Each service is responding to an event from a different source, not calling itself.

**Potential issue:** If Inventory Service's StockReserved event handler mistakenly published another SalesOrderCreated event, an infinite loop could occur. This is mitigated by:
1. Strong typing of events (Inventory Service only publishes to events.inventory)
2. Idempotency keys (each saga step checks saga_states before executing)
3. Dead letter topic for permanently failed events (preventing infinite retry loops)

**Conclusion: No circular event dependencies exist in the designed event topology.**

---

## 8. Dependency Summary Table

This table summarizes the full dependency profile of each service at runtime:

| Service | REST Depends On | Kafka Consumes From | Kafka Publishes To | Redis | PostgreSQL | MinIO | SMTP |
|---|---|---|---|---|---|---|---|
| Identity | — | — | events.identity | Yes | identity_db | No | No |
| API Gateway | Identity | — | — | Yes | No | No | No |
| Product | Identity | — | events.product | Yes | product_db | No | No |
| Warehouse | Identity | events.inventory | events.warehouse | Yes | warehouse_db | No | No |
| Supplier | — | events.order | events.supplier | Yes | supplier_db | No | No |
| Customer | — | — | events.customer | Yes | customer_db | No | No |
| Purchase Order | Supplier, Product | — | events.order | No | purchase_db | No | No |
| Sales Order | Customer, Product | events.inventory, events.warehouse | events.order | No | sales_db | No | No |
| Inventory | Product, Warehouse | events.product, events.order | events.inventory | Yes | inventory_db | No | No |
| Audit | Audit REST (self) | ALL topics | — | No | audit_db | No | No |
| Notification | — | events.inventory, events.order | — | No | notification_db | No | Yes |
| Reporting | Inventory (REST) | events.* | — | No | reporting_db | No | No |
| Data Export | Audit (REST) | — | events.export | No | export_db | Yes | No |
| Analytics | Reporting (REST) | events.inventory, events.order | events.analytics | No | analytics_db | No | No |
