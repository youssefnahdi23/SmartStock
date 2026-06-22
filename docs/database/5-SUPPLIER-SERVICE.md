# Database Specification: Supplier Service

**Service**: Supplier Service  
**Purpose**: Supplier master data, contracts, and performance tracking  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Supplier Service manages supplier relationships, contracts, performance metrics, and delivery history.

### High-Level Architecture
```
suppliers
├── supplier_contacts (1:M)
├── supplier_products (M:N)
├── supplier_contracts (1:M)
├── supplier_deliveries (1:M)
└── supplier_metrics (1:M)
```

---

## 2. Tables Specification

### 2.1 `suppliers` Table
**Purpose**: Store supplier master data

```sql
CREATE TABLE suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_code VARCHAR(100) UNIQUE NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_type VARCHAR(50) NOT NULL DEFAULT 'VENDOR',
    business_registration_number VARCHAR(100),
    tax_id VARCHAR(100),
    website_url VARCHAR(500),
    payment_terms VARCHAR(100),
    currency_code VARCHAR(3) DEFAULT 'USD',
    country_code VARCHAR(2) NOT NULL DEFAULT 'US',
    headquarter_address TEXT,
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    primary_contact_id UUID,
    account_manager_id UUID,
    credit_limit DECIMAL(15, 2),
    average_lead_time_days INT DEFAULT 7,
    minimum_order_quantity INT DEFAULT 1,
    minimum_order_value DECIMAL(12, 2),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    verification_date TIMESTAMP WITH TIME ZONE,
    risk_rating VARCHAR(50) DEFAULT 'MEDIUM',
    rating DECIMAL(3, 2),
    total_orders INT DEFAULT 0,
    total_spent DECIMAL(15, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT supplier_name_not_empty CHECK (supplier_name != ''),
    CONSTRAINT valid_supplier_type CHECK (supplier_type IN ('VENDOR', 'DISTRIBUTOR', 'MANUFACTURER', 'WHOLESALER', 'AGENT')),
    CONSTRAINT valid_risk_rating CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by, verification_date
**Indexes**: supplier_code, supplier_name, is_active, is_verified, risk_rating
**Analytics**: Supplier concentration, relationship value, risk profile

---

### 2.2 `supplier_contacts` Table
**Purpose**: Store contact information for suppliers

```sql
CREATE TABLE supplier_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    contact_name VARCHAR(255) NOT NULL,
    contact_title VARCHAR(100),
    email_address VARCHAR(255),
    phone_number VARCHAR(20),
    mobile_number VARCHAR(20),
    address_line1 TEXT,
    address_line2 TEXT,
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    contact_type VARCHAR(50) DEFAULT 'GENERAL',
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    last_contacted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT contact_name_not_empty CHECK (contact_name != '')
);
```

**Audit Fields**: created_at, updated_at, last_contacted_at
**Indexes**: supplier_id, is_primary, contact_type
**Analytics**: Contact relationship history

---

### 2.3 `supplier_products` Table
**Purpose**: Many-to-many relationship between suppliers and products

```sql
CREATE TABLE supplier_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    supplier_product_code VARCHAR(255),
    unit_price DECIMAL(12, 2) NOT NULL,
    minimum_order_quantity INT DEFAULT 1,
    lead_time_days INT DEFAULT 7,
    quality_rating DECIMAL(3, 2),
    is_active BOOLEAN DEFAULT true,
    last_ordered_at TIMESTAMP WITH TIME ZONE,
    total_quantity_ordered INT DEFAULT 0,
    total_spent DECIMAL(15, 2) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_supplier_product UNIQUE (supplier_id, product_id),
    CONSTRAINT positive_unit_price CHECK (unit_price > 0),
    CONSTRAINT positive_lead_time CHECK (lead_time_days >= 0)
);
```

**Audit Fields**: created_at, updated_at, last_ordered_at
**Indexes**: supplier_id, product_id, is_active
**Analytics**: Supplier product mix, pricing history

---

### 2.4 `supplier_contracts` Table
**Purpose**: Store supplier contracts and agreements

```sql
CREATE TABLE supplier_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    contract_number VARCHAR(100) UNIQUE NOT NULL,
    contract_title VARCHAR(255) NOT NULL,
    contract_type VARCHAR(50) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    renewal_date DATE,
    contract_value DECIMAL(15, 2),
    payment_terms VARCHAR(100),
    discount_percentage DECIMAL(5, 2) DEFAULT 0,
    minimum_volume INT,
    contract_status VARCHAR(50) DEFAULT 'ACTIVE',
    approval_status VARCHAR(50) DEFAULT 'PENDING',
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT contract_title_not_empty CHECK (contract_title != ''),
    CONSTRAINT valid_contract_type CHECK (contract_type IN ('PURCHASE_AGREEMENT', 'MASTER_SUPPLY', 'FRAMEWORK', 'SPOT', 'BLANKET')),
    CONSTRAINT valid_contract_status CHECK (contract_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'EXPIRED', 'TERMINATED')),
    CONSTRAINT contract_dates CHECK (start_date <= end_date)
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by, approved_at, approved_by
**Indexes**: supplier_id, contract_number, contract_status, end_date
**Analytics**: Contract terms analysis, renewal tracking

