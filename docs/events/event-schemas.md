# Event Schemas - JSON Schema Definitions

This document provides formal JSON Schema definitions for all SmartStock AI events.

## Base Event Schema

All events extend this base schema:

\\\json
{
  \"\\": \"http://json-schema.org/draft-07/schema#\",
  \"title\": \"BaseEvent\",
  \"type\": \"object\",
  \"required\": [\"eventId\", \"eventType\", \"eventVersion\", \"timestamp\", \"correlationId\", \"requestId\", \"serviceName\", \"data\"],
  \"properties\": {
    \"eventId\": {
      \"type\": \"string\",
      \"format\": \"uuid\",
      \"description\": \"Unique event identifier\"
    },
    \"eventType\": {
      \"type\": \"string\",
      \"description\": \"Event type (e.g., StockIn, OrderCreated)\"
    },
    \"eventVersion\": {
      \"type\": \"string\",
      \"pattern\": \"^[0-9]+\\\\.[0-9]+\$\",
      \"description\": \"Semantic version (MAJOR.MINOR)\"
    },
    \"timestamp\": {
      \"type\": \"string\",
      \"format\": \"date-time\",
      \"description\": \"When event occurred (ISO8601)\"
    },
    \"correlationId\": {
      \"type\": \"string\",
      \"format\": \"uuid\",
      \"description\": \"Request correlation ID for tracing\"
    },
    \"requestId\": {
      \"type\": \"string\",
      \"format\": \"uuid\",
      \"description\": \"Original request ID\"
    },
    \"serviceName\": {
      \"type\": \"string\",
      \"enum\": [
        \"IdentityService\",
        \"ProductService\",
        \"InventoryService\",
        \"WarehouseService\",
        \"SupplierService\",
        \"CustomerService\",
        \"OrderService\",
        \"AuditService\",
        \"NotificationService\",
        \"ReportingService\",
        \"DataExportService\",
        \"AnalyticsService\"
      ],
      \"description\": \"Publishing service\"
    },
    \"userId\": {
      \"type\": [\"string\", \"null\"],
      \"format\": \"uuid\",
      \"description\": \"User who triggered event (optional)\"
    },
    \"tenantId\": {
      \"type\": [\"string\", \"null\"],
      \"format\": \"uuid\",
      \"description\": \"Tenant ID for multi-tenancy (optional)\"
    },
    \"data\": {
      \"type\": \"object\",
      \"description\": \"Event-specific data payload\"
    }
  }
}
\\\

---

## Critical Event Schemas

### StockIn Event (v1.0)

\\\json
{
  \"\\": \"http://json-schema.org/draft-07/schema#\",
  \"title\": \"StockInEvent\",
  \"allOf\": [
    { \"\\": \"#/definitions/BaseEvent\" },
    {
      \"type\": \"object\",
      \"required\": [\"data\"],
      \"properties\": {
        \"data\": {
          \"type\": \"object\",
          \"required\": [\"inventoryId\", \"productId\", \"warehouseId\", \"quantity\", \"unitCost\", \"totalCost\", \"sourceType\", \"receivedBy\"],
          \"properties\": {
            \"inventoryId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"productId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"warehouseId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"zoneId\": { \"type\": [\"string\", \"null\"], \"format\": \"uuid\" },
            \"quantity\": { \"type\": \"integer\", \"minimum\": 1 },
            \"unitCost\": { \"type\": \"number\", \"minimum\": 0 },
            \"totalCost\": { \"type\": \"number\", \"minimum\": 0 },
            \"sourceType\": {
              \"type\": \"string\",
              \"enum\": [\"PURCHASE_ORDER\", \"TRANSFER\", \"ADJUSTMENT\", \"RETURN\"]
            },
            \"referenceId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"receivedBy\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"notes\": { \"type\": [\"string\", \"null\"] },
            \"batchNumber\": { \"type\": [\"string\", \"null\"] },
            \"expiryDate\": { \"type\": [\"string\", \"null\"], \"format\": \"date-time\" }
          }
        }
      }
    }
  ]
}
\\\

