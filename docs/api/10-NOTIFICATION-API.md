# Notification Service API

**Base URL:** `/api/v1/notifications`  
**Service Port:** 8010  
**Authentication:** JWT Bearer Token (required for most endpoints)  
**Status:** Supporting Service

---

## Overview

The Notification Service manages system notifications, email alerts, low stock warnings, and customer communications across the SmartStock AI platform.

---

## Endpoints

### 1. Send Email Alert

**Endpoint:** `POST /api/v1/notifications/email`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `notification:send`

**Request Body:**
```json
{
  "to": "john@company.com",
  "subject": "Low Stock Alert: Premium Widget",
  "templateId": "low-stock-alert",
  "templateData": {
    "productName": "Premium Widget",
    "currentStock": 45,
    "reorderPoint": 50,
    "warehouseName": "Main Warehouse"
  },
  "priority": "HIGH"
}
```

**Response (202 Accepted):**
```json
{
  "data": {
    "notificationId": "notif-123",
    "status": "QUEUED",
    "to": "john@company.com",
    "templateId": "low-stock-alert",
    "sentAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 2. Get User Notifications

**Endpoint:** `GET /api/v1/notifications/user/notifications`  
**Authentication:** Required (JWT Bearer Token)

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page
- `read` (boolean, optional) - Filter by read status
- `type` (string, optional) - Notification type

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "notif-123",
      "type": "ALERT",
      "title": "Low Stock Alert",
      "message": "Premium Widget stock below reorder point",
      "read": false,
      "createdAt": "2026-06-20T12:00:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "unreadCount": 5,
    "traceId": "trace-123"
  }
}
```

---

### 3. Mark Notification as Read

**Endpoint:** `PUT /api/v1/notifications/{notificationId}/read`  
**Authentication:** Required (JWT Bearer Token)

**Response (200 OK):**
```json
{
  "data": {
    "id": "notif-123",
    "read": true,
    "readAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 4. Configure Notification Preferences

**Endpoint:** `POST /api/v1/notifications/preferences`  
**Authentication:** Required (JWT Bearer Token)

**Request Body:**
```json
{
  "userId": "user-123",
  "lowStockAlerts": {
    "enabled": true,
    "threshold": 5,
    "frequency": "IMMEDIATE",
    "channels": ["EMAIL", "IN_APP"]
  },
  "orderAlerts": {
    "enabled": true,
    "channels": ["EMAIL"]
  },
  "systemAlerts": {
    "enabled": true,
    "channels": ["IN_APP"]
  },
  "quietHours": {
    "enabled": true,
    "startTime": "18:00",
    "endTime": "08:00"
  }
}
```

**Response (200 OK):**
```json
{
  "data": {
    "userId": "user-123",
    "preferences": {
      "lowStockAlerts": {
        "enabled": true,
        "channels": ["EMAIL", "IN_APP"]
      }
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Events Published

- `LowStockAlertTriggered` - Stock below reorder point
- `OrderStatusChanged` - Order status updated
- `DeliveryReminder` - Upcoming delivery reminder
- `PaymentDue` - Payment due reminder
- `SystemAlert` - System-level alert (backup, maintenance)

---

## Implementation Notes

1. Email delivery via Brevo API integration
2. Notification preferences per user configurable
3. Quiet hours respected for non-critical alerts
4. Template-based email generation
5. Retry logic for failed email delivery
6. Notifications retained for 90 days
7. Bulk notification support for system-wide alerts
8. Priority-based delivery queue
9. SMS support for critical alerts (future)
10. Push notifications for mobile clients (future)
