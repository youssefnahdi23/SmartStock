# ADR-0012: Domain-Driven Design - Bounded Contexts and Microservice Boundaries

## Status
Accepted

## Context
Microservices architecture is only effective if service boundaries align with business domains. Poorly defined boundaries lead to:
- **Chatty Services**: Services constantly calling each other (defeats microservices benefit)
- **Shared Domains**: Multiple services responsible for same business concept (consistency problems)
- **Data Inconsistency**: Same data owned by multiple services
- **Team Confusion**: Unclear which team owns which feature
- **Future Scaling Issues**: Cannot scale teams independently

Domain-Driven Design (DDD) provides a framework for identifying natural business boundaries (bounded contexts) that become microservice boundaries.

SmartStock AI serves inventory management across multiple domains:
- Identity & Access Management
- Product Lifecycle Management
- Physical Inventory Tracking
- Warehouse Operations
- Supplier Relationships
- Customer Orders
- Purchase Workflows
- Audit & Compliance
- Notifications
- Business Reporting
- Data Analytics
- System Configuration

Each represents a distinct bounded context with its own ubiquitous language, business rules, and data model.

## Decision
Implement **Domain-Driven Design with 12 Primary Bounded Contexts**, each represented by a microservice:

### 1. **Bounded Contexts and Microservices**

**Context 1: Identity & Access Management**
- Service: Identity Service
- Responsibilities:
  - User authentication (login, logout, token management)
  - User management (create, update, deactivate users)
  - Role and permission management
  - Multi-factor authentication
  - Password policies and reset
- Ubiquitous Language: User, Role, Permission, Token, Credential
- Data: User profiles, passwords (hashed), roles, permissions

**Context 2: Product Catalog**
- Service: Product Service
- Responsibilities:
  - Product information management
  - SKU generation and management
  - Barcode/QR code generation
  - Product categorization
  - Pricing management
  - Import/export functionality
- Ubiquitous Language: Product, SKU, Category, Price, Barcode, Variant
- Data: Product master data, categories, pricing

**Context 3: Physical Inventory**
- Service: Inventory Service
- Responsibilities:
  - Real-time stock tracking (quantity on hand, reserved, damaged)
  - Stock movements (in, out, adjustments, transfers)
  - Quantity validation and constraints
  - Historical tracking (audit trail)
  - Low stock alerts
  - Stock aging analysis
- Ubiquitous Language: StockLevel, Quantity, Reserve, Adjust, Movement, Balance
- Data: Stock levels, transactions, adjustments, historical records

**Context 4: Warehouse Operations**
- Service: Warehouse Service
- Responsibilities:
  - Multi-warehouse management
  - Physical locations (zones, shelves, bins)
  - Capacity management and utilization
  - Location assignment strategies
  - Warehouse transfer workflows
  - Receiving and shipping processes
- Ubiquitous Language: Warehouse, Zone, Shelf, Bin, Location, Capacity, Transfer
- Data: Warehouse structure, locations, capacity, utilization

**Context 5: Supplier Management**
- Service: Supplier Service
- Responsibilities:
  - Supplier profiles and contacts
  - Supplier performance metrics (on-time delivery, quality)
  - Purchase history tracking
  - Delivery schedules and agreements
  - Supplier rating and scoring
- Ubiquitous Language: Supplier, SupplierContact, DeliveryPerformance, Rating, AgreementTerm
- Data: Supplier information, contacts, performance data

**Context 6: Customer Management**
- Service: Customer Service
- Responsibilities:
  - Customer profiles and contacts
  - Customer segmentation
  - Credit profiles and limits
  - Purchase history
  - Customer preferences
- Ubiquitous Language: Customer, CustomerSegment, CreditProfile, CustomerPreference
- Data: Customer information, credit data, preferences

**Context 7: Purchase Order Management**
- Service: Purchase Order Service
- Responsibilities:
  - Purchase order creation and management
  - PO line item management
  - Delivery tracking
  - Invoice reconciliation
  - Payment status tracking
  - Return management
- Ubiquitous Language: PurchaseOrder, POLineItem, Delivery, Invoice, Payment
- Data: Purchase orders, line items, delivery status, invoices

**Context 8: Sales Order Management**
- Service: Sales Order Service
- Responsibilities:
  - Sales order creation and management
  - Order line items
  - Fulfillment status tracking
  - Shipping management
  - Return processing
  - Sales order reporting
