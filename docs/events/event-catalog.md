# SmartStock AI - Enterprise Event Catalog

This document defines the complete set of domain events used across the SmartStock AI microservices platform.

## Event Standards

All events MUST follow these standards:

- **Immutable**: Events are immutable records of what happened
- **Versioned**: Events support versioning for backward compatibility
- **Replayable**: Events can be replayed to rebuild state
- **Timestamped**: All events include ISO 8601 timestamps
- **Traceable**: All events include correlationId and requestId
- **Provenance**: All events track the service source and user context

### Event Structure

Every event MUST include this metadata:

\\\json
{
  "eventId": "UUID",
  "eventType": "string",
  "eventVersion": "1",
  "timestamp": "ISO8601",
  "correlationId": "UUID",
  "requestId": "UUID",
  "serviceName": "string",
  "userId": "UUID (optional)",
  "tenantId": "UUID (optional)",
  "data": {}
}
\\\

---

## Domain Events

### Product Service Events

#### ProductCreated

**Producer**: Product Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years (compliance)  
**AI Usefulness**: High (product baseline)  
**Analytics Usefulness**: High (product catalog)

\\\json
{
  "eventType": "ProductCreated",
  "eventVersion": "1.0",
  "data": {
    "productId": "UUID",
    "sku": "string",
    "name": "string",
    "description": "string",
    "category": "string",
    "unitPrice": "decimal",
    "unitCost": "decimal",
    "weight": "decimal (kg)",
    "dimensions": {
      "length": "decimal (cm)",
      "width": "decimal (cm)",
      "height": "decimal (cm)"
    },
    "barcode": "string",
    "qrCode": "string",
    "createdBy": "UUID",
    "status": "ACTIVE | INACTIVE | DISCONTINUED"
  }
}
\\\

#### ProductUpdated

**Producer**: Product Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Medium  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "ProductUpdated",
  "eventVersion": "1.0",
  "data": {
    "productId": "UUID",
    "changes": {
      "name": "string (optional)",
      "description": "string (optional)",
      "unitPrice": "decimal (optional)",
      "unitCost": "decimal (optional)",
      "status": "string (optional)"
    },
    "updatedBy": "UUID",
    "previousValues": {}
  }
}
\\\

#### ProductDeleted

**Producer**: Product Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "ProductDeleted",
  "eventVersion": "1.0",
  "data": {
    "productId": "UUID",
    "reason": "string",
    "deletedBy": "UUID"
  }
}
\\\

---

### Warehouse Service Events

#### WarehouseCreated

**Producer**: Warehouse Service  
**Consumers**: Inventory Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: High (location baseline)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "WarehouseCreated",
  "eventVersion": "1.0",
  "data": {
    "warehouseId": "UUID",
    "name": "string",
    "code": "string",
    "location": {
      "street": "string",
      "city": "string",
      "state": "string",
      "postalCode": "string",
      "country": "string",
      "latitude": "decimal",
      "longitude": "decimal"
    },
    "capacity": {
      "totalVolume": "decimal (cubic meters)",
      "totalWeight": "decimal (kg)"
    },
    "zones": [
      {
        "zoneId": "UUID",
        "name": "string",
        "type": "RECEIVING | STORAGE | PACKING | RETURNS",
        "capacity": "decimal"
      }
    ],
    "createdBy": "UUID",
    "status": "ACTIVE | INACTIVE"
  }
}
\\\

#### WarehouseCapacityUpdated

**Producer**: Warehouse Service  
**Consumers**: Inventory Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 3 years  
**AI Usefulness**: High (optimization)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "WarehouseCapacityUpdated",
  "eventVersion": "1.0",
  "data": {
    "warehouseId": "UUID",
    "totalCapacityVolume": "decimal (cubic meters)",
    "usedCapacityVolume": "decimal (cubic meters)",
    "percentageUsed": "decimal",
    "timestamp": "ISO8601"
  }
}
\\\

#### ZoneCreated

