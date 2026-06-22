# 10. Notification Service

**Bounded Context:** Notifications & Alerts  
**Database:** `notification_db` (PostgreSQL)  
**Port:** 8010  
**Team:** DevOps & Platform  

---

## Purpose

The Notification Service is responsible for delivering alerts and notifications to users and stakeholders through various channels (email, SMS, in-app).

---

## Responsibilities

- Low stock alerts
- Order status notifications
- System alerts and warnings
- Email delivery (via Brevo API)
- SMS delivery (future)
- In-app notifications
- Notification templates
- User subscription management

---

## Database Ownership

**Schema:** `notification_db`

**Core Tables:**
```sql
notification_templates (
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  event_type VARCHAR,
  channel ENUM ('email', 'sms', 'in_app'),
  subject VARCHAR,
  template_body TEXT,
  variables VARCHAR,
  created_at TIMESTAMP
)

subscriptions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  notification_type VARCHAR,
  channel ENUM ('email', 'sms', 'in_app'),
  enabled BOOLEAN DEFAULT true,
  created_at TIMESTAMP
)

notifications_sent (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  notification_type VARCHAR,
  channel VARCHAR,
  recipient VARCHAR,
  subject VARCHAR,
  body TEXT,
  status ENUM ('sent', 'failed', 'bounced', 'opened'),
  sent_at TIMESTAMP,
  delivery_timestamp TIMESTAMP,
  error_details TEXT
)

notification_queue (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  event_type VARCHAR,
  payload JSONB,
  status ENUM ('pending', 'processing', 'sent', 'failed'),
  retry_count INT DEFAULT 0,
  created_at TIMESTAMP,
  processed_at TIMESTAMP
)

low_stock_subscriptions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  warehouse_id UUID,
  threshold_qty INT,
  notification_trigger ENUM ('below_threshold', 'daily_summary'),
  created_at TIMESTAMP
)
```

---

## Events Consumed

### From Inventory Service
- **LowStockDetected:** Send alert to subscribed users
- **StockIn/StockOut:** Optional notification (on subscription)

### From Sales Order Service
- **SalesOrderCreated:** Customer order confirmation
- **SalesOrderShipped:** Shipment notification
- **SalesOrderDelivered:** Delivery confirmation

### From Purchase Order Service
- **PurchaseOrderReceived:** Purchase completion notification

### From Identity Service
- **UserCreated:** Welcome notification
- **PasswordChanged:** Confirmation notification

### From Warehouse Service
- **ReceivingCompleted:** Warehouse manager notification
- **ShippingCompleted:** Shipment completion notification

### From Audit Service
- **AccessDenied:** Security alert to admins

---

## Events Published

### 1. NotificationSent
**When:** Notification successfully delivered  
**Consumers:** Audit Service (for audit trail)

### 2. NotificationFailed
**When:** Delivery failed (retry exceeded)  
**Consumers:** Alert to operations team

---

## REST APIs

**Base URL:** `/api/v1/notifications`

### Notification Templates
- `GET /templates` - List notification templates
- `POST /templates` - Create template
- `GET /templates/{templateId}` - Get template
- `PUT /templates/{templateId}` - Update template

### Subscriptions
- `GET /subscriptions` - User's subscriptions
- `POST /subscriptions` - Subscribe to notifications
- `DELETE /subscriptions/{subscriptionId}` - Unsubscribe
- `PUT /subscriptions/{subscriptionId}` - Update subscription

### Low Stock Alerts
- `GET /subscriptions/low-stock` - Low stock subscriptions
- `POST /subscriptions/low-stock` - Subscribe to low stock
- `PUT /subscriptions/low-stock/{subscriptionId}` - Update alert threshold

### Notification History
- `GET /sent` - History of sent notifications
- `GET /sent/{notificationId}` - Notification details

### Manual Notifications (admin)
- `POST /send` - Send notification to user (admin only)

---

## Dependencies

**Event Sources:**
- Inventory Service
- Sales Order Service
- Purchase Order Service
- Identity Service
- Warehouse Service
- Audit Service

**External Dependencies:**
- Brevo API (email delivery)
- (SMS provider - future)
- Mailpit (development email testing)

---

## Implementation Pattern

### Event Processing Flow

```
1. Event arrives (e.g., LowStockDetected)
   ↓
2. Notification Service processes:
   - Look up subscribed users
   - Load notification template
   - Populate template variables from event payload
   - Add to notification_queue
   ↓
3. Queue processor (async):
   - Fetch pending notifications
   - Route by channel (email, SMS, in-app)
   - Call Brevo API for email
   - Record result (sent/failed)
   - Retry failed notifications (3x with backoff)
   ↓
4. On success:
   - Mark as sent
   - Record delivery timestamp
   - Publish NotificationSent event
   ↓
5. On failure (after retries):
   - Mark as failed
   - Alert operations
   - Log error details
```

---

## Future Scalability

### Multi-Channel Delivery
- SMS delivery (Twilio integration)
- In-app notifications (WebSocket delivery)
- Slack integration (for admin alerts)
- Push notifications (mobile app)

### Notification Preferences
- Per-notification-type preferences
- Quiet hours (no notifications outside business hours)
- Batching/digest mode (daily summary instead of individual alerts)
- Frequency limits (max 1 alert per hour for same type)

### Analytics
- Delivery rates by channel
- Open rates (email)
- User engagement metrics
- Notification effectiveness

---

## Deployment Checklist

- [ ] `notification_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Brevo API credentials configured (environment variable)
- [ ] Email templates created and tested
- [ ] Low-stock alert thresholds configured
- [ ] Event subscription configured (Kafka/RabbitMQ)
- [ ] Queue processor scheduled/containerized
- [ ] Retry logic tested
- [ ] Monitoring/alerting configured
- [ ] Mailpit configured (development)

