# ADR-0005-Event-Driven-Architecture-Strategy

## Status
Accepted

## Context

SmartStock AI is designed as an enterprise inventory platform requiring:
- Real-time inventory tracking across multiple warehouses
- Complete audit trail for compliance (7-10 year retention)
- High-quality structured data generation for future AI models
- Asynchronous communication between independent microservices
- Replayable event history for system recovery and reconstruction

The system must support 12+ independent microservices communicating reliably without direct database coupling.

## Decision

Adopt a **comprehensive event-driven architecture** using:

1. **Event-First Design Pattern**
   - Every business action MUST emit an immutable, versioned event
   - Events are the source of truth for system state
   - Services derive state from events, not from other service databases

2. **Transactional Outbox Pattern for Publishing**
   - Write entity + event to database in single transaction
   - Asynchronous worker publishes events from outbox table
   - Guarantees no events lost in crashes
   - Guarantees exactly-once semantics

3. **Event Message Broker**
   - Use Kafka for long-term event stream (7 years retention)
   - RabbitMQ acceptable for short-term cache invalidation
   - All events must be timestamped, versioned, and idempotent-consumable

4. **Event Metadata Standard**
   Every event must include:
   - eventId (UUID - unique identifier)
   - eventType (string - immutable)
   - eventVersion (semantic - for evolution)
   - timestamp (ISO8601 - when it happened)
   - correlationId (for request tracing)
   - requestId (for debugging)
   - serviceName (which service published it)
   - userId (who triggered it, if applicable)
   - tenantId (for multi-tenancy support)

5. **Event Lifecycle Management**
   - **Publishing**: Max 100ms latency from action to event
   - **Consumption**: Max 500ms processing latency
   - **Ordering**: Partition by entity ID (product, order, etc.)
   - **Consumer Lag**: Must remain < 5 minutes
   - **Dead Letter Queue**: Non-retriable failures go to DLQ

6. **Semantic Versioning for Events**
   - MAJOR.MINOR (e.g., 1.0, 2.0)
   - MAJOR: Breaking changes (new required fields, type changes)
   - MINOR: Non-breaking (new optional fields)
   - Services must support current and 1 previous MAJOR version

7. **Retention Strategy**
   - 10 years: Regulatory audit events
   - 7 years: Transactions, compliance events (StockIn/Out, Orders, Suppliers)
   - 3 years: Analytics aggregations, operational snapshots
   - 1 year: Notification, session, short-term logs

## Alternatives Considered

### Alternative 1: Direct REST/RPC Calls
- **Rejected**: Creates tight coupling, single points of failure, no audit trail
- Would require synchronous blocking between services
- No replay capability for recovery

### Alternative 2: Database Replication
- **Rejected**: Violates microservice principle of data isolation
- Creates schema coupling
- Compliance nightmare (duplicate sensitive data)

### Alternative 3: Event Sourcing Only (No Current State)
- **Rejected**: Too complex for Phase 1
- Requires mature event store implementation
- Can be adopted incrementally in Phase 2

### Alternative 4: Simple Message Queue (No Outbox)
- **Rejected**: Vulnerable to message loss on crashes
- Non-deterministic ordering
- No exactly-once semantics

## Consequences

### Positive Impacts
1. **Reliability**: Exactly-once semantics prevents data loss
2. **Auditability**: Complete immutable record of all actions
3. **Compliance**: 7-10 year retention support for regulations
4. **Scalability**: Decoupled services scale independently
5. **Resilience**: Can replay events to recover state
6. **AI-Ready**: High-quality structured data for ML models
7. **Debugging**: Correlation IDs enable tracing issues across services
8. **Testing**: Can replay events in test scenarios
9. **Analytics**: Natural data generation for reporting and BI

### Negative Impacts & Trade-offs
1. **Complexity**: Event sourcing is harder to reason about than CRUD
2. **Storage**: 7-10 year retention requires significant storage (petabyte scale)
3. **Latency**: Asynchronous processing adds 100ms+ to operations
4. **Eventual Consistency**: Consumers see stale data initially
5. **Debugging**: Harder to trace issues in asynchronous flow
6. **Infrastructure**: Requires Kafka/RabbitMQ expertise
7. **Testing**: Must mock event broker for unit tests

### Mitigation Strategies
- Use clear naming conventions for events (past tense: ProductCreated, StockMoved)
- Implement event versioning from day 1 (no ad-hoc changes)
- Build monitoring dashboard for event lag and DLQ depth
- Establish clear SLAs for event processing (p99 latencies)
- Document event schemas in machine-readable format (JSON Schema)
- Create comprehensive event catalog (this document)
- Use correlation IDs in all logs
- Implement dead letter queue handling

## Future Considerations

### Phase 2 - Advanced Event Patterns
- **Event Sourcing as Storage**: Replace current state with event replay
- **CQRS Pattern**: Separate read models from write events
- **Saga Pattern**: Multi-service transactions via choreography
- **Event Upcasting**: Handle backward compatibility for ancient event versions

### Phase 3 - AI Integration
- **Feature Store**: Aggregate events into ML features
- **Training Data Pipeline**: Export events to data lake for model training
- **Prediction Events**: New event type: AIRecommendation, DemandForecast
- **Feedback Loop**: Model predictions generate new events
- **Anomaly Detection**: Detect unusual patterns in event streams

### Data Lake Integration
- Export events daily to S3/MinIO in Parquet format
- Create analytical tables from event aggregations
- Build time-series database for inventory snapshots
- Support data exploration tools (Spark, DuckDB, etc.)

### Scalability Considerations
- Kafka should partition by entity ID (product_id, order_id)
- Consumer groups for different service needs (real-time vs batch)
- Retention policies per topic based on business requirements
- Schema registry for centralized event definition management

### Operational Excellence
- Implement prometheus metrics for event lag
- Set up alerts for dead letter queue depth
- Build dashboards showing event flow per service
- Create runbooks for handling event processing failures
- Establish event schema review process

## Implementation Checklist

- [ ] Design event schema registry
- [ ] Implement transactional outbox table in all services
- [ ] Configure Kafka broker clusters
- [ ] Build event publisher framework (Spring events)
- [ ] Build event consumer framework with idempotence
- [ ] Create monitoring and alerting
- [ ] Document all events in event catalog
- [ ] Build dead letter queue handler
- [ ] Implement event versioning strategy
- [ ] Create testing utilities for event replay