**Producer**: Warehouse Service  
**Consumers**: Inventory Service, Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Medium  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "ZoneCreated",
  "eventVersion": "1.0",
  "data": {
    "zoneId": "UUID",
    "warehouseId": "UUID",
    "name": "string",
    "type": "RECEIVING | STORAGE | PACKING | RETURNS",
    "capacity": "decimal (cubic meters)",
    "createdBy": "UUID"
  }
}
\\\

---

### Inventory Service Events

#### StockIn

**Producer**: Inventory Service  
**Consumers**: Warehouse Service, Audit Service, Analytics Service, Notification Service, Reporting Service  
**Version**: 1.0  
**Retention**: 7 years (permanent)  
**AI Usefulness**: Critical (demand pattern)  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "StockIn",
  "eventVersion": "1.0",
  "data": {
    "inventoryId": "UUID",
    "productId": "UUID",
    "warehouseId": "UUID",
    "zoneId": "UUID",
    "quantity": "integer",
    "unitCost": "decimal",
    "totalCost": "decimal",
    "sourceType": "PURCHASE_ORDER | TRANSFER | ADJUSTMENT | RETURN",
    "referenceId": "UUID (PO, Transfer ID, etc.)",
    "receivedBy": "UUID",
    "notes": "string (optional)",
    "batchNumber": "string (optional)",
    "expiryDate": "ISO8601 (optional)"
  }
}
\\\

#### StockOut

**Producer**: Inventory Service  
**Consumers**: Warehouse Service, Audit Service, Analytics Service, Notification Service, Reporting Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Critical (sales pattern)  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "StockOut",
  "eventVersion": "1.0",
  "data": {
    "inventoryId": "UUID",
    "productId": "UUID",
    "warehouseId": "UUID",
    "quantity": "integer",
    "unitPrice": "decimal",
    "totalPrice": "decimal",
    "destinationType": "SALES | TRANSFER | DAMAGE | LOSS",
    "referenceId": "UUID (Order ID, Transfer ID, etc.)",
    "releasedBy": "UUID",
    "reason": "string (optional)"
  }
}
\\\

#### StockMoved

**Producer**: Inventory Service  
**Consumers**: Warehouse Service, Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 3 years  
**AI Usefulness**: Medium (location pattern)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "StockMoved",
  "eventVersion": "1.0",
  "data": {
    "inventoryId": "UUID",
    "productId": "UUID",
    "quantity": "integer",
    "fromWarehouse": "UUID",
    "fromZone": "UUID",
    "toWarehouse": "UUID",
    "toZone": "UUID",
    "reason": "REPLENISHMENT | OPTIMIZATION | ORGANIZATION | TRANSFER",
    "movedBy": "UUID"
  }
}
\\\

#### StockAdjusted

**Producer**: Inventory Service  
**Consumers**: Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Medium (data quality)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "StockAdjusted",
  "eventVersion": "1.0",
  "data": {
    "inventoryId": "UUID",
    "productId": "UUID",
    "warehouseId": "UUID",
    "quantityBefore": "integer",
    "quantityAfter": "integer",
    "adjustment": "integer (can be negative)",
    "reason": "INVENTORY_COUNT | DAMAGE | LOSS | CORRECTION | EXPIRY",
    "adjustedBy": "UUID",
    "notes": "string"
  }
}
\\\

#### LowStockThresholdReached

**Producer**: Inventory Service  
**Consumers**: Audit Service, Notification Service, Reporting Service  
**Version**: 1.0  
**Retention**: 2 years  
**AI Usefulness**: High (replenishment trigger)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "LowStockThresholdReached",
  "eventVersion": "1.0",
  "data": {
    "productId": "UUID",
    "warehouseId": "UUID",
    "currentQuantity": "integer",
    "minimumThreshold": "integer",
    "reorderQuantity": "integer",
    "reorderPoint": "integer"
  }
}
\\\

---

### Supplier Service Events

#### SupplierCreated

**Producer**: Supplier Service  
**Consumers**: Order Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: High (supplier baseline)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "SupplierCreated",
  "eventVersion": "1.0",
  "data": {
    "supplierId": "UUID",
    "name": "string",
    "code": "string",
    "contact": {
      "email": "string",
      "phone": "string",
      "address": "string"
    },
    "paymentTerms": "NET30 | NET60 | COD",
    "leadTimeDays": "integer",
    "minimumOrderQuantity": "integer",
    "status": "ACTIVE | INACTIVE | SUSPENDED",
    "createdBy": "UUID"
  }
}
\\\

