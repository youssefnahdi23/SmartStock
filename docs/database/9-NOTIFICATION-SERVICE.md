# Database Specification: Notification Service

**Service**: Notification Service  
**Purpose**: Store notification templates, logs, and delivery tracking  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Notification Service manages notification templates, queuing, delivery tracking, and communication history.

### High-Level Architecture
```
notification_templates
├── notification_queue (1:M)
├── notification_deliveries (1:M)
├── notification_preferences (1:M)
└── notification_channels (1:M)
```

---

## 2. Tables Specification

### 2.1 `notification_channels` Table
**Purpose**: Define available notification channels

```sql
CREATE TABLE notification_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_name VARCHAR(100) UNIQUE NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    max_daily_limit INT,
    throttle_rate_ms INT DEFAULT 100,
    retry_enabled BOOLEAN DEFAULT true,
    max_retries INT DEFAULT 3,
    retry_backoff_ms INT DEFAULT 1000,
    timeout_ms INT DEFAULT 30000,
    provider_config JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT valid_channel_type CHECK (channel_type IN ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK', 'WEBHOOK_SIGNATURE', 'IN_APP', 'BREVO_EMAIL'))
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: channel_type, is_active
**Analytics**: Channel usage metrics

---

### 2.2 `notification_templates` Table
**Purpose**: Store notification message templates

```sql
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    description TEXT,
    template_type VARCHAR(50) NOT NULL,
    supported_channels JSONB NOT NULL,
    subject_template VARCHAR(500),
    body_template TEXT NOT NULL,
    html_body_template TEXT,
    variables JSONB,
    is_active BOOLEAN DEFAULT true,
    version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT template_code_not_empty CHECK (template_code != ''),
    CONSTRAINT template_name_not_empty CHECK (template_name != ''),
    CONSTRAINT valid_template_type CHECK (template_type IN ('TRANSACTIONAL', 'PROMOTIONAL', 'ALERT', 'CONFIRMATION', 'REMINDER', 'REPORT', 'NOTIFICATION'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by, version
**Indexes**: template_code, template_type, is_active
**Analytics**: Template usage frequency

---

### 2.3 `notification_preferences` Table
**Purpose**: Store user notification preferences

```sql
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    email_notifications BOOLEAN DEFAULT true,
    sms_notifications BOOLEAN DEFAULT false,
    push_notifications BOOLEAN DEFAULT true,
    in_app_notifications BOOLEAN DEFAULT true,
    webhook_notifications BOOLEAN DEFAULT false,
    notification_frequency VARCHAR(50) DEFAULT 'REAL_TIME',
    do_not_disturb_start TIME,
    do_not_disturb_end TIME,
    do_not_disturb_timezone VARCHAR(100),
    do_not_disturb_days JSONB,
    quiet_hours_enabled BOOLEAN DEFAULT false,
    email_digest_frequency VARCHAR(50),
    unsubscribe_all BOOLEAN DEFAULT false,
    unsubscribe_all_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: created_at, updated_at, unsubscribe_all_date
**Indexes**: user_id
**Analytics**: User preference patterns

---

### 2.4 `notification_queue` Table
**Purpose**: Queue for pending notifications

```sql
CREATE TABLE notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES notification_templates(id),
    channel_id UUID NOT NULL REFERENCES notification_channels(id),
    recipient_id UUID,
    recipient_address VARCHAR(500) NOT NULL,
    recipient_type VARCHAR(50) DEFAULT 'USER',
    subject VARCHAR(500),
    message_body TEXT NOT NULL,
    html_body TEXT,
    message_variables JSONB,
    queue_status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    priority INT DEFAULT 5,
    scheduled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    queued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    process_started_at TIMESTAMP WITH TIME ZONE,
    process_completed_at TIMESTAMP WITH TIME ZONE,
    reference_id UUID,
    reference_type VARCHAR(100),
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_queue_status CHECK (queue_status IN ('QUEUED', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED', 'BOUNCED')),
    CONSTRAINT positive_priority CHECK (priority >= 0 AND priority <= 10)
);
```

**Audit Fields**: queued_at, scheduled_at, process_started_at, process_completed_at, created_at, updated_at
**Indexes**: queue_status, scheduled_at, recipient_id, channel_id, template_id, priority
**Partitioning**: By month on queued_at
**Analytics**: Notification throughput, delivery success rate

---

### 2.5 `notification_deliveries` Table
**Purpose**: Immutable record of all notification deliveries

```sql
CREATE TABLE notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_queue_id UUID NOT NULL REFERENCES notification_queue(id),
    delivery_id VARCHAR(255) UNIQUE,
    channel_id UUID NOT NULL REFERENCES notification_channels(id),
    recipient_address VARCHAR(500) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    delivery_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    delivered_timestamp TIMESTAMP WITH TIME ZONE,
    opened_timestamp TIMESTAMP WITH TIME ZONE,
    clicked_timestamp TIMESTAMP WITH TIME ZONE,
    bounced_timestamp TIMESTAMP WITH TIME ZONE,
    bounce_type VARCHAR(50),
    bounce_reason TEXT,
    failed_timestamp TIMESTAMP WITH TIME ZONE,
    failure_code VARCHAR(100),
    failure_reason TEXT,
    provider_message_id VARCHAR(255),
    provider_response JSONB,
    response_time_ms INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_delivery_status CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'OPENED', 'CLICKED', 'FAILED', 'BOUNCED', 'MARKED_AS_SPAM', 'UNSUBSCRIBED'))
);
```

**Audit Fields**: delivery_timestamp (immutable), delivered_timestamp, bounced_timestamp, failed_timestamp
**Immutability**: No UPDATE/DELETE (append-only)
**Indexes**: notification_queue_id, channel_id, delivery_status, delivery_timestamp
**Analytics**: Delivery metrics, channel performance

---

### 2.6 `notification_logs` Table
**Purpose**: Track notification events and debugging

```sql
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_queue_id UUID,
    event_type VARCHAR(100) NOT NULL,
    log_level VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    context JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_log_level CHECK (log_level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL'))
);
```

**Indexes**: notification_queue_id, timestamp DESC, event_type
**Partitioning**: By month on timestamp
**Retention**: 90 days (for debugging)

---

### 2.7 `notification_bounce_list` Table
**Purpose**: Track bounced email addresses

```sql
CREATE TABLE notification_bounce_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email_address VARCHAR(255) NOT NULL UNIQUE,
    bounce_type VARCHAR(50) NOT NULL,
    bounce_reason TEXT,
    bounce_count INT DEFAULT 1,
    first_bounce_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_bounce_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    suppressed BOOLEAN DEFAULT true,
    suppressed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_bounce_type CHECK (bounce_type IN ('HARD', 'SOFT', 'COMPLAINT', 'UNSUBSCRIBED'))
);
```

**Indexes**: email_address, bounce_type, suppressed
**Analytics**: Email quality metrics

---

### 2.8 `notification_unsubscribe_list` Table
**Purpose**: Track unsubscribed recipients

```sql
CREATE TABLE notification_unsubscribe_list (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    email_address VARCHAR(255),
    unsubscribe_reason VARCHAR(200),
    unsubscribe_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    unsubscribed_by VARCHAR(50),
    from_notification_type VARCHAR(100),
    unsubscribe_all BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT email_or_user CHECK ((user_id IS NOT NULL) OR (email_address IS NOT NULL))
);
```

**Indexes**: user_id, email_address, unsubscribe_timestamp
**Analytics**: Unsubscribe trends

---

### 2.9 `audit_logs` Table
**Purpose**: Immutable audit trail of notification operations

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'QUEUE', 'SEND', 'RETRY', 'CANCEL'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
notification_templates (1) ----→ (M) notification_queue ----→ (M) notification_deliveries
notification_channels (1) ----→ (M) notification_queue
notification_channels (1) ----→ (M) notification_deliveries
notification_queue (1) ----→ (M) notification_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_queue_status ON notification_queue(queue_status);
CREATE INDEX idx_queue_scheduled ON notification_queue(scheduled_at);
CREATE INDEX idx_queue_priority ON notification_queue(priority DESC);
CREATE INDEX idx_deliveries_status ON notification_deliveries(delivery_status);
CREATE INDEX idx_deliveries_timestamp ON notification_deliveries(delivery_timestamp DESC);
CREATE INDEX idx_bounce_list_email ON notification_bounce_list(email_address);
CREATE INDEX idx_logs_timestamp ON notification_logs(timestamp DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_queue_status_scheduled ON notification_queue(queue_status, scheduled_at);
CREATE INDEX idx_deliveries_queue_status ON notification_deliveries(notification_queue_id, delivery_status);
```

