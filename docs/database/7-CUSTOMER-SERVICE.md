# Database Specification: Customer Service

**Service**: Customer Service  
**Purpose**: Customer master data, profiles, and relationship management  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Customer Service manages customer master data, contacts, preferences, and relationship history.

### High-Level Architecture
```
customers
├── customer_contacts (1:M)
├── customer_addresses (1:M)
├── customer_preferences (1:1)
├── customer_segments (M:N)
└── customer_interactions (1:M)
```

---

## 2. Tables Specification

### 2.1 `customers` Table
**Purpose**: Store customer master data

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_code VARCHAR(100) UNIQUE NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_type VARCHAR(50) NOT NULL DEFAULT 'RETAIL',
    company_name VARCHAR(255),
    industry VARCHAR(100),
    business_registration_number VARCHAR(100),
    tax_id VARCHAR(100),
    website_url VARCHAR(500),
    total_orders INT DEFAULT 0,
    total_spent DECIMAL(15, 2) DEFAULT 0,
    average_order_value DECIMAL(12, 2) DEFAULT 0,
    lifetime_value DECIMAL(15, 2) DEFAULT 0,
    first_order_date DATE,
    last_order_date DATE,
    days_since_last_order INT,
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    preferred_language VARCHAR(10) DEFAULT 'EN',
    preferred_currency VARCHAR(3) DEFAULT 'USD',
    payment_terms VARCHAR(100),
    credit_limit DECIMAL(15, 2),
    current_credit_balance DECIMAL(15, 2) DEFAULT 0,
    customer_rating DECIMAL(3, 2),
    risk_rating VARCHAR(50) DEFAULT 'LOW',
    account_manager_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT customer_name_not_empty CHECK (customer_name != ''),
    CONSTRAINT valid_customer_type CHECK (customer_type IN ('RETAIL', 'WHOLESALE', 'DISTRIBUTOR', 'CORPORATE', 'GOVERNMENT')),
    CONSTRAINT valid_risk_rating CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: customer_code, customer_name, customer_type, is_active, risk_rating
**Analytics**: Customer lifetime value, customer segments, churn prediction

---

### 2.2 `customer_contacts` Table
**Purpose**: Store customer contact information

```sql
CREATE TABLE customer_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contact_name VARCHAR(255) NOT NULL,
    contact_title VARCHAR(100),
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    mobile_number VARCHAR(20),
    fax_number VARCHAR(20),
    contact_type VARCHAR(50) DEFAULT 'GENERAL',
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    preferred_contact_method VARCHAR(50),
    last_contacted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT contact_name_not_empty CHECK (contact_name != '')
);
```

**Audit Fields**: created_at, updated_at, last_contacted_at
**Indexes**: customer_id, is_primary, contact_type
**Analytics**: Contact interaction history

---

### 2.3 `customer_addresses` Table
**Purpose**: Store customer addresses (billing, shipping, etc.)

```sql
CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city VARCHAR(100) NOT NULL,
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country_code VARCHAR(2) NOT NULL DEFAULT 'US',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    address_type VARCHAR(50) NOT NULL DEFAULT 'SHIPPING',
    is_default BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_address_type CHECK (address_type IN ('BILLING', 'SHIPPING', 'SERVICE', 'OTHER'))
);
```

**Audit Fields**: created_at, updated_at
**Indexes**: customer_id, address_type, is_default
**Analytics**: Shipping location patterns, geographic distribution

---

### 2.4 `customer_preferences` Table
**Purpose**: Store customer communication and service preferences