#### SupplierPerformanceUpdated

**Producer**: Supplier Service  
**Consumers**: Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 3 years  
**AI Usefulness**: Critical (supplier optimization)  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "SupplierPerformanceUpdated",
  "eventVersion": "1.0",
  "data": {
    "supplierId": "UUID",
    "totalOrders": "integer",
    "completedOnTime": "integer",
    "completedLate": "integer",
    "onTimePercentage": "decimal (0-100)",
    "averageLeadTimeDays": "decimal",
    "qualityScore": "decimal (0-100)",
    "priceCompetitiveness": "decimal (0-100)",
    "lastUpdated": "ISO8601"
  }
}
\\\

#### SupplierDeliveryRegistered

**Producer**: Supplier Service  
**Consumers**: Inventory Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Critical  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "SupplierDeliveryRegistered",
  "eventVersion": "1.0",
  "data": {
    "deliveryId": "UUID",
    "purchaseOrderId": "UUID",
    "supplierId": "UUID",
    "items": [
      {
        "productId": "UUID",
        "quantity": "integer",
        "unitCost": "decimal"
      }
    ],
    "totalCost": "decimal",
    "deliveryDate": "ISO8601",
    "expectedDate": "ISO8601",
    "daysLate": "integer (can be negative for early)",
    "qualityIssues": "boolean",
    "notes": "string (optional)"
  }
}
\\\

---

### Order Service Events

#### OrderCreated

**Producer**: Order Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Critical (demand signal)  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "OrderCreated",
  "eventVersion": "1.0",
  "data": {
    "orderId": "UUID",
    "customerId": "UUID",
    "orderDate": "ISO8601",
    "expectedDeliveryDate": "ISO8601",
    "items": [
      {
        "productId": "UUID",
        "quantity": "integer",
        "unitPrice": "decimal"
      }
    ],
    "totalAmount": "decimal",
    "status": "PENDING | CONFIRMED | PROCESSING",
    "createdBy": "UUID"
  }
}
\\\

#### OrderConfirmed

**Producer**: Order Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: High  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "OrderConfirmed",
  "eventVersion": "1.0",
  "data": {
    "orderId": "UUID",
    "customerId": "UUID",
    "totalAmount": "decimal",
    "items": [
      {
        "productId": "UUID",
        "quantity": "integer"
      }
    ]
  }
}
\\\

#### OrderFulfilled

**Producer**: Order Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: High  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "OrderFulfilled",
  "eventVersion": "1.0",
  "data": {
    "orderId": "UUID",
    "customerId": "UUID",
    "fulfillmentDate": "ISO8601",
    "items": [
      {
        "productId": "UUID",
        "quantity": "integer"
      }
    ]
  }
}
\\\

#### OrderShipped

**Producer**: Order Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 5 years  
**AI Usefulness**: Medium  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "OrderShipped",
  "eventVersion": "1.0",
  "data": {
    "orderId": "UUID",
    "customerId": "UUID",
    "shipmentDate": "ISO8601",
    "trackingNumber": "string",
    "carrier": "string",
    "estimatedDeliveryDate": "ISO8601"
  }
}
\\\

#### OrderCancelled

**Producer**: Order Service  
**Consumers**: Inventory Service, Warehouse Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Medium  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "OrderCancelled",
  "eventVersion": "1.0",
  "data": {
    "orderId": "UUID",
    "customerId": "UUID",
    "reason": "string",
    "cancelledBy": "UUID"
  }
}
\\\

---

### Customer Service Events

#### CustomerCreated

**Producer**: Customer Service  
**Consumers**: Order Service, Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Medium (customer baseline)  
**Analytics Usefulness**: High

\\\json
{
  "eventType": "CustomerCreated",
  "eventVersion": "1.0",
  "data": {
    "customerId": "UUID",
    "name": "string",
    "email": "string",
    "phone": "string",
    "address": {
      "street": "string",
      "city": "string",
      "state": "string",
      "postalCode": "string",
      "country": "string"
    },
    "customerType": "INDIVIDUAL | BUSINESS",
    "createdBy": "UUID"
  }
}
\\\

