# ADR-0002: Service Communication Model - REST and Event-Driven Architecture

## Status
Accepted

## Context
In a microservices architecture, services must communicate to accomplish business processes. SmartStock AI requires:
- **Synchronous Communication**: For operations requiring immediate response (e.g., user interface requests)
- **Asynchronous Communication**: For eventual consistency operations and event propagation to analytics/AI layers
- **Data Consistency**: Eventual consistency across service boundaries while maintaining audit trail
- **Scalability**: Decoupled services that can operate independently at varying throughput levels
- **AI Readiness**: All business events must be captured in a durable, replayable format for future analytics

The choice between REST, gRPC, message queues, and event streams impacts operational complexity, latency, consistency models, and data collection quality for AI training.

## Decision
Implement a **Hybrid Communication Model**:

### 1. **Synchronous Communication: REST APIs**
- Used for request-response patterns
- Implemented with Spring Boot controllers
- Protected by JWT authentication at API Gateway
- Examples: Querying products, checking inventory, user authentication
- Response time target: <200ms per request

### 2. **Asynchronous Communication: Event-Driven Architecture**
- Used for business events that trigger workflows across services
- Published to event broker (RabbitMQ or Kafka)
- Events are immutable, append-only, and permanently retained
- Examples: StockIn, StockOut, ProductCreated, OrderPlaced, SupplierUpdated

### 3. **Event Format (Standardized)**
All domain events follow this contract:
```json
{
  "eventId": "UUID",
  "eventType": "StockIn|StockOut|StockMoved|ProductCreated|OrderPlaced|...",
  "aggregateId": "product-123",
  "aggregateType": "Product|Inventory|Order",
  "timestamp": "ISO-8601",
  "version": 1,
  "userId": "user-456",
  "correlationId": "request-789",
  "payload": {
    "productId": "P001",
    "warehouseId": "W01",
    "quantity": 50,
    "unitCost": 12.5,
    "supplierId": "S99",
    "reason": "Purchase Order PO-2026-001"
  },
  "metadata": {
    "source": "inventory-service",
    "environment": "production"
  }
}
```

### 4. **Event Durability & Replay**
- Events stored in event log (append-only)
- Configurable retention policy (minimum: 7 years for audit compliance)
- Events replayable for disaster recovery and analytics pipeline rebuilding
- Dead letter queues for failed message processing

## Alternatives Considered

### Option 1: REST-Only Synchronous Communication
All service-to-service communication via HTTP REST

**Pros:**
- Simple to understand and implement
- Low operational overhead
- Well-understood technology

**Cons:**
- Tight coupling between services
- Creates distributed transaction problems
- Cascading failures if a dependent service is down
- No event history for AI/analytics
- Difficult to implement eventual consistency
- Cannot replay operations for data recovery
- Lost business events for analytics training data

### Option 2: Event Sourcing Only (No REST)
All communication via events, no synchronous calls

**Pros:**
- Complete event history
- Perfect for analytics and AI
- Highly decoupled

**Cons:**
- Difficult to implement request-response patterns needed by UI
- High latency for user-facing operations
- Complex eventual consistency handling
- Requires sophisticated CQRS implementation
- Operational complexity for queries

### Option 3: gRPC for Service-to-Service, Events for Cross-Domain
gRPC for performance, events for decoupling

**Pros:**
- Better performance than REST
- Type-safe communication
- Good for service mesh integration

**Cons:**
- Requires language support across services
- Not ideal for public APIs (no browser support)
- Additional operational complexity
- Steeper learning curve

## Consequences

### Positive
- **Decoupling**: Services remain independent; REST synchronous calls are minimized
- **Audit Trail**: Every business event recorded with full context (userId, timestamp, correlationId)
- **AI/Analytics Ready**: Events provide high-quality training data with full traceability
- **Replay Capability**: Operational events replayable for disaster recovery and debugging
- **Eventual Consistency**: Services can operate independently and converge asynchronously
- **Scalability**: Event broker decouples producer and consumer scaling
- **Fault Isolation**: Service failures do not cascade; failed events go to dead letter queues
- **Business Visibility**: Complete history of all inventory transactions available
- **Compliance**: Immutable audit logs support regulatory requirements

### Negative
- **Operational Complexity**: Must run and maintain event broker infrastructure
- **Synchronous Latency**: REST calls between services add network overhead to UI responses
- **Eventual Consistency Complexity**: Developers must handle temporary inconsistency states
- **Debugging Difficulty**: Distributed tracing and correlation IDs essential for troubleshooting
- **Event Versioning Overhead**: Must carefully manage event schema evolution
- **Storage Overhead**: Long-term event retention requires significant database/storage capacity
- **Data Privacy**: Event logs may contain sensitive data requiring encryption and access controls

### Trade-offs
- **Consistency vs. Availability**: Choose eventual consistency for operational resilience
- **Latency vs. Decoupling**: Accept network latency to achieve service independence
- **Simplicity vs. Auditability**: Accept operational complexity for complete audit trail
- **Storage Costs**: Pay for event retention to enable analytics and recovery

## Future Considerations

1. **Event Schema Registry**: Implement schema versioning (Apache Avro or Protocol Buffers) to support backward/forward compatibility as events evolve

2. **Event Sourcing Pattern**: Consider event sourcing for services with complex state transitions (Order Service, Inventory Service)

3. **CQRS Pattern**: Implement Command Query Responsibility Segregation in services requiring complex queries (Reporting Service, Analytics Service)

4. **Temporal Event Processing**: Events with timestamps enable time-series analysis and historical analysis crucial for demand forecasting

5. **Data Lake Integration**: Events automatically exported to data lake in Parquet format for efficient analytics and machine learning

6. **Real-Time Analytics**: Event stream processing via Kafka Streams or Spark Streaming for real-time dashboard updates

7. **AI Feature Engineering**: Event logs become feature stores for machine learning models (demand patterns, supplier reliability, warehouse efficiency)

8. **Correlation Tracking**: Maintain request correlation IDs across all service calls for end-to-end request tracing and root cause analysis

9. **Service Mesh Integration**: Layer a service mesh (Istio, Linkerd) to handle circuit breaking, retries, and observability without application-level code

## Implementation Guidance

- All events must include correlationId for request tracing
- Event handlers must be idempotent (can safely process same event multiple times)
- Each service publishes events for its domain changes; never publishes events for other domains
- REST calls between services kept minimal; use events for most inter-service communication
- Event retention policies defined per event type based on business requirements
- Dead letter queues monitored actively for failed event processing
- Events immutable once published; updates done via new events, not mutations
- All payload data must be included in event (no "look up details later")
