# ADR-0004: Event Broker Selection - Kafka Over RabbitMQ

## Status
Accepted

## Context
The event-driven architecture requires a reliable message broker for asynchronous communication between microservices. The broker must:
- **Durability**: Retain messages for extended periods (minimum 7 years for audit compliance)
- **Replay**: Support replaying events for disaster recovery and analytics pipeline rebuilding
- **Scalability**: Handle thousands of messages per second as inventory operations scale
- **Operational Overhead**: Minimal operational burden for a small team
- **Analytics Integration**: Efficiently export events to data lake for ML pipelines
- **Consumer Independence**: Allow multiple independent consumers to process same events at different rates

Both RabbitMQ and Kafka are viable options, but have different characteristics affecting long-term sustainability and AI-readiness.

## Decision
Implement **Apache Kafka as the Event Broker** for SmartStock AI.

### 1. **Kafka Configuration**
- **Broker Cluster**: Minimum 3 brokers for production (redundancy and availability)
- **Replication Factor**: 3 (each message replicated across 3 brokers)
- **Topic Strategy**: Event type-based topics (events.inventory, events.product, events.order, etc.)
- **Partitioning**: Partition by aggregate ID (product_id, order_id) for ordering guarantees within domain
- **Retention Policy**: 
  - Transient events (e.g., low-stock alerts): 30 days
  - Operational events (inventory movements, orders): 7 years
  - Audit events (user actions, security): 10 years
  - Analytical events: 2 years (older data archived to object storage)

### 2. **Topic Structure**
```
events.inventory (partitions: 10, replication: 3)
events.product (partitions: 10, replication: 3)
events.order (partitions: 10, replication: 3)
events.warehouse (partitions: 10, replication: 3)
events.supplier (partitions: 10, replication: 3)
events.customer (partitions: 10, replication: 3)
events.audit (partitions: 5, replication: 3) -- High durability, lower throughput
```

### 3. **Consumer Groups**
Each service subscribes as independent consumer groups:
- **Inventory Service Consumer**: Subscribes to events.order, events.product, events.warehouse to sync local state
- **Reporting Service Consumer**: Subscribes to all events for analytics tables
- **Notification Service Consumer**: Subscribes to inventory, order events to trigger alerts
- **Audit Service Consumer**: Subscribes to all events for immutable log storage

Services start consuming from latest offset; can replay from beginning if needed.

### 4. **Key Advantages for SmartStock AI**

**Durability & Replay**: Messages stored in Kafka logs for extended periods
- Enable disaster recovery: rebuild Reporting Service by replaying events
- Enable data recovery: reprocess events if bugs found in analytics
- Support AI training: export historical events for model training

**Scalability**: Kafka designed for high-throughput, distributed scenarios
- Easily handle thousands of inventory transactions per second
- Partition across multiple brokers for horizontal scaling
- Each partition processed independently (parallelism)

**Ordering Guarantees**: Partition ordering guarantees
- Events for same product_id always processed in order
- Prevents inventory quantity becoming negative
- Maintains causal ordering for audit compliance

**Decoupling**: Producers and consumers completely independent
- Services publish events without knowing consumers
- New consumers can subscribe without modifying producers
- Operational resilience: consumer lag doesn't block producers

**Data Lake Integration**: Kafka Connect ecosystem
- Stream events directly to S3/MinIO object storage in Parquet format
- Connector-based architecture for flexible export options
- No custom code for data pipeline

## Alternatives Considered

### Option 1: RabbitMQ
Message broker focused on reliability and complex routing

**Pros:**
- Lighter weight than Kafka
- Lower memory footprint
- Excellent for traditional message queues
- Simpler cluster setup
- Good monitoring/management UI

**Cons:**
- Not designed for long-term message retention (7-year compliance requirement)
- Message ordering only guaranteed within single queue (not ideal for event sourcing)
- Complex to replay events; must design custom replay mechanism
- Not optimized for high-throughput streaming scenarios
- Limited support for event time semantics (critical for time-series analytics)
- Difficult to integrate with data lake for ML pipelines
- Consumer groups less sophisticated (no offset management)
- Rebalancing more disruptive to operations

### Option 2: Cloud-Managed Services (AWS SNS/SQS, GCP Pub/Sub, Azure Service Bus)
Fully managed event services by cloud providers

**Pros:**
- No operational burden
- Automatic scaling
- Built-in monitoring and alerting
- Excellent availability