### StockOut Event (v1.0)

\\\json
{
  \"\\": \"http://json-schema.org/draft-07/schema#\",
  \"title\": \"StockOutEvent\",
  \"allOf\": [
    { \"\\": \"#/definitions/BaseEvent\" },
    {
      \"type\": \"object\",
      \"required\": [\"data\"],
      \"properties\": {
        \"data\": {
          \"type\": \"object\",
          \"required\": [\"inventoryId\", \"productId\", \"warehouseId\", \"quantity\", \"unitPrice\", \"totalPrice\", \"destinationType\", \"releasedBy\"],
          \"properties\": {
            \"inventoryId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"productId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"warehouseId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"quantity\": { \"type\": \"integer\", \"minimum\": 1 },
            \"unitPrice\": { \"type\": \"number\", \"minimum\": 0 },
            \"totalPrice\": { \"type\": \"number\", \"minimum\": 0 },
            \"destinationType\": {
              \"type\": \"string\",
              \"enum\": [\"SALES\", \"TRANSFER\", \"DAMAGE\", \"LOSS\"]
            },
            \"referenceId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"releasedBy\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"reason\": { \"type\": [\"string\", \"null\"] }
          }
        }
      }
    }
  ]
}
\\\

### OrderCreated Event (v1.0)

\\\json
{
  \"\\": \"http://json-schema.org/draft-07/schema#\",
  \"title\": \"OrderCreatedEvent\",
  \"allOf\": [
    { \"\\": \"#/definitions/BaseEvent\" },
    {
      \"type\": \"object\",
      \"required\": [\"data\"],
      \"properties\": {
        \"data\": {
          \"type\": \"object\",
          \"required\": [\"orderId\", \"customerId\", \"orderDate\", \"items\", \"totalAmount\", \"createdBy\"],
          \"properties\": {
            \"orderId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"customerId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"orderDate\": { \"type\": \"string\", \"format\": \"date-time\" },
            \"expectedDeliveryDate\": { \"type\": [\"string\", \"null\"], \"format\": \"date-time\" },
            \"items\": {
              \"type\": \"array\",
              \"minItems\": 1,
              \"items\": {
                \"type\": \"object\",
                \"required\": [\"productId\", \"quantity\", \"unitPrice\"],
                \"properties\": {
                  \"productId\": { \"type\": \"string\", \"format\": \"uuid\" },
                  \"quantity\": { \"type\": \"integer\", \"minimum\": 1 },
                  \"unitPrice\": { \"type\": \"number\", \"minimum\": 0 }
                }
              }
            },
            \"totalAmount\": { \"type\": \"number\", \"minimum\": 0 },
            \"status\": {
              \"type\": \"string\",
              \"enum\": [\"PENDING\", \"CONFIRMED\", \"PROCESSING\"]
            },
            \"createdBy\": { \"type\": \"string\", \"format\": \"uuid\" }
          }
        }
      }
    }
  ]
}
\\\

### SupplierDeliveryRegistered Event (v1.0)

\\\json
{
  \"\\": \"http://json-schema.org/draft-07/schema#\",
  \"title\": \"SupplierDeliveryRegisteredEvent\",
  \"allOf\": [
    { \"\\": \"#/definitions/BaseEvent\" },
    {
      \"type\": \"object\",
      \"required\": [\"data\"],
      \"properties\": {
        \"data\": {
          \"type\": \"object\",
          \"required\": [\"deliveryId\", \"purchaseOrderId\", \"supplierId\", \"items\", \"totalCost\", \"deliveryDate\", \"expectedDate\"],
          \"properties\": {
            \"deliveryId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"purchaseOrderId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"supplierId\": { \"type\": \"string\", \"format\": \"uuid\" },
            \"items\": {
              \"type\": \"array\",
              \"minItems\": 1,
              \"items\": {
                \"type\": \"object\",
                \"required\": [\"productId\", \"quantity\", \"unitCost\"],
                \"properties\": {
                  \"productId\": { \"type\": \"string\", \"format\": \"uuid\" },
                  \"quantity\": { \"type\": \"integer\", \"minimum\": 1 },
                  \"unitCost\": { \"type\": \"number\", \"minimum\": 0 }
                }
              }
            },
            \"totalCost\": { \"type\": \"number\", \"minimum\": 0 },
            \"deliveryDate\": { \"type\": \"string\", \"format\": \"date-time\" },
            \"expectedDate\": { \"type\": \"string\", \"format\": \"date-time\" },
            \"daysLate\": { \"type\": \"integer\" },
            \"qualityIssues\": { \"type\": \"boolean\" },
            \"notes\": { \"type\": [\"string\", \"null\"] }
          }
        }
      }
    }
  ]
}
\\\

