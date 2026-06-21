# ADR-0003: Database Strategy - PostgreSQL Per Service with Eventual Consistency

## Status
Accepted

## Context
The traditional approach of sharing a single database across microservices creates:
- **Tight Coupling**: Service deployments blocked by database migrations
- **Scaling Bottlenecks**: Single database cannot scale with individual service load
- **Failure Domains**: One service's database issues affect all services
- **Data Privacy**: Services cannot restrict data access to their domains
- **Conflicting Requirements**: Services may need different database optimization strategies

SmartStock AI must support:
- Multi-warehouse inventory operations at scale (potentially thousands of warehouses)
- Independent scalability of product catalog, inventory tracking, orders, and analytics
- High-quality audit logs for compliance and regulatory requirements
- Data consistency without distributed transactions
- Future data export capabilities for AI/ML platforms

The challenge is implementing eventual consistency patterns that maintain data correctness without violating domain boundaries.

## Decision
Implement **PostgreSQL Per Service** architecture with eventual consistency:

### 1. **Database Ownership**
Each microservice owns its PostgreSQL database:
- **Identity Service**: user, role, permission tables
- **Product Service**: product, category, pricing tables
- **Inventory Service**: stock_level, stock_transaction, adjustment tables
- **Warehouse Service**: warehouse, zone, shelf, bin tables
- **Supplier Service**: supplier, supplier_contact, delivery_performance tables
- **Customer Service**: customer, customer_contact, credit_profile tables
- **Order Service**: purchase_order, sales_order, order_line tables
- **Audit Service**: audit_log, immutable event store
- **Reporting Service**: aggregated_inventory, warehouse_utilization, kpi_snapshots tables
- **Analytics Service**: analytical views and denormalized tables for dashboards

No service accesses another service's database directly.

### 2. **Data Synchronization**
Services synchronize data via events, not database replication:
- When a business event occurs (StockIn, ProductCreated), an event is published
- Consumer services subscribe to relevant events
- Services update their local tables based on events they receive
- Temporary inconsistency is acceptable; eventual consistency is the goal

### 3. **Foreign Key Strategy**
Services store references to entities from other services as **denormalized IDs**, not foreign keys:
```sql
-- Inventory Service local table
CREATE TABLE stock_level (
  id UUID PRIMARY KEY,
  product_id UUID NOT NULL,  -- Reference to Product Service, not FK
  warehouse_id UUID NOT NULL,  -- Reference to Warehouse Service, not FK
  quantity_on_hand INT NOT NULL,
  quantity_reserved INT NOT NULL,
  last_updated TIMESTAMP NOT NULL
);

-- NO foreign key constraints to other services!
-- Referential integrity checked at application layer via REST calls
```

### 4. **Audit and Immutability**
The Audit Service maintains the immutable event store:
```sql
CREATE TABLE event_log (
  event_id UUID PRIMARY KEY,
  event_type VARCHAR(100) NOT NULL,
  aggregate_id UUID NOT NULL,
  aggregate_type VARCHAR(50) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  published_at TIMESTAMP NOT NULL,
  user_id UUID,
  correlation_id UUID NOT NULL,
  payload JSONB NOT NULL,
  metadata JSONB NOT NULL,
  version INT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW(),
  
  CONSTRAINT event_immutable CHECK (true)  -- Events never updated/deleted
);

CREATE INDEX idx_event_type ON event_log(event_type);
CREATE INDEX idx_aggregate_id ON event_log(aggregate_id);
CREATE INDEX idx_occurred_at ON event_log(occurred_at);
```

### 5. **Consistency Patterns**
Services handle eventual consistency via:

**Saga Pattern**: Multi-service workflows managed via choreography
- Example: Order fulfillment spans Inventory → Warehouse → Supplier services
- Each service publishes events that trigger next step
- Compensating transactions handle rollback (e.g., release inventory if order fails)

**Read Replicas**: Reporting Service subscribes to all events and maintains denormalized views
- Reporting Service never blocks operational services
- Dashboards show eventually consistent snapshots
- Queries optimized for read performance

## Alternatives Considered

### Option 1: Shared Database with Schema Isolation
Single PostgreSQL instance, separate schemas per service

**Pros:**
- Simpler operations (one database)
- Easier backup/recovery
- Simpler backup and recovery
- Easier to debug relationships