**Cons:**
- Vendor lock-in (cannot migrate without rewriting event handling)
- Higher costs at scale (thousands of messages per day)
- Limited local development/testing capability
- Compliance concerns (data privacy regulations)
- Not suitable for on-premise deployments
- Difficult to manage event retention policies
- Less flexibility for advanced event processing

### Option 3: Apache Pulsar
Cloud-native event streaming platform

**Pros:**
- Similar features to Kafka
- Better geo-replication
- Excellent for multi-datacenter deployments
- Lightweight broker tier

**Cons:**
- Smaller ecosystem compared to Kafka
- Fewer managed services available
- Less operational expertise available in market
- Kafka maturity and adoption significantly higher
- Similar operational complexity to Kafka without Kafka's community

## Consequences

### Positive
- **7-Year Compliance Ready**: Retains events for audit and regulatory requirements
- **Disaster Recovery**: Replay events to rebuild any service state from scratch
- **Analytics Pipeline**: Seamlessly export events to data lake for ML training
- **Scalability**: Handles millions of inventory transactions as platform grows
- **Operational Resilience**: Partition-based processing ensures failures don't cascade
- **Consumer Independence**: Reporting Service can lag behind operational services
- **Ordering Guarantees**: Same-product events always processed in order (prevents data corruption)
- **Stream Processing**: Kafka Streams library enables complex event processing
- **Community & Ecosystem**: Largest community, most operational expertise available
- **Technology Stack Alignment**: Kafka integrates with Spark, Flink, Beam for future AI/analytics
- **Cost Effectiveness**: Open source, no per-message costs

### Negative
- **Operational Complexity**: Requires managing Kafka cluster (ZooKeeper or KRaft mode)
- **Memory Footprint**: Kafka brokers require more resources than RabbitMQ
- **Infrastructure Cost**: Need dedicated cluster (3+ brokers for production)
- **Network Overhead**: Message payload sent to all brokers (vs. RabbitMQ routing)
- **Storage Requirements**: Long retention policies require significant disk space
- **Learning Curve**: Partition management, consumer group coordination more complex
- **JVM Dependency**: Kafka runs on JVM; requires JVM maintenance and monitoring
- **Monitoring Complexity**: Requires understanding of partition rebalancing, lag, offset management

### Trade-offs
- **Simplicity vs. Compliance**: Accept operational complexity for 7-year retention
- **Infrastructure Costs vs. Data Integrity**: Invest in cluster infrastructure to guarantee event delivery
- **Memory Usage vs. Performance**: Accept higher memory footprint for throughput and reliability
- **Team Expertise vs. Capabilities**: Require team learning curve for advanced Kafka features

## Future Considerations

1. **Kafka Streams**: Process events in real-time for complex business logic
   - Example: Real-time inventory balance calculation when stock movements occur
   - Example: Real-time low-stock alerts as threshold is crossed

2. **Schema Registry**: Implement Confluent Schema Registry for event schema versioning
   - Supports backward/forward compatibility
   - Enables safe event schema evolution

3. **Change Data Capture (CDC)**: Use Kafka Connect to capture database changes
   - Automatically stream operational database changes to data lake
   - Eliminate manual event publishing for certain operations

4. **Event Time Processing**: Implement event time vs. processing time semantics
   - Critical for accurate time-series analytics (demand patterns, seasonality)
   - Supports out-of-order event handling (delayed messages from unreliable networks)

5. **Multi-Datacenter Replication**: Kafka MirrorMaker for geo-redundancy
   - Replicate events across datacenters for disaster recovery
   - Support global deployment as business expands internationally

6. **Performance Optimization**: Batch events into larger messages
   - Trade latency for throughput on non-critical paths
   - Reduce broker load during peak inventory operations

7. **Access Control**: Implement Kafka ACLs for fine-grained access control
   - Restrict which services can produce/consume which topics
   - Support compliance requirements (data privacy, segregation of duties)

8. **Monitoring & Alerting**: Integrate Prometheus/Grafana for:
   - Consumer lag monitoring
   - Broker health monitoring
   - Event throughput and latency metrics

## Implementation Guidance

- Kafka deployed via Docker/Kubernetes with persistent volumes
- Implement exponential backoff with jitter for failed message processing
- Use consumer group offsets to track consumption progress; never discard offsets
- Dead letter topics for failed event processing; monitor actively
- Document event schema for each topic in ADR (ADR-0005)
- Implement idempotent event handlers (safe to process same event twice)
- Use correlation IDs in events for end-to-end tracing
- Archive old events to object storage after retention period expires
- Services implement error handling: retry transient failures, move to DLQ for permanent failures