#### CustomerUpdated

**Producer**: Customer Service  
**Consumers**: Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "CustomerUpdated",
  "eventVersion": "1.0",
  "data": {
    "customerId": "UUID",
    "changes": {
      "email": "string (optional)",
      "phone": "string (optional)",
      "address": "object (optional)"
    },
    "updatedBy": "UUID"
  }
}
\\\

---

### Audit Service Events

#### AuditLogCreated

**Producer**: Audit Service  
**Consumers**: Analytics Service, Reporting Service  
**Version**: 1.0  
**Retention**: 10 years (regulatory)  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "AuditLogCreated",
  "eventVersion": "1.0",
  "data": {
    "auditId": "UUID",
    "action": "CREATE | UPDATE | DELETE | TRANSFER | ADJUST",
    "entityType": "PRODUCT | INVENTORY | ORDER | SUPPLIER | WAREHOUSE",
    "entityId": "UUID",
    "userId": "UUID",
    "timestamp": "ISO8601",
    "changes": "object",
    "ipAddress": "string",
    "status": "SUCCESS | FAILURE"
  }
}
\\\

---

### Notification Service Events

#### NotificationSent

**Producer**: Notification Service  
**Consumers**: Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 1 year  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "NotificationSent",
  "eventVersion": "1.0",
  "data": {
    "notificationId": "UUID",
    "type": "EMAIL | SMS | IN_APP | PUSH",
    "recipientId": "UUID",
    "subject": "string",
    "channel": "BREVO | SMS_GATEWAY | PUSH_SERVICE",
    "status": "SENT | FAILED | BOUNCED",
    "triggerEvent": "string"
  }
}
\\\

---

### Analytics Service Events

#### InventorySnapshotCreated

**Producer**: Analytics Service  
**Consumers**: Reporting Service, Data Export Service  
**Version**: 1.0  
**Retention**: 3 years  
**AI Usefulness**: Critical (time-series)  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "InventorySnapshotCreated",
  "eventVersion": "1.0",
  "data": {
    "snapshotId": "UUID",
    "snapshotDate": "ISO8601",
    "warehouseId": "UUID",
    "productId": "UUID",
    "quantity": "integer",
    "value": "decimal",
    "utilizationPercentage": "decimal"
  }
}
\\\

#### DailyAnalyticsReportGenerated

**Producer**: Analytics Service  
**Consumers**: Reporting Service, Data Export Service  
**Version**: 1.0  
**Retention**: 3 years  
**AI Usefulness**: High  
**Analytics Usefulness**: Critical

\\\json
{
  "eventType": "DailyAnalyticsReportGenerated",
  "eventVersion": "1.0",
  "data": {
    "reportId": "UUID",
    "reportDate": "ISO8601",
    "metrics": {
      "totalOrders": "integer",
      "totalRevenue": "decimal",
      "totalInventoryValue": "decimal",
      "warehouseUtilization": "decimal",
      "stockInCount": "integer",
      "stockOutCount": "integer"
    }
  }
}
\\\

---

### Data Export Service Events

#### DataExportRequested

**Producer**: Data Export Service  
**Consumers**: Analytics Service  
**Version**: 1.0  
**Retention**: 1 year  
**AI Usefulness**: Medium  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "DataExportRequested",
  "eventVersion": "1.0",
  "data": {
    "exportId": "UUID",
    "dataType": "INVENTORY_MOVEMENTS | SUPPLIER_PERFORMANCE | WAREHOUSE_UTILIZATION | SALES_HISTORY",
    "format": "CSV | JSON | PARQUET",
    "startDate": "ISO8601",
    "endDate": "ISO8601",
    "requestedBy": "UUID"
  }
}
\\\

#### DataExportCompleted

