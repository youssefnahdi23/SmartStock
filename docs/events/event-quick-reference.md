# Event Catalog - Quick Reference

This document provides a quick lookup for all SmartStock AI events.

## Events by Service

### Product Service
- ProductCreated
- ProductUpdated
- ProductDeleted

### Warehouse Service
- WarehouseCreated
- WarehouseCapacityUpdated
- ZoneCreated

### Inventory Service
- StockIn
- StockOut
- StockMoved
- StockAdjusted
- LowStockThresholdReached

### Supplier Service
- SupplierCreated
- SupplierPerformanceUpdated
- SupplierDeliveryRegistered

### Order Service
- OrderCreated
- OrderConfirmed
- OrderFulfilled
- OrderShipped
- OrderCancelled

### Customer Service
- CustomerCreated
- CustomerUpdated

### Audit Service
- AuditLogCreated

### Notification Service
- NotificationSent

### Analytics Service
- InventorySnapshotCreated
- DailyAnalyticsReportGenerated

### Data Export Service
- DataExportRequested
- DataExportCompleted

### Identity Service
- UserCreated
- UserAuthenticated

---

## Events by AI Usefulness

### Critical (ML Model Training)
- StockIn
- StockOut
- OrderCreated
- SupplierDeliveryRegistered
- InventorySnapshotCreated
- DailyAnalyticsReportGenerated

### High (Strongly Influences Predictions)
- ProductCreated
- SupplierPerformanceUpdated
- OrderConfirmed
- OrderFulfilled
- OrderShipped
- OrderCancelled
- LowStockThresholdReached
- DataExportRequested

### Medium (Contextual Enrichment)
- ProductUpdated
- WarehouseCapacityUpdated
- StockMoved
- StockAdjusted
- CustomerCreated
- CustomerUpdated
- SupplierCreated
- DataExportCompleted

### Low (Minimal ML Value)
- WarehouseCreated
- ZoneCreated
- AuditLogCreated
- NotificationSent
- UserCreated
- UserAuthenticated
- ProductDeleted

---

## Events by Analytics Usefulness

### Critical (Essential for KPIs)
- StockIn
- StockOut
- OrderCreated
- OrderConfirmed
- OrderFulfilled
- OrderShipped
- DailyAnalyticsReportGenerated
- SupplierDeliveryRegistered

### High (Important for Dashboards)
- ProductCreated
- WarehouseCapacityUpdated
- LowStockThresholdReached
- SupplierPerformanceUpdated
- InventorySnapshotCreated
- CustomerCreated
- SupplierCreated

### Medium (Useful for Trend Analysis)
- ProductUpdated
- StockMoved
- StockAdjusted
- OrderCancelled
- CustomerUpdated
- AuditLogCreated
- NotificationSent
- DataExportRequested
- DataExportCompleted

### Low (Minimal Value)
- ZoneCreated
- WarehouseCreated
- UserCreated
- UserAuthenticated
- ProductDeleted

---

## Event Publishing Standards

### Transactional Outbox Pattern
All writes MUST be transactional:
1. Update database entity
2. Insert event into outbox table
3. Commit transaction
4. Async worker publishes events

### Required Metadata (Every Event)
\\\json
{
  "eventId": "UUID",                    // Unique event identifier
  "eventType": "string",                // Type of event
  "eventVersion": "1.0",                // Semantic version
  "timestamp": "ISO8601",               // When event occurred
  "correlationId": "UUID",              // For request tracing
  "requestId": "UUID",                  // For debugging
  "serviceName": "string",              // Publishing service
  "userId": "UUID (optional)",          // Who triggered it
  "tenantId": "UUID (optional)",        // Multi-tenancy
  "data": {}                            // Event-specific data
}
\\\

---

## Event Consumption Patterns

### Idempotent Handling
- Use eventId as deduplication key
- Store processed event IDs in consumer database
- Skip if already processed

### Ordering Guarantees
- Use product_id or entity_id as partition key
- Kafka ensures order within partition
- Process in order per entity

### Error Handling
1. Process event
2. If success: mark complete
3. If retriable error: retry with exponential backoff (1s, 2s, 4s, 8s, 16s max)
4. If non-retriable error: send to dead letter queue

### Dead Letter Queue Strategy
- Capture failed events
- Retain for manual review
- Create alert when DLQ size > threshold
- Manual replay after issue resolution

---

## Performance Requirements

| Metric | Target |
|--------|--------|
| Event publishing latency (p99) | < 100ms |
| Event consumption latency (p99) | < 500ms per consumer |
| Consumer lag | < 5 minutes |
| Dead letter queue | < 100 events/day |
| Event ordering violations | 0 |
| System uptime | 99.9% |

---

## Retention by Service

| Service | Retention | Reason |
|---------|-----------|--------|
| Inventory | 7 years | Core business records |
| Supplier | 7 years | Vendor compliance |
| Order | 7 years | Sales compliance |
| Product | 7 years | Product history |
| Warehouse | 3 years | Operational reference |
| Audit | 10 years | Legal compliance |
| Notification | 1 year | Operational only |
| Analytics | 3 years | Business reporting |
| Data Export | 1 year | Request tracking |
| Identity | 7 years | Security compliance |

---

## Future Event Extensions

### Planned Events (Phase 2 - AI Ready)
- DemandForecastGenerated
- StockOptimizationRecommended
- SupplierRiskDetected
- WarehouseBottleneckIdentified
- AnomalyDetectedInStockMovement
- OptimalReorderSuggested

### Extensibility
Events are designed to support:
- New optional fields via minor version bumps
- New metadata without breaking consumers
- Custom metadata via extensible field
- Future AI-generated recommendations