---

### 2.5 `supplier_deliveries` Table
**Purpose**: Track supplier delivery history and performance

```sql
CREATE TABLE supplier_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    purchase_order_id UUID,
    delivery_number VARCHAR(100) UNIQUE NOT NULL,
    order_date DATE NOT NULL,
    promised_delivery_date DATE NOT NULL,
    actual_delivery_date DATE,
    quantity_ordered INT NOT NULL,
    quantity_received INT NOT NULL,
    quantity_rejected INT DEFAULT 0,
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    on_time BOOLEAN,
    on_time_days_variance INT,
    quality_inspection_status VARCHAR(50) DEFAULT 'PENDING',
    quality_issues_found INT DEFAULT 0,
    quality_rating DECIMAL(3, 2),
    carrier_name VARCHAR(255),
    tracking_number VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    received_by UUID,
    CONSTRAINT valid_delivery_status CHECK (delivery_status IN ('PENDING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT valid_inspection_status CHECK (quality_inspection_status IN ('PENDING', 'PASSED', 'FAILED', 'PARTIAL'))
);
```

**Audit Fields**: created_at, updated_at, received_by
**Indexes**: supplier_id, delivery_number, promised_delivery_date, actual_delivery_date, delivery_status
**Analytics**: On-time delivery rate, quality metrics, lead time accuracy

---

### 2.6 `supplier_metrics` Table
**Purpose**: Daily supplier performance metrics

```sql
CREATE TABLE supplier_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    total_orders INT,
    total_units_received INT,
    on_time_deliveries INT,
    on_time_delivery_rate DECIMAL(5, 2),
    quality_pass_rate DECIMAL(5, 2),
    average_quality_rating DECIMAL(3, 2),
    quality_issues_count INT,
    order_accuracy_rate DECIMAL(5, 2),
    average_lead_time_days DECIMAL(8, 2),
    total_value_received DECIMAL(15, 2),
    communication_score DECIMAL(3, 2),
    overall_performance_score DECIMAL(3, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_supplier_metric UNIQUE (supplier_id, metric_date)
);
```

**Audit Fields**: created_at
**Indexes**: supplier_id, metric_date
**Analytics**: Supplier scorecard, KPI tracking

---

### 2.7 `supplier_risk_assessment` Table
**Purpose**: Track supplier risk factors and assessments

```sql
CREATE TABLE supplier_risk_assessment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    assessment_date DATE NOT NULL,
    financial_health_score DECIMAL(3, 2),
    delivery_reliability_score DECIMAL(3, 2),
    quality_consistency_score DECIMAL(3, 2),
    communication_responsiveness_score DECIMAL(3, 2),
    compliance_score DECIMAL(3, 2),
    overall_risk_score DECIMAL(3, 2),
    risk_level VARCHAR(50),
    key_risks TEXT,
    mitigation_actions TEXT,
    next_assessment_date DATE,
    assessed_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_assessment UNIQUE (supplier_id, assessment_date),
    CONSTRAINT valid_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);
```