- Ubiquitous Language: SalesOrder, OrderLineItem, Fulfillment, Shipping, Return
- Data: Sales orders, line items, fulfillment status, shipping information

**Context 9: Audit & Compliance**
- Service: Audit Service
- Responsibilities:
  - Immutable event log (append-only)
  - Compliance audit trails
  - Data access tracking
  - Changes audit (who changed what, when)
  - Compliance reporting
  - Data retention policies enforcement
- Ubiquitous Language: AuditLog, AuditEvent, ComplianceReport, DataRetention, Immutable
- Data: Event logs (immutable), audit records, compliance documentation

**Context 10: Notifications & Alerts**
- Service: Notification Service
- Responsibilities:
  - Low stock alerts
  - Order status notifications
  - System alerts
  - Email delivery
  - SMS delivery (future)
  - Notification templates
  - Subscription management
- Ubiquitous Language: Notification, Alert, Template, Subscription, Channel
- Data: Notification templates, subscriptions, delivery status

**Context 11: Business Reporting & Analytics**
- Service: Reporting Service
- Responsibilities:
  - Inventory reports (aged inventory, stock levels)
  - Warehouse utilization reports
  - Sales and purchase reports
  - Supplier performance analytics
  - Customer analysis
  - KPI calculations and dashboards
- Ubiquitous Language: Report, Dashboard, KPI, Metric, Aggregation, Snapshot
- Data: Aggregated inventory snapshots, warehouse utilization, KPIs

**Context 12: Data Export & Analytics**
- Service: Data Export Service
- Responsibilities:
  - Export operational data to data lake
  - Event streaming to object storage
  - Data format transformation (Parquet, CSV, JSON)
  - Schema management
  - Data quality monitoring
  - Compliance data exports
- Ubiquitous Language: Export, DataLake, Parquet, Schema, Transformation, DataQuality
- Data: Event logs, snapshots, aggregations in structured formats

### 2. **Inter-Service Communication Map**

**Synchronous Dependencies** (REST calls, minimized)
- Product Service → Identity Service (validate user permissions)
- Inventory Service → Product Service (validate product exists)
- Warehouse Service → Identity Service (validate user permissions)
- Reporting Service → Inventory Service (read-only queries, cacheable)
- Data Export Service → Audit Service (read event logs)

**Asynchronous Dependencies** (Events, preferred)
```
Product Service publishes:
  - ProductCreated → Inventory Service, Warehouse Service
  - ProductUpdated → Inventory Service, Reporting Service
  - ProductDeleted → Inventory Service, Warehouse Service

Inventory Service publishes:
  - StockIn → Reporting Service, Notification Service, Audit Service
  - StockOut → Reporting Service, Notification Service, Audit Service
  - InventoryAdjusted → Reporting Service, Audit Service

Purchase Order Service publishes:
  - PurchaseOrderCreated → Inventory Service, Supplier Service, Audit Service
  - PurchaseOrderReceived → Inventory Service, Reporting Service

Sales Order Service publishes:
  - SalesOrderCreated → Inventory Service (reserve stock), Reporting Service
  - SalesOrderFulfilled → Inventory Service (deduct stock), Reporting Service

Warehouse Service publishes:
  - StockTransferred → Inventory Service, Reporting Service, Audit Service

Notification Service consumes:
  - StockIn, StockOut from Inventory Service
  - OrderCreated from Sales/Purchase Order Services
```

### 3. **Ubiquitous Language Definition**

Each bounded context defines its vocabulary to prevent confusion:

**Inventory Service Ubiquitous Language**
- Stock Level: Current inventory quantity available
- Reserve: Allocate stock for future fulfillment
- On-Hand: Physically present inventory
- Damaged: Inventory beyond use (waste)
- Adjust: Manual correction of inventory
- Movement: Event of stock change
- Transaction: Immutable record of movement

**Warehouse Service Ubiquitous Language**
- Zone: Logical area within warehouse (e.g., "High Value", "Hazmat")
- Shelf: Physical shelving unit
- Bin: Individual storage location on shelf
- Location: Unique identifier of zone/shelf/bin
- Putaway: Process of placing received goods
- Pick: Process of removing goods for fulfillment
- Capacity: Maximum items storable at location

