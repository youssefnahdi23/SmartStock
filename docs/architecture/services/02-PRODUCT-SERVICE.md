# 2. Product Service

**Bounded Context:** Product Catalog  
**Database:** `product_db` (PostgreSQL)  
**Port:** 8002  
**Team:** Product Management  

---

## Purpose

The Product Service is the authoritative source for product master data across the enterprise. It manages product definitions, SKUs, categorization, and pricing.

---

## Responsibilities

- Product master data management (CRUD)
- SKU generation and management
- Barcode/QR code generation
- Product categorization and hierarchies
- Pricing management (cost, retail, wholesale)
- Product variants and options
- Import/export functionality (CSV, JSON)
- Product lifecycle management (active, discontinued)

---

## Database Ownership

**Schema:** `product_db`

**Core Tables:**
```sql
products (
  id UUID PRIMARY KEY,
  sku VARCHAR UNIQUE NOT NULL,
  name VARCHAR NOT NULL,
  description TEXT,
  category_id UUID,
  unit_of_measure VARCHAR,
  status ENUM ('active', 'discontinued'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

categories (
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  parent_id UUID,
  created_at TIMESTAMP
)

pricing (
  id UUID PRIMARY KEY,
  product_id UUID,
  cost_price DECIMAL,
  retail_price DECIMAL,
  wholesale_price DECIMAL,
  effective_from TIMESTAMP,
  effective_to TIMESTAMP
)

barcodes (
  id UUID PRIMARY KEY,
  product_id UUID,
  barcode VARCHAR UNIQUE NOT NULL,
  barcode_format VARCHAR,
  created_at TIMESTAMP
)

variants (
  id UUID PRIMARY KEY,
  product_id UUID,
  variant_name VARCHAR,
  sku VARCHAR UNIQUE,
  created_at TIMESTAMP
)
```

---

## Events Published

### 1. ProductCreated
**When:** New product registered  
**Consumers:** Inventory, Warehouse, Reporting Services

### 2. ProductUpdated
**When:** Product details changed  
**Consumers:** Inventory, Warehouse, Reporting Services

### 3. ProductDiscontinued
**When:** Product marked inactive  
**Consumers:** Inventory, Warehouse, Notification Services

### 4. PricingUpdated
**When:** Product pricing changed  
**Consumers:** Reporting, Sales Order Services

### 5. SKUGenerated
**When:** New SKU created  
**Consumers:** Audit Service

---

## Events Consumed

None directly. Product Service is primarily a data source.

---

## REST APIs

**Base URL:** `/api/v1/products`

### Product Management
- `GET /products` - List all products (paginated)
- `POST /products` - Create product
- `GET /products/{productId}` - Get product details
- `PUT /products/{productId}` - Update product
- `DELETE /products/{productId}` - Discontinue product

### SKU Management
- `GET /products/{productId}/skus` - Get SKUs for product
- `POST /products/sku/generate` - Auto-generate SKU

### Pricing
- `GET /products/{productId}/pricing` - Get pricing history
- `PUT /products/{productId}/pricing` - Update pricing

### Categorization
- `GET /categories` - List all categories
- `POST /categories` - Create category
- `GET /categories/{categoryId}/products` - Products in category

### Barcodes
- `POST /products/{productId}/barcodes/generate` - Generate barcode
- `GET /products/{productId}/barcodes` - List barcodes
- `GET /products/by-barcode/{barcode}` - Look up by barcode

### Import/Export
- `POST /products/import` - Bulk import CSV
- `GET /products/export` - Export to CSV/JSON

---

## Dependencies

**Synchronous Calls:**
- Calls Identity Service to validate user permissions

**Consumed By:**
- Inventory Service (validates product exists)
- Warehouse Service (location-product assignments)
- Sales Order Service (order line items)
- Purchase Order Service (PO line items)
- Reporting Service (analytics)

---

## Future Scalability

### Caching
- Product details in Redis (30-min TTL)
- SKU lookup cache
- Pricing cache

### Read Replicas
- Heavy read queries to read replicas
- Writes to master only

### Search Index
- Elasticsearch for full-text product search
- Filter by category, price range, SKU

### Variants & Bundles
- Support product variants (size, color, etc.)
- Support product bundles
- Separate variants table with separate SKUs

---

## Deployment Checklist

- [ ] `product_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Barcode/QR generator library configured
- [ ] Redis caching configured
- [ ] Monitoring/alerting configured
- [ ] Search index configured (future)