**Audit Fields**: created_at, updated_at, assessed_by
**Indexes**: supplier_id, assessment_date, risk_level
**Analytics**: Supplier risk profile, risk trends

---

### 2.8 `audit_logs` Table
**Purpose**: Immutable audit trail of supplier changes

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
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'VERIFY', 'RISK_ASSESSMENT'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id
**Immutability**: No UPDATE/DELETE operations
**Indexes**: entity_type, entity_id, timestamp
**Retention**: Indefinite

---

## 3. Relationships & Foreign Keys

```
suppliers (1) ----→ (M) supplier_contacts
         ├──→ (M) supplier_products
         ├──→ (M) supplier_contracts
         ├──→ (M) supplier_deliveries
         ├──→ (M) supplier_metrics
         ├──→ (M) supplier_risk_assessment
         └──→ (M) audit_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_suppliers_code ON suppliers(supplier_code);
CREATE INDEX idx_suppliers_name ON suppliers(supplier_name);
CREATE INDEX idx_suppliers_active ON suppliers(is_active);
CREATE INDEX idx_suppliers_verified ON suppliers(is_verified);
CREATE INDEX idx_suppliers_risk ON suppliers(risk_rating);
CREATE INDEX idx_deliveries_date ON supplier_deliveries(promised_delivery_date, actual_delivery_date);
CREATE INDEX idx_metrics_date ON supplier_metrics(supplier_id, metric_date DESC);
```

### Composite Indexes
```sql
CREATE INDEX idx_supplier_product ON supplier_products(supplier_id, product_id);
CREATE INDEX idx_deliveries_supplier_status ON supplier_deliveries(supplier_id, delivery_status, on_time);
```

---

## 5. Constraints & Business Rules

### Contract Management
```sql
-- Contract must have end_date >= start_date
-- Cannot activate contract before start_date
-- Renewal reminders at 90 days before end_date
```

### Performance Tracking
```sql
-- On-time calculation: actual_delivery_date <= promised_delivery_date
-- Quality metrics calculated daily from deliveries
-- Risk assessment scores 0-10
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V5.0__Initialize_supplier_schema.sql
V5.1__Add_contracts_and_deliveries.sql
V5.2__Add_metrics_and_risk_assessment.sql
V5.3__Add_performance_indexes.sql
```

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily supplier performance metrics
- Delivery history with quality metrics
- Contract terms and status
- Risk assessment history
- Supplier relationship value

### ML Feature Inputs
- Delivery reliability prediction
- Quality issue prediction
- Supplier performance clustering
- Recommendation engine (alternative suppliers)
- Price elasticity analysis
- Lead time forecasting

### Business Intelligence
- Supplier scorecard dashboards
- On-time delivery tracking
- Quality metrics trending
- Cost analysis (cost per unit, total spend)
- Risk profile analysis
- Supplier diversification metrics

### Procurement Analytics
- Spend analysis by supplier
- Category spend distribution
- Contract compliance metrics
- Vendor concentration analysis
- Procurement efficiency metrics

---

## 8. Scalability Considerations

### Partitioning Strategy

**supplier_metrics (Time-based)**:
```sql
PARTITION BY RANGE (metric_date)
```

**supplier_deliveries (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', order_date))
```

---

## 9. Monitoring & Observability

### Key Metrics
- Total active suppliers
- Average on-time delivery %
- Average quality pass rate
- Supplier concentration
- Total spend by supplier

### Alerts
- Delivery past due
- Quality issues spike
- Risk rating upgrade needed
- Contract renewal approaching
- Supplier inactive (no orders)

---

## Summary

**Total Tables**: 8  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Performance Tracking**: Daily metrics and KPIs  
**Analytics-Ready**: Complete delivery history and scorecard data  