**Producer**: Data Export Service  
**Consumers**: Analytics Service  
**Version**: 1.0  
**Retention**: 1 year  
**AI Usefulness**: Medium  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "DataExportCompleted",
  "eventVersion": "1.0",
  "data": {
    "exportId": "UUID",
    "fileLocation": "string (MinIO path)",
    "fileSize": "integer (bytes)",
    "rowCount": "integer",
    "completionTime": "ISO8601"
  }
}
\\\

---

### Identity Service Events

#### UserCreated

**Producer**: Identity Service  
**Consumers**: Audit Service, Analytics Service, Notification Service  
**Version**: 1.0  
**Retention**: 7 years  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "UserCreated",
  "eventVersion": "1.0",
  "data": {
    "userId": "UUID",
    "email": "string",
    "roles": ["ADMIN | MANAGER | OPERATOR | VIEWER"],
    "status": "ACTIVE | INACTIVE"
  }
}
\\\

#### UserAuthenticated

**Producer**: Identity Service  
**Consumers**: Audit Service, Analytics Service  
**Version**: 1.0  
**Retention**: 1 year  
**AI Usefulness**: Low  
**Analytics Usefulness**: Medium

\\\json
{
  "eventType": "UserAuthenticated",
  "eventVersion": "1.0",
  "data": {
    "userId": "UUID",
    "timestamp": "ISO8601",
    "ipAddress": "string",
    "success": "boolean"
  }
}
\\\

---

## Event Versioning Strategy

### Version Format

Events follow semantic versioning: MAJOR.MINOR

- **MAJOR**: Breaking changes (new required fields, field type changes)
- **MINOR**: Non-breaking changes (new optional fields, field additions)

### Backward Compatibility

- Consumers MUST handle missing optional fields with defaults
- Producers MUST maintain support for at least 2 previous major versions
- Old event versions MUST be migrated during service updates
- Migration scripts MUST be idempotent

---

## Event Publishing Guidelines

1. **Exactly Once Semantics**: Use transactional outbox pattern
2. **Ordering**: Events for same entity MUST maintain order
3. **Latency**: Events MUST be published within 100ms of action
4. **Dead Letter Queue**: Failed events sent to DLQ for replay
5. **Circuit Breaker**: Use circuit breaker for messaging failures

---

## Event Consumption Guidelines

1. **Idempotence**: Consumers MUST be idempotent (handle duplicates)
2. **Ordering**: Consume in order per partition key (entity ID)
3. **Replay Safety**: Consumers MUST support full event replay
4. **Error Handling**: Retry with exponential backoff
5. **Dead Letter Queue**: Send unprocessable events to DLQ

---

## Data Retention Policy

| Retention | Categories | Reason |
|-----------|-----------|--------|
| 1 year | Notifications, Session logs | Short-term operational |
| 2-3 years | Analytics, Cache-related events | Business reporting |
| 5 years | Shipment, Delivery events | Business reference |
| 7 years | Transactions, Product changes, Supplier data, Audit logs | Business compliance |
| 10 years | Regulatory Audit logs | Legal compliance |

---

## AI Usefulness Classification

| Level | Criteria | Examples |
|-------|----------|----------|
| **Critical** | Core to ML model training | StockIn, StockOut, SupplierDelivery, OrderCreated |
| **High** | Strongly influences predictions | ProductCreated, SupplierPerformance, InventorySnapshot |
| **Medium** | Contextual data for enrichment | ProductUpdated, CustomerUpdated, StockMoved |
| **Low** | Minimal ML value | UserAuthenticated, NotificationSent |

---

## Analytics Usefulness Classification

| Level | Criteria | Examples |
|-------|----------|----------|
| **Critical** | Essential for business KPIs | StockIn/Out, Orders, Daily Reports |
| **High** | Important for dashboards | Warehouse capacity, Product updates, Supplier performance |
| **Medium** | Useful for trend analysis | Customer updates, Notifications |
| **Low** | Minimal analytics value | Internal service events |

---

## Event Monitoring & Alerting

Monitor these metrics:

- Event publishing latency (p99 < 100ms)
- Event processing latency per consumer (p99 < 500ms)
- Dead letter queue size (should be < 100/day)
- Event ordering violations (should be 0)
- Consumer lag (should be < 5 minutes)

---

## Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-22 | Architecture Team | Initial event catalog |

