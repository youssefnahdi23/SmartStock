# 6. Customer Service

**Bounded Context:** Customer Management  
**Database:** `customer_db` (PostgreSQL)  
**Port:** 8006  
**Team:** Sales & Customer Success  

---

## Purpose

The Customer Service is the authoritative source for customer relationships, segmentation, and credit profiles.

---

## Responsibilities

- Customer master data (profiles, contacts)
- Customer segmentation (VIP, wholesale, retail)
- Credit profile and limit management
- Sales history aggregation
- Customer preferences
- Contact information management

---

## Database Ownership

**Schema:** `customer_db`

**Core Tables:**
```sql
customers (
  id UUID PRIMARY KEY,
  name VARCHAR NOT NULL,
  email VARCHAR,
  phone VARCHAR,
  address TEXT,
  segment ENUM ('retail', 'wholesale', 'vip', 'inactive'),
  credit_limit DECIMAL,
  current_balance DECIMAL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

customer_contacts (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  contact_name VARCHAR,
  contact_role VARCHAR,
  email VARCHAR,
  phone VARCHAR
)

credit_profiles (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  credit_limit DECIMAL,
  current_used DECIMAL,
  available DECIMAL,
  credit_rating VARCHAR,
  last_evaluated_at TIMESTAMP
)

customer_preferences (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  preferred_payment_method VARCHAR,
  preferred_delivery_days VARCHAR,
  special_requirements TEXT
)

sales_history (
  id UUID PRIMARY KEY,
  customer_id UUID NOT NULL,
  sale_date DATE,
  amount DECIMAL,
  order_count INT,
  last_order_date DATE
)
```

---

## Events Published

### 1. CustomerCreated
**When:** New customer registered  
**Consumers:** Audit, Notification Services

### 2. CustomerUpdated
**When:** Customer details changed  
**Consumers:** Audit Service

### 3. CreditLimitUpdated
**When:** Credit limit modified  
**Consumers:** Sales Order Service (validation)

### 4. SegmentationChanged
**When:** Customer segment updated  
**Consumers:** Reporting Service

---

## Events Consumed

### From Sales Order Service
- **SalesOrderCreated:** Track sales history, update credit used
- **OrderShipped:** Update last order date

---

## REST APIs

**Base URL:** `/api/v1/customers`

### Customer Management
- `GET /customers` - List customers (paginated)
- `POST /customers` - Create customer
- `GET /customers/{customerId}` - Get customer details
- `PUT /customers/{customerId}` - Update customer

### Credit Management
- `GET /customers/{customerId}/credit` - Get credit profile
- `PUT /customers/{customerId}/credit/limit` - Update credit limit
- `GET /customers/{customerId}/credit/balance` - Current balance

### Sales History
- `GET /customers/{customerId}/sales-history` - Sales transactions
- `GET /customers/{customerId}/orders` - Order history

### Segmentation
- `GET /customers/by-segment` - List by segment
- `GET /customers/vip` - Top-tier customers

---

## Dependencies

**Event Sources:**
- Sales Order Service

---

## Future Scalability

### Customer Analytics
- Purchase pattern analysis
- Churn prediction models
- Lifetime value calculations
- Recommendation engines

### Multi-Tenancy
- Support multi-tenant customer management
- Per-tenant customer segmentation

---

## Deployment Checklist

- [ ] `customer_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured
- [ ] Credit limit policies defined
- [ ] Monitoring/alerting configured

