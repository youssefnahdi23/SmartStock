# Event Catalog Index

Welcome to the SmartStock AI Event Catalog.

This directory contains the complete specification for all domain events in the SmartStock AI microservices platform.

## Documents

### 1. **event-catalog.md** (MASTER REFERENCE)
The comprehensive event catalog defining:
- All 32+ business events across 12 services
- Producer and consumer relationships
- JSON schema for each event
- Versioning strategy
- Metadata requirements
- AI usefulness classification
- Analytics usefulness classification
- Data retention recommendations

**Use this when**: You need complete event definitions with all metadata

### 2. **event-quick-reference.md** (LOOKUP GUIDE)
Quick lookup tables organized by:
- Events by service
- Events by AI usefulness level
- Events by analytics usefulness level
- Event publishing standards
- Event consumption patterns
- Performance requirements
- Retention policies

**Use this when**: You need a fast reference or to find events by category

### 3. **event-schemas.md** (TECHNICAL SPECIFICATION)
Formal JSON Schema definitions:
- Base event structure (required on all events)
- JSON schemas for critical events
- Schema registry structure
- Validation rules
- Backward compatibility rules
- Example event flow

**Use this when**: You're implementing event publishing/consumption code

### 4. **ADR-0005-Event-Driven-Architecture-Strategy.md**
Architecture Decision Record explaining:
- Why event-driven architecture was chosen
- Transactional outbox pattern for reliability
- Event metadata standards
- Lifecycle management (publish, consume, process)
- Trade-offs and mitigation strategies
- Future AI integration path

**Use this when**: You need to understand the architectural decisions

---

## Event Categories

### Inventory Events (5)
- StockIn - Critical for demand forecasting
- StockOut - Critical for sales patterns
- StockMoved - Warehouse optimization
- StockAdjusted - Data quality tracking
- LowStockThresholdReached - Replenishment trigger

### Order Events (5)
- OrderCreated - Demand signal
- OrderConfirmed - Commitment
- OrderFulfilled - Completion
- OrderShipped - Logistics
- OrderCancelled - Loss tracking

### Supplier Events (3)
- SupplierCreated - Baseline
- SupplierPerformanceUpdated - Optimization data
- SupplierDeliveryRegistered - Reliability tracking

### Product Events (3)
- ProductCreated - Catalog
- ProductUpdated - Changes
- ProductDeleted - Discontinuation

### Warehouse Events (3)
- WarehouseCreated - Location setup
- WarehouseCapacityUpdated - Space tracking
- ZoneCreated - Organization

### Analytics Events (2)
- InventorySnapshotCreated - Time-series data
- DailyAnalyticsReportGenerated - KPI aggregation

### Other Events (11)
- Customer events (2)
- Audit events (1)
- Notification events (1)
- Data Export events (2)
- Identity events (2)

---

## Critical Events for AI/Analytics

### Tier 1 - Model Training (Critical)
These events are essential for machine learning:
- StockIn (inventory input)
- StockOut (inventory output)
- OrderCreated (demand signal)
- SupplierDeliveryRegistered (supplier reliability)
- InventorySnapshotCreated (time-series state)
- DailyAnalyticsReportGenerated (aggregated metrics)

### Tier 2 - Feature Engineering (High)
These support advanced modeling:
- ProductCreated (baseline characteristics)
- SupplierPerformanceUpdated (vendor metrics)
- WarehouseCapacityUpdated (location constraints)
- LowStockThresholdReached (stock-out risk)

### Tier 3 - Context (Medium)
These provide enrichment:
- ProductUpdated (price/spec changes)
- StockMoved (location changes)
- CustomerCreated (demand patterns)
- SupplierCreated (new suppliers)

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Create transactional outbox tables in all services
- [ ] Implement event publishing framework (Spring Events + Kafka)
- [ ] Implement event consumer framework with idempotence
- [ ] Set up Kafka brokers with 7-year retention policy
- [ ] Deploy schema registry