---

## Event Schema Registry

All event schemas MUST be stored in a centralized schema registry:

\\\
schemas/
+-- inventory/
¦   +-- StockIn-1.0.json
¦   +-- StockOut-1.0.json
¦   +-- StockMoved-1.0.json
¦   +-- StockAdjusted-1.0.json
¦   +-- LowStockThresholdReached-1.0.json
+-- order/
¦   +-- OrderCreated-1.0.json
¦   +-- OrderConfirmed-1.0.json
¦   +-- OrderFulfilled-1.0.json
¦   +-- OrderShipped-1.0.json
¦   +-- OrderCancelled-1.0.json
+-- supplier/
¦   +-- SupplierCreated-1.0.json
¦   +-- SupplierPerformanceUpdated-1.0.json
¦   +-- SupplierDeliveryRegistered-1.0.json
+-- product/
¦   +-- ProductCreated-1.0.json
¦   +-- ProductUpdated-1.0.json
¦   +-- ProductDeleted-1.0.json
+-- warehouse/
¦   +-- WarehouseCreated-1.0.json
¦   +-- WarehouseCapacityUpdated-1.0.json
¦   +-- ZoneCreated-1.0.json
+-- customer/
¦   +-- CustomerCreated-1.0.json
¦   +-- CustomerUpdated-1.0.json
+-- analytics/
¦   +-- InventorySnapshotCreated-1.0.json
¦   +-- DailyAnalyticsReportGenerated-1.0.json
+-- identity/
    +-- UserCreated-1.0.json
    +-- UserAuthenticated-1.0.json
\\\

---

## Validation Rules

### All Events MUST
- ? Have unique eventId (UUID v4)
- ? Be immutable after publishing
- ? Include ISO8601 timestamp
- ? Include correlationId for tracing
- ? Include serviceName of producer
- ? Follow semantic versioning
- ? Pass JSON Schema validation
- ? Be idempotent when consumed (same data, same eventId ? same result)

### All Events MUST NOT
- ? Contain null values for required fields
- ? Contain secrets (passwords, API keys, PII)
- ? Contain business logic
- ? Reference other services' internal structures
- ? Use timestamps without timezone info
- ? Contain non-numeric transaction amounts

---

## Backward Compatibility Rules

### Version 1.0 ? 1.1 (Minor version bump - non-breaking)
- ? Add new optional fields
- ? Add new enum values
- ? Add new nested objects
- ? Cannot remove fields
- ? Cannot make optional fields required
- ? Cannot change field types

### Version 1.0 ? 2.0 (Major version bump - breaking)
- ? Remove fields
- ? Make optional fields required
- ? Change field types
- ? Must keep 1 major version for backward compatibility
- ? Must provide migration path

---

## Example Event Flow

\\\
1. Customer places order
   ? OrderService publishes OrderCreated v1.0

2. Inventory Service consumes OrderCreated
   ? Validates event schema v1.0
   ? Uses correlationId for tracing
   ? Updates inventory counts
   ? Publishes StockOut events

3. Warehouse Service consumes StockOut
   ? Marks items for picking
   ? Updates zone usage

4. Analytics Service consumes all events
   ? Aggregates into hourly snapshots
   ? Publishes InventorySnapshotCreated

5. Data Export Service consumes all events
   ? Exports to data lake daily
   ? Data available for AI training
\\\