**Cons:**
- Database still a bottleneck for scaling
- Shared database credentials and access control
- Schema migrations block all services
- Single database failure affects all services
- Difficult to optimize for service-specific workloads
- Violates microservices principle of independence
- Cannot isolate security and compliance requirements

### Option 2: NoSQL (MongoDB, DynamoDB, Cassandra) Per Service
Each service uses appropriate NoSQL database

**Pros:**
- Excellent horizontal scalability
- Flexible schema evolution
- Good for time-series inventory data

**Cons:**
- Adds operational complexity (managing multiple database types)
- Cassandra requires experienced operators
- DynamoDB increases cloud costs
- Reduces team expertise (must learn multiple databases)
- Not ideal for transactional consistency
- PostgreSQL sufficient for current requirements

### Option 3: Shared Data Warehouse (Single PostgreSQL as Data Lake)
All services write to single data warehouse

**Pros:**
- Simple data exports for analytics
- No replication needed
- Analytics always consistent

**Cons:**
- Complete antithesis of microservices
- Single point of failure
- Operational services compete for database resources with analytics queries
- Cannot be implemented without violating service boundaries

## Consequences

### Positive
- **Service Independence**: Each service can scale, deploy, and optimize independently
- **Failure Isolation**: Database failure in one service doesn't affect others
- **Technology Flexibility**: Each service can optimize database configuration for its workload
- **Clear Data Ownership**: No ambiguity about which service owns which data
- **Deployment Freedom**: Services deploy without coordinating database migrations
- **Security Isolation**: Services can implement role-based access control independently
- **Scalability**: Inventory Service database scales for stock tracking; Reporting Service scales separately for analytics
- **Compliance**: Audit Service maintains immutable, tamper-proof event log

### Negative
- **Operational Complexity**: Must manage multiple PostgreSQL instances
- **Eventual Consistency**: Services experience temporary inconsistency
- **Event Processing Overhead**: All data synchronization via events (slower than direct DB access)
- **Distributed Transaction Challenges**: Multi-service operations require saga pattern implementation
- **Query Complexity**: Reporting requires aggregating data from multiple databases
- **Infrastructure Costs**: Multiple database instances increase operational costs
- **Data Duplication**: Services maintain local copies of data from other services
- **Debugging Difficulty**: Inconsistency issues harder to diagnose

### Trade-offs
- **Consistency vs. Availability**: Choose eventual consistency for operational resilience
- **Operational Simplicity vs. Independence**: Accept multiple databases for service autonomy
- **Query Performance vs. Data Isolation**: Accept joins across services via application layer
- **Infrastructure Costs vs. Scalability**: Invest in multiple databases for independent scaling

## Future Considerations

1. **Data Warehouse Strategy**: For Phase 2 (Analytics), implement separate data warehouse (PostgreSQL or Snowflake) that consumes events and maintains denormalized analytical tables

2. **Event Sourcing**: For services with complex state (Inventory, Order), consider event sourcing where database is purely audit log

3. **Temporal Queries**: Archive historical snapshots to support "what was the inventory on date X" queries essential for AI/analytics

4. **Cross-Service Queries**: As complexity grows, implement application-level query federation (e.g., GraphQL gateway) to simplify cross-service queries

5. **Sharding Strategy**: Inventory Service may eventually shard by warehouse_id or product_id for extreme scale

6. **Read Replicas**: Services maintain read replicas for reporting queries without blocking transactional workload

7. **Disaster Recovery**: Define RTO/RPO for each service; implement backup strategy considering event log as source of truth

8. **Data Privacy**: Implement encryption at rest and in transit; carefully control which services can read events from other services

9. **Stream Processing**: Real-time event processing via Kafka Streams for immediate consistency where needed

## Implementation Guidance

- Each service repository includes Flyway migrations for its own database schema
- No shared SQL libraries; each service owns its data access logic
- Application layer responsible for maintaining eventual consistency, not database constraints
- All inter-service data references use IDs, not database foreign keys
- Event handlers must be idempotent (safe to process events multiple times)
- Reconciliation jobs run periodically to detect and fix consistency issues
- Reporting Service subscribes to all events and rebuilds analytical tables as needed
- Audit Service event log is write-once; never update or delete events
- Services document which events they publish and which they consume