---

## 5. Constraints & Business Rules

### Queue Processing
```sql
-- Status progression: QUEUED → PROCESSING → SENT → DELIVERED
-- Priority 10 = urgent, 0 = low priority
-- Auto-retry on failure with exponential backoff
```

### Bounce Handling
```sql
-- Hard bounces: immediately suppress
-- Soft bounces: retry up to max_retries
-- Suppress before sending to avoid wasting resources
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V9.0__Initialize_notification_schema.sql
V9.1__Add_channels_and_templates.sql
V9.2__Add_delivery_tracking.sql
V9.3__Add_bounce_and_unsubscribe.sql
V9.4__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily notification delivery summary
- Channel performance metrics
- Template effectiveness metrics
- Bounce rate analysis
- User preference patterns

### ML Feature Inputs
- Email deliverability prediction
- Optimal send time prediction
- Template effectiveness scoring
- Bounce prediction
- Churn prediction from notification engagement

### Business Intelligence
- Notification delivery dashboard
- Channel performance comparison
- Template A/B testing results
- Bounce rate trending
- Unsubscribe rate analysis
- Engagement metrics (open, click rates)

---

## 8. Scalability Considerations

### Partitioning Strategy

**notification_queue (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', queued_at))
```

**notification_deliveries (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', delivery_timestamp))
```

**notification_logs (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', timestamp))
```

---

## 9. Monitoring & Observability

### Key Metrics
- Queue depth (pending notifications)
- Delivery success rate
- Average delivery time
- Bounce rate
- Unsubscribe rate
- Messages per second throughput

### Alerts
- Queue depth exceeds threshold
- Delivery success rate drops
- Channel provider error
- High bounce rate detected
- Retry loop detected

---

## 10. External Integrations

### Brevo (Email Provider)
- Configured in `notification_channels.provider_config`
- API calls for: send, bounce feedback, complaint feedback
- Webhook for delivery updates
- Rate limiting: respect provider limits

### Provider Response Handling
```
200 - Success
429 - Rate limited (retry with backoff)
5xx - Server error (retry)
4xx - Client error (fail permanently)
```

---

## Summary

**Total Tables**: 9  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Immutability**: Deliveries and audit logs (append-only)  
**Queue Processing**: Async with retries and priority  
**Analytics-Ready**: Delivery metrics and engagement tracking  

