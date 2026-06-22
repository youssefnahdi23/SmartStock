# 5. Supplier Service

**Bounded Context:** Supplier Management  
**Database:** `supplier_db` (PostgreSQL)  
**Port:** 8005  
**Team:** Procurement  

---

## Purpose

The Supplier Service is the authoritative source for supplier relationships, performance metrics, and procurement history.

---

## Responsibilities

- Supplier master data (profiles, contacts, agreements)
- Supplier performance tracking (on-time delivery, quality)
- Purchase history aggregation
- Supplier rating and scoring
- Delivery schedule management
- Supplier agreement terms

---

## Database Ownership

**Schema:** `supplier_db`

**Core Tables:**
```sql
suppliers (
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  contact_person VARCHAR,
  email VARCHAR,
  phone VARCHAR,
  address TEXT,
  payment_terms VARCHAR,
  rating DECIMAL,
  status ENUM ('active', 'inactive', 'suspended'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

supplier_performance (
  id UUID PRIMARY KEY,
  supplier_id UUID NOT NULL,
  period_start DATE,
  period_end DATE,
  on_time_delivery_rate DECIMAL,
  quality_score DECIMAL,
  response_time_hours INT,
  order_count INT,
  total_spend DECIMAL
)

supplier_contacts (
  id UUID PRIMARY KEY,
  supplier_id UUID NOT NULL,
  contact_name VARCHAR,
  contact_role VARCHAR,
  email VARCHAR,
  phone VARCHAR
)

delivery_schedules (
  id UUID PRIMARY KEY,
  supplier_id UUID NOT NULL,
  product_id UUID,
  frequency VARCHAR,
  lead_time_days INT,
  minimum_order_qty INT
)

supplier_agreements (
  id UUID PRIMARY KEY,
  supplier_id UUID NOT NULL,
  agreement_type VARCHAR,
  terms TEXT,
  effective_from DATE,
  effective_to DATE
)
```

---

## Events Published

### 1. SupplierCreated
**When:** New supplier registered  
**Consumers:** Audit Service

### 2. SupplierUpdated
**When:** Supplier details changed  
**Consumers:** Audit, Reporting Services

### 3. PerformanceScoreUpdated
**When:** Performance metrics calculated  
**Consumers:** Reporting Service

### 4. DeliveryScheduleUpdated
**When:** Schedule terms changed  
**Consumers:** Purchase Order Service

---

## Events Consumed

### From Purchase Order Service
- **PurchaseOrderCreated:** Track order for performance metrics
- **PurchaseOrderReceived:** Update on-time delivery metrics
- **DeliveryDelayed:** Track delivery issues

---

## REST APIs

**Base URL:** `/api/v1/suppliers`

### Supplier Management
- `GET /suppliers` - List suppliers (paginated)
- `POST /suppliers` - Create supplier
- `GET /suppliers/{supplierId}` - Get supplier details
- `PUT /suppliers/{supplierId}` - Update supplier
- `DELETE /suppliers/{supplierId}` - Deactivate supplier

### Performance Analytics
- `GET /suppliers/{supplierId}/performance` - Get performance metrics
- `GET /suppliers/{supplierId}/performance/history` - Historical metrics
- `GET /suppliers/by-rating` - Top-rated suppliers

### Contacts
- `GET /suppliers/{supplierId}/contacts` - List contacts
- `POST /suppliers/{supplierId}/contacts` - Add contact

### Delivery Schedules
- `GET /suppliers/{supplierId}/schedules` - Get delivery schedules
- `PUT /suppliers/{supplierId}/schedules` - Update schedules

---

## Dependencies

**Event Sources:**
- Purchase Order Service

---

## Future Scalability

### Performance Analytics
- Real-time KPI calculations
- Predictive supplier reliability models
- Automated supplier ranking

### Risk Scoring
- Assess supplier risk factors
- Predict delivery delays
- Quality issue prediction

---

## Deployment Checklist

- [ ] `supplier_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Event subscription configured
- [ ] Performance calculation scheduled
- [ ] Monitoring/alerting configured