### Phase 2: Core Events (Week 3-4)
- [ ] Publish Inventory Service events (StockIn, StockOut, etc.)
- [ ] Publish Order Service events
- [ ] Publish Supplier Service events
- [ ] Wire up consumption in dependent services
- [ ] Enable dead letter queue handling

### Phase 3: Monitoring (Week 5)
- [ ] Set up Prometheus metrics for event lag
- [ ] Create Grafana dashboards
- [ ] Implement alerting for dead letter queues
- [ ] Build event flow visualization
- [ ] Create runbooks

### Phase 4: Analytics (Week 6-8)
- [ ] Wire up Analytics Service event aggregation
- [ ] Create inventory snapshots
- [ ] Build daily reports
- [ ] Export data to data lake (MinIO)

### Phase 5: AI Integration (Future)
- [ ] Create feature store from events
- [ ] Build data pipelines to ML platform
- [ ] Define feature definitions
- [ ] Implement model inference events

---

## Service Event Responsibilities

### Product Service
- **Publishes**: ProductCreated, ProductUpdated, ProductDeleted
- **Retains**: 7 years (compliance)

### Inventory Service
- **Publishes**: StockIn, StockOut, StockMoved, StockAdjusted, LowStockThresholdReached
- **Retains**: 7 years (compliance)
- **Critical for**: Demand forecasting, stock optimization

### Order Service
- **Publishes**: OrderCreated, OrderConfirmed, OrderFulfilled, OrderShipped, OrderCancelled
- **Retains**: 7 years (compliance)
- **Critical for**: Demand patterns, sales forecasting

### Supplier Service
- **Publishes**: SupplierCreated, SupplierPerformanceUpdated, SupplierDeliveryRegistered
- **Retains**: 7 years (compliance)
- **Critical for**: Supplier optimization, delivery reliability

### Warehouse Service
- **Publishes**: WarehouseCreated, WarehouseCapacityUpdated, ZoneCreated
- **Retains**: 3 years (operational)
- **Important for**: Capacity planning, location optimization

### Analytics Service
- **Publishes**: InventorySnapshotCreated, DailyAnalyticsReportGenerated
- **Consumes**: ALL events
- **Retains**: 3 years (operational)
- **Critical for**: KPI reporting, AI feature generation

### Data Export Service
- **Publishes**: DataExportRequested, DataExportCompleted
- **Consumes**: Relevant events
- **Retains**: 1 year (request tracking)
- **Critical for**: AI data pipelines, backups

---

## Troubleshooting Guide

### Event Not Being Consumed?
1. Check consumer group lag in Kafka
2. Verify consumer has subscribed to topic
3. Check for exceptions in consumer logs
4. Look in dead letter queue for failures
5. Verify event schema matches consumer expectations

### Lost Events?
1. Verify transactional outbox table is being used
2. Check outbox worker is running
3. Verify Kafka broker replication factor >= 2
4. Check storage space on Kafka broker

### Slow Event Processing?
1. Check consumer parallelism (partition count)
2. Verify no blocking operations in consumer
3. Look for database query bottlenecks
4. Check network latency to Kafka broker
5. Monitor CPU/memory on consumer service

### Event Versioning Issues?
1. Verify eventVersion field is semantic (X.Y format)
2. Check consumer handles new optional fields gracefully
3. Ensure old versions still work during migration
4. Use schema registry for validation

---

## Next Steps

1. **For Developers**: Read event-schemas.md to understand event structure
2. **For Architects**: Review ADR-0005 for design rationale
3. **For Operations**: Set up monitoring from event-quick-reference.md
4. **For Data Scientists**: Use event-catalog.md for AI/analytics usefulness levels

---

## Contact & Questions

For questions about the event catalog:
- Architecture: See ADR-0005
- Schema updates: Update ADR with change rationale first
- New events: Create ADR, then update catalogs
- Operational issues: Check troubleshooting guide

---

**Last Updated**: 2026-06-22  
**Version**: 1.0  
**Status**: Accepted