### 4. **Service Autonomy Principles**

Each bounded context/service:
- **Owns its database**: No shared tables
- **Owns its API contracts**: No consumers depend on internal details
- **Owns its business logic**: Complex rules stay within service
- **Owns its events**: Publishes events for external systems to consume
- **Minimal dependencies**: Limited synchronous calls to other services
- **Independent deployment**: Can deploy without coordinating other services
- **Team ownership**: Clear team responsible for service

### 5. **Data Consistency Across Contexts**

**Shared Concepts** (same concept referenced from multiple services)
- Product (referenced from Inventory, Warehouse, PO, SO)
- Warehouse (referenced from Inventory, Warehouse, PO SO)
- User (referenced from all services)
- Supplier (referenced from PO, Reporting)
- Customer (referenced from SO, Reporting)

**Consistency Approach**
Each service:
1. Stores the ID of shared concepts (not entire object)
2. Subscribes to events from owning service
3. Updates local denormalized cache when event received
4. Temporary inconsistency acceptable (eventual consistency)
5. Validation done at API level (not DB level)

### 6. **Anti-Corruption Layer**

If integrating with external systems, use anti-corruption layer:
```
External Supplier System
        ↓
Anti-Corruption Layer
(translate supplier format to SmartStock format)
        ↓
Supplier Service
```

## Alternatives Considered

### Option 1: Technical Boundaries (Not Business Domains)
Services organized by technical layer (UserService, ProductService, InventoryService)

**Pros:**
- Simpler initial organization

**Cons:**
- High coupling between services
- Cannot scale teams independently
- Difficult to change business logic
- Not aligned with business
- Violates DDD principles

### Option 2: Fewer, Larger Contexts
Combine multiple domains into fewer services (e.g., Orders service includes both PO and SO)

**Pros:**
- Fewer services to manage
- Simpler inter-service communication

**Cons:**
- Services become too large
- Cannot scale independently
- Team cannot scale independently
- Violates single responsibility

### Option 3: More, Smaller Contexts
Over-decompose into many micro-services

**Pros:**
- Maximum independence

**Cons:**
- Operational complexity
- Distributed system complexity
- Difficult debugging
- Over-engineering for current phase

## Consequences

### Positive
- **Business Alignment**: Services align with business domains; stakeholders understand ownership
- **Team Scaling**: Each team owns bounded context; teams scale independently
- **Clear Contracts**: Service boundaries clear; no ambiguity about responsibilities
- **Language Clarity**: Ubiquitous language prevents misunderstandings
- **Independent Deployment**: Services deployable independently
- **Consistent Logic**: Business rules for domain consolidated in one service
- **Easy Testing**: Clear boundaries make testing easier
- **Future Extensibility**: New features clearly belong to specific service

### Negative
- **Complex Coordination**: Multi-service workflows require careful orchestration
- **Distributed Transactions**: ACID transactions not possible across services
- **Data Consistency Delay**: Eventual consistency requires handling delays
- **Team Communication**: Teams must coordinate via service contracts
- **Debugging Difficulty**: Issues spanning services harder to debug

### Trade-offs
- **Simplicity vs. Business Alignment**: Accept complexity for alignment with business
- **Coordination vs. Independence**: Accept cross-service coordination for domain autonomy

## Future Considerations

1. **Subdomain Identification**: Within large contexts, identify subdomains
   - Core Domain: Inventory (most important for competitive advantage)
   - Supporting Domains: Warehouse (necessary but generic)
   - Generic Domains: User management (off-the-shelf solutions exist)

2. **Context Mapping**: Document relationships between contexts
   - Published Language: Contexts publish interfaces for others
   - Shared Kernel: Contexts share common library code (minimal)
   - Customer-Supplier: One context customer of another's services

3. **Event Storming**: Facilitated workshops to identify contexts
   - Business event discovery
   - Command identification
   - Aggregate boundary identification

4. **Domain Events**: Rich events that capture business semantics
   - Move beyond technical events (StockIn) to business events (InventoryReplenished)

## Implementation Guidance

- Each bounded context has dedicated service repository
- Ubiquitous language documented in service README
- Service API documented with Clear business semantics
- Event contracts documented: what events published, consumed
- Cross-service calls minimized; async preferred
- Services validate business rules; not DB constraints
- Data duplication accepted where justified by performance/autonomy