```sql
CREATE TABLE customer_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    receive_email_communications BOOLEAN DEFAULT true,
    receive_sms_communications BOOLEAN DEFAULT false,
    receive_phone_communications BOOLEAN DEFAULT true,
    receive_product_updates BOOLEAN DEFAULT true,
    receive_promotional_offers BOOLEAN DEFAULT true,
    receive_order_notifications BOOLEAN DEFAULT true,
    receive_shipping_updates BOOLEAN DEFAULT true,
    preferred_contact_method VARCHAR(50) DEFAULT 'EMAIL',
    preferred_communication_time VARCHAR(50),
    do_not_contact_until TIMESTAMP WITH TIME ZONE,
    newsletter_subscribed BOOLEAN DEFAULT true,
    communication_frequency VARCHAR(50) DEFAULT 'AS_NEEDED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: created_at, updated_at
**Indexes**: customer_id

---

### 2.5 `customer_segments` Table (M:N)
**Purpose**: Many-to-many relationship between customers and segments

```sql
CREATE TABLE customer_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    segment_code VARCHAR(100) NOT NULL,
    segment_name VARCHAR(255) NOT NULL,
    segment_type VARCHAR(50),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by UUID NOT NULL,
    removed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_customer_segment UNIQUE (customer_id, segment_code) WHERE removed_at IS NULL
);
```

**Audit Fields**: assigned_at, assigned_by, removed_at, created_at, updated_at
**Indexes**: customer_id, segment_code, segment_type
**Analytics**: Customer segmentation analysis

---

### 2.6 `customer_interactions` Table
**Purpose**: Track customer interactions and communications

```sql
CREATE TABLE customer_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    interaction_type VARCHAR(100) NOT NULL,
    interaction_channel VARCHAR(50) NOT NULL,
    interaction_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    subject VARCHAR(255),
    description TEXT,
    interaction_status VARCHAR(50) DEFAULT 'COMPLETED',
    contacted_by UUID,
    notes TEXT,
    reference_id UUID,
    sentiment VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT valid_interaction_type CHECK (interaction_type IN ('INQUIRY', 'COMPLAINT', 'FEEDBACK', 'SUPPORT', 'SALES', 'OTHER')),
    CONSTRAINT valid_interaction_channel CHECK (interaction_channel IN ('EMAIL', 'PHONE', 'CHAT', 'SOCIAL_MEDIA', 'IN_PERSON', 'SURVEY'))
);
```

**Audit Fields**: interaction_date, created_at, updated_at
**Indexes**: customer_id, interaction_date DESC, interaction_type, interaction_channel
**Analytics**: Customer sentiment analysis, interaction patterns

---

### 2.7 `customer_credit_history` Table
**Purpose**: Track customer credit transactions

```sql
CREATE TABLE customer_credit_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    transaction_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    order_id UUID,
    credit_applied DECIMAL(15, 2),
    credit_available DECIMAL(15, 2),
    reference_number VARCHAR(100),
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT positive_credit CHECK (credit_applied > 0),
    CONSTRAINT valid_transaction_type CHECK (transaction_type IN ('ORDER_PLACED', 'PAYMENT_RECEIVED', 'CREDIT_ADJUSTMENT', 'REFUND', 'CHARGE_BACK'))
);
```

**Audit Fields**: transaction_date, created_by, created_at
**Indexes**: customer_id, transaction_date DESC, transaction_type
**Analytics**: Credit risk analysis, payment patterns

---

### 2.8 `customer_segments_master` Table
**Purpose**: Define available customer segments

```sql
CREATE TABLE customer_segments_master (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_code VARCHAR(100) UNIQUE NOT NULL,
    segment_name VARCHAR(255) NOT NULL,
    segment_description TEXT,
    segment_type VARCHAR(50) NOT NULL,
    segment_criteria JSONB,
    auto_assign BOOLEAN DEFAULT false,
    priority INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, created_by
**Indexes**: segment_code, segment_type, is_active
**Analytics**: Segment distribution, auto-assignment performance

---

### 2.9 `audit_logs` Table
**Purpose**: Immutable audit trail of customer changes

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
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'SEGMENT_ADDED', 'SEGMENT_REMOVED', 'CREDIT_ADJUSTED', 'STATUS_CHANGED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE operations
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
customers (1) ----→ (M) customer_contacts
         ├──→ (M) customer_addresses
         ├──→ (1) customer_preferences
         ├──→ (M) customer_segments ----→ (M) customer_segments_master
         ├──→ (M) customer_interactions
         ├──→ (M) customer_credit_history
         └──→ (M) audit_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_customers_code ON customers(customer_code);
CREATE INDEX idx_customers_name ON customers(customer_name);
CREATE INDEX idx_customers_type ON customers(customer_type);
CREATE INDEX idx_customers_active ON customers(is_active);
CREATE INDEX idx_customers_risk ON customers(risk_rating);
CREATE INDEX idx_contacts_customer ON customer_contacts(customer_id);
CREATE INDEX idx_addresses_customer ON customer_addresses(customer_id);
CREATE INDEX idx_interactions_date ON customer_interactions(customer_id, interaction_date DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_customers_ltv ON customers(lifetime_value DESC) WHERE is_active = true;
CREATE INDEX idx_segments_customer ON customer_segments(customer_id) WHERE removed_at IS NULL;
```

---

## 5. Constraints & Business Rules

### Customer Lifecycle
```sql
-- Customer created with is_active = true
-- first_order_date set on first order
-- last_order_date updated on each order
-- days_since_last_order calculated daily
```

### Credit Management
```sql
-- current_credit_balance = credit_limit - (sum of all charges)
-- Cannot exceed credit_limit
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V7.0__Initialize_customer_schema.sql
V7.1__Add_addresses_and_preferences.sql
V7.2__Add_segments_and_interactions.sql
V7.3__Add_credit_history.sql
V7.4__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily customer snapshots
- Customer lifetime value tracking
- Customer segment distribution
- Interaction history
- Credit usage patterns

### ML Feature Inputs
- Customer RFM (Recency, Frequency, Monetary)
- Churn prediction signals
- Lifetime value prediction
- Segment classification
- Product recommendation features
- Credit risk scoring

### Business Intelligence
- Customer analytics dashboard
- Lifetime value analysis
- Segment performance metrics
- Interaction sentiment analysis
- Payment behavior analysis
- Geographic distribution analysis

### CRM Analytics
- Customer acquisition cost
- Customer retention rate
- Churn rate by segment
- Net Promoter Score (NPS)
- Customer satisfaction trends

---

## 8. Scalability Considerations

### Partitioning Strategy

**customer_interactions (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', interaction_date))
```

**customer_credit_history (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', transaction_date))
```

---

## 9. Monitoring & Observability

### Key Metrics
- Total active customers
- Average customer lifetime value
- Customer acquisition rate
- Churn rate
- Average order frequency
- Customer satisfaction score

### Alerts
- High-value customer inactivity
- Credit limit exceeded
- Repeated complaints on customer
- Churn risk detected
- Customer segment migration

---

## Summary

**Total Tables**: 9  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**360 View**: Complete customer profile with history  
**Analytics-Ready**: Lifetime value and segmentation data  

