# SmartStock AI - Complete API Catalog

**Last Updated:** 2026-06-20  
**Version:** 1.0  
**Status:** Comprehensive - All 13 Services Documented

---

## Executive Summary

The SmartStock AI Platform API Catalog provides complete, enterprise-grade API documentation for all 13 microservices. Each service operates independently with its own database, authentication, and domain responsibilities while maintaining strict service boundaries and event-driven communication.

**Key Statistics:**
- **Total Microservices:** 13
- **Total API Endpoints:** 125+ endpoints
- **Authentication Method:** JWT Bearer (RS256, 4096-bit RSA)
- **API Versioning:** URL-based (/api/v1/*, /api/v2/*)
- **Response Format:** Standardized JSON envelope
- **Rate Limiting:** Enforced at API Gateway
- **Documentation Scope:** Endpoints, DTOs, Examples, Error Codes, Authentication, Authorization

---

## 1. Core Services (Authentication & Security)

### Identity Service `/api/v1/identity`
**Role:** Authentication gateway, user management, role-based access control  
**Port:** 8001  
**Database:** PostgreSQL (Identity DB)  
**Key Responsibilities:**
- JWT token generation and validation
- User authentication (login/logout)
- User management (CRUD)
- Role hierarchy and permission enforcement
- Rate limiting (login: 5 req/min, password reset: 3/hour)

**Endpoints:** 16 total
- Authentication (login, logout, refresh, validate)
- User Management (create, read, update, delete, list)
- Role Management (CRUD roles and permissions)
- Permission Assignment

**Documentation:** [01-IDENTITY-API.md](01-IDENTITY-API.md)

---

## 2. Catalog Services (Product Master Data)

### Product Service `/api/v1/products`
**Role:** Product catalog management and attributes  
**Port:** 8002  
**Database:** PostgreSQL (Product DB)  
**Key Responsibilities:**
- Product CRUD operations
- Product categorization
- Pricing management
- Barcode and QR code generation
- Bulk import/export operations
- Supplier relationship mapping

**Endpoints:** 13 total
- Product Operations (create, read, update, delete, list)
- Categories (CRUD, hierarchical)
- Barcode Management (generate, validate, update)
- Bulk Operations (import with dry-run, export)

**Documentation:** [02-PRODUCT-API.md](02-PRODUCT-API.md)

---

## 3. Inventory Services (Stock Management)

### Inventory Service `/api/v1/inventory`
**Role:** Stock tracking, movements, and physical counts  
**Port:** 8003  
**Database:** PostgreSQL (Inventory DB)  
**Key Responsibilities:**
- Stock in/out operations
- Stock transfers between warehouses
- Stock adjustments (damage, discrepancies)
- Physical inventory counts
- Stock reservations and allocation
- Negative stock prevention

**Endpoints:** 12 total
- Stock Movements (in, out, transfer, adjustment)
- Physical Counts (create, finalize, variance tracking)
- Reservations (create, cancel, allocation)
- Stock Queries (current levels, history, details)

**Documentation:** [03-INVENTORY-API.md](03-INVENTORY-API.md)

---

### Warehouse Service `/api/v1/warehouses`
**Role:** Physical infrastructure management  
**Port:** 8004  
**Database:** PostgreSQL (Warehouse DB)  
**Key Responsibilities:**
- Warehouse CRUD and deactivation
- Zone management (temperature-controlled, standard)
- Shelf management (physical storage levels)
- Bin management (individual storage units)
- Capacity tracking (space, weight, pallets)
- Utilization reporting

**Endpoints:** 11 total
- Warehouse Operations (create, read, update, list, deactivate)
- Zone Management (CRUD)
- Shelf Management (CRUD)
- Bin Management (CRUD, allocation algorithm)
- Capacity Reporting

**Documentation:** [04-WAREHOUSE-API.md](04-WAREHOUSE-API.md)

---

## 4. Partner Services (Suppliers & Customers)

### Supplier Service `/api/v1/suppliers`
**Role:** Vendor management and performance tracking  
**Port:** 8005  
**Database:** PostgreSQL (Supplier DB)  
**Key Responsibilities:**
- Supplier CRUD operations
- Supplier performance metrics (on-time delivery, quality)
- Delivery history tracking
- Lead time calculations
- Supplier suspension/resumption
- Relationship management

**Endpoints:** 9 total
- Supplier Operations (create, read, update, list)
- Performance Metrics (on-time rates, quality scores)
- Delivery History (query, analysis)
- Suspension/Resumption Workflows

**Documentation:** [05-SUPPLIER-API.md](05-SUPPLIER-API.md)

---

### Customer Service `/api/v1/customers`
**Role:** Customer management and relationship tracking  
**Port:** 8006  
**Database:** PostgreSQL (Customer DB)  
**Key Responsibilities:**
- Customer CRUD operations
- Customer credit management
- Address management
- Customer segmentation
- Satisfaction tracking
- Payment history and metrics

**Endpoints:** 12 total
- Customer Operations (create, read, update, list)
- Credit Management (limits, scoring, usage)
- Addresses (CRUD)
- Satisfaction Feedback (record, retrieve)
- Segment Analysis

**Documentation:** [06-CUSTOMER-API.md](06-CUSTOMER-API.md)

---

## 5. Order Services (Procurement & Fulfillment)

### Purchase Order Service `/api/v1/purchase-orders`
**Role:** Supplier order lifecycle management  
**Port:** 8007  
**Database:** PostgreSQL (PO DB)  
**Key Responsibilities:**
- PO creation and confirmation
- Delivery registration and tracking
- Quality issue reporting
- Partial/full delivery handling
- PO cancellation workflows
- Supplier performance impact

**Endpoints:** 7 total
- PO Lifecycle (create, confirm, delivery, cancel)
- Delivery Registration (confirm, quality check)
- Quality Issues (report, tracking)
- Performance Tracking

**Documentation:** [07-PURCHASE-ORDER-API.md](07-PURCHASE-ORDER-API.md)

---

### Sales Order Service `/api/v1/sales-orders`
**Role:** Customer order lifecycle management  
**Port:** 8008  
**Database:** PostgreSQL (SO DB)  
**Key Responsibilities:**
- SO creation and confirmation
- Stock reservation and picking
- Shipment management
- Delivery tracking
- Order cancellation workflows
- Customer performance analysis

**Endpoints:** 9 total
- SO Lifecycle (create, confirm, pick, ship, deliver)
- Picking Workflows (optimization, confirmation)
- Shipment Tracking (updates, status)
- Performance Analysis

**Documentation:** [08-SALES-ORDER-API.md](08-SALES-ORDER-API.md)

---

## 6. Cross-Cutting Services

### Audit Service `/api/v1/audit`
**Role:** Compliance logging and forensics  
**Port:** 8009  
**Database:** PostgreSQL (Audit DB - Immutable)  
**Key Responsibilities:**
- Immutable audit logging
- User activity tracking
- Entity history (who changed what, when)
- Compliance reporting (7-year retention)
- Real-time critical alerts
- Sensitive data protection

**Endpoints:** 6 total
- Audit Logs (query, filter, export)
- User Activity (timeline, analytics)
- Entity History (changes, diff view)
- Compliance Reports

**Documentation:** [09-AUDIT-API.md](09-AUDIT-API.md)

---

### Notification Service `/api/v1/notifications`
**Role:** Alert and communication management  
**Port:** 8010  
**Database:** PostgreSQL (Notification DB)  
**Key Responsibilities:**
- Email alert dispatch (Brevo API)
- User notification preferences
- Low stock alerts
- Order status notifications
- System-level alerts
- Quiet hours enforcement

**Endpoints:** 4 total
- Email Alerts (send, queue management)
- User Notifications (retrieve, mark read)
- Preferences (configure, update)
- Alert Subscriptions

**Documentation:** [10-NOTIFICATION-API.md](10-NOTIFICATION-API.md)

---

## 7. Analytics & Reporting Services

### Reporting Service `/api/v1/reports`
**Role:** Business intelligence and dashboards  
**Port:** 8011  
**Database:** Analytics Store (read-only aggregated data)  
**Key Responsibilities:**
- Inventory summary reports
- Sales performance analysis
- Warehouse utilization reports
- Supplier performance reports
- Custom report generation
- Scheduled report delivery

**Endpoints:** 6 total
- Pre-built Reports (inventory, sales, warehouse, supplier)
- Custom Reports (create, schedule)
- Report Downloads (PDF, Excel, CSV)
- Report History

**Documentation:** [11-REPORTING-API.md](11-REPORTING-API.md)

---

### Analytics Service `/api/v1/analytics`
**Role:** Statistical analysis and KPI calculations  
**Port:** 8013  
**Database:** Analytics Store (aggregated data)  
**Key Responsibilities:**
- KPI calculations (turnover, days out, accuracy)
- Demand pattern analysis
- Warehouse efficiency metrics
- Supplier trend analysis
- Customer segmentation
- Forecasting data (statistical methods)
- Anomaly detection

**Endpoints:** 7 total
- KPI Retrieval (inventory, customer, warehouse)
- Demand Patterns (analysis, forecasting)
- Efficiency Metrics (warehouse operations)
- Trend Analysis (suppliers, products)
- Segmentation Analysis (ABC, RFM)
- Anomaly Alerts

**Documentation:** [13-ANALYTICS-API.md](13-ANALYTICS-API.md)

---

## 8. Data Platform Services (AI-Ready)

### Data Export Service `/api/v1/exports`
**Role:** High-quality structured data extraction  
**Port:** 8012  
**Database:** PostgreSQL (Export Config) + MinIO (Data Lake)  
**Key Responsibilities:**
- Export stock movements (time-series data)
- Export inventory snapshots (daily/scheduled)
- Export orders (sales & purchase)
- Export supplier performance data
- Data quality assurance
- Parquet/CSV/JSON formatting
- S3-compatible delivery

**Endpoints:** 7 total
- Stock Movements Export (time-series)
- Inventory Snapshots (historical)
- Order Exports (sales & purchase)
- Supplier Data (performance)
- Export Status & History
- Scheduled Exports

**Documentation:** [12-DATA-EXPORT-API.md](12-DATA-EXPORT-API.md)

---

## Authentication & Authorization

### JWT Token Structure (RS256 - 4096-bit RSA)

**Access Token (1 hour expiry):**
```json
{
  "sub": "user-123",
  "name": "John Doe",
  "email": "john@company.com",
  "roles": ["WAREHOUSE_MANAGER", "INVENTORY_OPERATOR"],
  "permissions": ["inventory:read", "inventory:write", "stock:transfer"],
  "iat": 1687270000,
  "exp": 1687273600,
  "iss": "smartstock-identity-service"
}
```

**Refresh Token (30 day expiry):**
- Used to obtain new access tokens
- Can be revoked per user
- Stored securely with hash

### Role Hierarchy

```
SYSTEM_ADMIN (all permissions)
├── WAREHOUSE_MANAGER
│   ├── INVENTORY_OPERATOR
│   ├── STOCK_AUDITOR
│   └── WAREHOUSE_SUPERVISOR
├── SUPPLIER_MANAGER
├── CUSTOMER_MANAGER
├── PURCHASE_MANAGER
├── SALES_MANAGER
├── REPORTER
└── AUDITOR
```

---

## Standard Response Format

### Success Response (200, 201, etc.)
```json
{
  "data": {
    // Resource object or array
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-abc-123",
    "page": 0,              // if paginated
    "size": 20,             // if paginated
    "total": 100,           // if paginated
    "totalPages": 5         // if paginated
  }
}
```

### Error Response (4xx, 5xx)
```json
{
  "errors": [
    {
      "code": "ERR_INVALID_REQUEST",
      "message": "Inventory quantity cannot be negative",
      "field": "quantity",
      "severity": "ERROR"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-abc-123",
    "status": 400
  }
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `ERR_INVALID_REQUEST` | 400 | Invalid request parameters |
| `ERR_UNAUTHORIZED` | 401 | Missing or invalid JWT token |
| `ERR_FORBIDDEN` | 403 | Insufficient permissions |
| `ERR_NOT_FOUND` | 404 | Resource not found |
| `ERR_CONFLICT` | 409 | Resource conflict (e.g., duplicate key) |
| `ERR_VALIDATION_FAILED` | 422 | Field validation error |
| `ERR_RATE_LIMITED` | 429 | Too many requests |
| `ERR_INTERNAL_SERVER_ERROR` | 500 | Server error (no details exposed) |
| `ERR_SERVICE_UNAVAILABLE` | 503 | Service temporarily unavailable |

---

## API Gateway Configuration

### Rate Limiting Policies

```
/identity/login          → 5 requests per minute
/identity/refresh        → 20 requests per minute
/identity/password-reset → 3 requests per hour
/list endpoints (GET)    → 50 requests per minute
/other endpoints         → 100 requests per minute
/exports/*               → 10 requests per minute
/analytics/*             → 50 requests per minute
```

### Request/Response Headers

**Required Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
X-Correlation-ID: <optional_correlation_id>
```

**Response Headers:**
```
X-Trace-ID: trace-abc-123
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1687273600
Content-Type: application/json
```

---

## Event-Driven Architecture

All business actions emit domain events through message broker (RabbitMQ/Kafka).

### Event Format

```json
{
  "eventId": "evt-uuid-123",
  "eventType": "InventoryStockInCompleted",
  "aggregateId": "inv-123",
  "aggregateType": "Inventory",
  "timestamp": "2026-06-20T12:00:00Z",
  "version": 1,
  "userId": "user-123",
  "correlationId": "corr-123",
  "payload": {
    "productId": "prod-001",
    "warehouseId": "W01",
    "quantity": 100,
    "unitCost": 50.00
  }
}
```

### Event Types by Service

**Inventory Service:**
- StockInStarted
- StockInCompleted
- StockOutStarted
- StockOutCompleted
- StockTransferred
- StockAdjusted
- PhysicalCountStarted
- PhysicalCountCompleted
- ReservationCreated
- ReservationCancelled

**Product Service:**
- ProductCreated
- ProductUpdated
- ProductDeleted
- BarcodeGenerated

**Order Services:**
- PurchaseOrderCreated
- PurchaseOrderConfirmed
- DeliveryRegistered
- QualityIssueReported
- SalesOrderCreated
- SalesOrderConfirmed
- OrderPicked
- OrderShipped
- OrderDelivered

**Supplier/Customer Services:**
- SupplierCreated
- SupplierUpdated
- SupplierSuspended
- CustomerCreated
- CustomerUpdated
- CustomerSegmentChanged

**Notification Service:**
- AlertTriggered
- EmailSent
- NotificationRead

**Audit Service:**
- AuditLogCreated
- SuspiciousActivityDetected

---

## Pagination

All list endpoints support pagination:

```
GET /api/v1/products?page=0&size=20&sort=name,asc

Query Parameters:
- page: 0-indexed page number (default: 0)
- size: results per page, max 100 (default: 20)
- sort: comma-separated field names with direction (asc/desc)

Response includes:
{
  "data": [...],
  "meta": {
    "page": 0,
    "size": 20,
    "total": 1234,
    "totalPages": 62
  }
}
```

---

## API Versioning Strategy

- **URL-based versioning:** `/api/v1/*`, `/api/v2/*`
- **Deprecation policy:** Minimum 2 years support per version
- **Backwards compatibility:** Maintained within major version
- **Breaking changes:** New major version required
- **Version sunset:** 6-month deprecation notice before removal

---

## Implementation Checklist

### Per Service Implementation
- [ ] OpenAPI/Swagger specification
- [ ] Rate limiting configuration
- [ ] Error handling middleware
- [ ] Request logging (structured JSON)
- [ ] Authentication middleware
- [ ] Authorization (RBAC) enforcement
- [ ] Event publishing setup
- [ ] Database migrations (Flyway)
- [ ] Integration tests
- [ ] API contract tests

---

## Service Dependency Map

```
API Gateway
├── Identity Service (central auth)
├── Product Service (catalog)
├── Inventory Service
│   └── Warehouse Service (location data)
├── Supplier Service
├── Customer Service
├── Purchase Order Service
│   └── Inventory Service (stock-in)
├── Sales Order Service
│   └── Inventory Service (stock-out)
├── Audit Service (logs all)
├── Notification Service
├── Reporting Service
│   └── Analytics Store (aggregated)
├── Analytics Service
│   └── Analytics Store (aggregated)
└── Data Export Service
    └── MinIO (data lake)
```

---

## Future Enhancements

1. **OpenAPI/Swagger Integration**
   - Machine-readable API contracts
   - Auto-generated client SDKs
   - Interactive API explorer

2. **GraphQL Layer** (optional, future)
   - Flexible query language
   - Reduced over-fetching

3. **WebSocket Support** (future)
   - Real-time notifications
   - Live dashboard updates

4. **API Gateway Features** (future)
   - Request transformation
   - Response caching
   - Circuit breaker patterns

5. **Observability Enhancements**
   - Distributed tracing (OpenTelemetry)
   - Centralized logging (ELK/Loki)
   - Prometheus metrics

---

## Documentation Maintenance

- Review ADRs quarterly for API changes
- Update error codes when new validations added
- Track rate limit adjustments per usage patterns
- Document event schema changes in dedicated Event Catalog
- Maintain backwards compatibility checklist

---

## Quick Reference Links

- [Identity Service API](01-IDENTITY-API.md)
- [Product Service API](02-PRODUCT-API.md)
- [Inventory Service API](03-INVENTORY-API.md)
- [Warehouse Service API](04-WAREHOUSE-API.md)
- [Supplier Service API](05-SUPPLIER-API.md)
- [Customer Service API](06-CUSTOMER-API.md)
- [Purchase Order Service API](07-PURCHASE-ORDER-API.md)
- [Sales Order Service API](08-SALES-ORDER-API.md)
- [Audit Service API](09-AUDIT-API.md)
- [Notification Service API](10-NOTIFICATION-API.md)
- [Reporting Service API](11-REPORTING-API.md)
- [Data Export Service API](12-DATA-EXPORT-API.md)
- [Analytics Service API](13-ANALYTICS-API.md)

---

**API Catalog Version:** 1.0  
**Last Updated:** 2026-06-20  
**Maintained By:** Architecture Team  
**Next Review:** 2026-09-20
