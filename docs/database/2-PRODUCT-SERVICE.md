# Database Specification: Product Service

**Service**: Product Service  
**Purpose**: Product master data, categorization, barcodes, and metadata  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Product Service manages product master data, barcodes, QR codes, categories, and product specifications.

### High-Level Architecture
```
products
├── product_categories (M:N)
├── product_barcodes (1:M)
├── product_attributes (1:M)
├── product_skus (1:M)
└── product_images (1:M)

categories
└── category_hierarchy (1:M)
```

---

## 2. Tables Specification

### 2.1 `products` Table
**Purpose**: Store product master data

```sql
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(255) UNIQUE NOT NULL,
    product_name VARCHAR(500) NOT NULL,
    product_description TEXT,
    manufacturer VARCHAR(255),
    brand VARCHAR(255),
    unit_of_measure VARCHAR(50) NOT NULL DEFAULT 'UNIT',
    standard_cost DECIMAL(12, 2) NOT NULL DEFAULT 0,
    standard_retail_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    weight DECIMAL(10, 3),
    length DECIMAL(10, 3),
    width DECIMAL(10, 3),
    height DECIMAL(10, 3),
    is_active BOOLEAN DEFAULT true,
    is_discontinued BOOLEAN DEFAULT false,
    discontinued_at TIMESTAMP WITH TIME ZONE,
    reorder_level INT DEFAULT 10,
    reorder_quantity INT DEFAULT 50,
    lifecycle_status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    CONSTRAINT product_name_not_empty CHECK (product_name != ''),
    CONSTRAINT valid_lifecycle_status CHECK (lifecycle_status IN ('ACTIVE', 'OBSOLETE', 'DISCONTINUED', 'ARCHIVED')),
    CONSTRAINT positive_cost CHECK (standard_cost >= 0),
    CONSTRAINT positive_price CHECK (standard_retail_price >= 0)
);
```

**Audit Fields**: created_at, updated_at, deleted_at, created_by, updated_by
**Indexes**: sku, product_name, is_active, lifecycle_status, created_at
**Analytics**: Product lifecycle, price trends, cost analysis

---

### 2.2 `categories` Table
**Purpose**: Product categorization hierarchy

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    parent_category_id UUID REFERENCES categories(id),
    category_level INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: category_name, parent_category_id, is_active
**Constraint**: Prevent circular hierarchy

---

### 2.3 `product_categories` Table
**Purpose**: Many-to-many relationship between products and categories

```sql
CREATE TABLE product_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT false,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_product_category UNIQUE (product_id, category_id)
);
```

**Audit Fields**: assigned_at, assigned_by, created_at, updated_at
**Indexes**: product_id, category_id, is_primary
**Analytics**: Category distribution, cross-selling patterns

---

### 2.4 `product_barcodes` Table
**Purpose**: Store barcode and QR code information

```sql
CREATE TABLE product_barcodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    barcode_value VARCHAR(255) UNIQUE NOT NULL,
    barcode_type VARCHAR(50) NOT NULL,
    barcode_format VARCHAR(50) DEFAULT 'EAN13',
    qr_code_data TEXT,
    qr_code_image_url VARCHAR(500),
    barcode_image_url VARCHAR(500),
    is_primary BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    scanned_count INT DEFAULT 0,
    last_scanned_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT valid_barcode_type CHECK (barcode_type IN ('EAN13', 'EAN8', 'UPCA', 'UPCE', 'CODE128', 'CODE39', 'QR'))
);
```

**Audit Fields**: created_at, updated_at, created_by, last_scanned_at
**Indexes**: barcode_value, product_id, barcode_type
**Analytics**: Barcode scan patterns, product scan history

---

### 2.5 `product_attributes` Table
**Purpose**: Store flexible product attributes

```sql
CREATE TABLE product_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_name VARCHAR(255) NOT NULL,
    attribute_value VARCHAR(500) NOT NULL,
    attribute_type VARCHAR(50) DEFAULT 'TEXT',
    sort_order INT DEFAULT 0,
    is_searchable BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, updated_by
**Indexes**: product_id, attribute_name, is_searchable
**Analytics**: Attribute value distribution, product specifications analysis

---

### 2.6 `product_skus` Table
**Purpose**: Store product SKU variants (size, color, etc.)

```sql
CREATE TABLE product_skus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_value VARCHAR(255) UNIQUE NOT NULL,
    sku_name VARCHAR(255) NOT NULL,
    variant_code VARCHAR(100),
    size VARCHAR(50),
    color VARCHAR(50),
    configuration JSONB,
    unit_cost DECIMAL(12, 2) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    weight DECIMAL(10, 3),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: sku_value, product_id, is_active
**Analytics**: SKU performance, variant popularity

---

### 2.7 `product_images` Table
**Purpose**: Store product images and media

```sql
CREATE TABLE product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    image_type VARCHAR(50) DEFAULT 'PRODUCT',
    display_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    alt_text VARCHAR(500),
    file_size INT,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: created_at, updated_at, uploaded_by
**Indexes**: product_id, is_primary

---

### 2.8 `audit_logs` Table
**Purpose**: Immutable audit trail of product changes

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL DEFAULT 'PRODUCT',
    entity_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    change_summary TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'DISCONTINUE', 'RESTORE', 'BULK_IMPORT'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id, request_id
**Immutability**: No UPDATE/DELETE operations allowed
**Indexes**: entity_id, timestamp, event_type
**Retention**: Indefinite
**Analytics**: Product change history, audit trail

---

## 3. Relationships & Foreign Keys

```
products (1) ----→ (M) product_categories ----→ (M) categories
            ├──→ (M) product_barcodes
            ├──→ (M) product_attributes
            ├──→ (M) product_skus
            ├──→ (M) product_images
            └──→ (M) audit_logs
```

---

## 4. Indexing Strategy

### Performance Indexes
```sql
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_active ON products(is_active) WHERE is_active = true;
CREATE INDEX idx_products_lifecycle ON products(lifecycle_status);
CREATE INDEX idx_product_barcodes_value ON product_barcodes(barcode_value);
CREATE INDEX idx_product_skus_value ON product_skus(sku_value);
```

### Composite Indexes
```sql
CREATE INDEX idx_product_category_active ON product_categories(product_id, is_primary) WHERE product_id IS NOT NULL;
CREATE INDEX idx_audit_logs_product ON audit_logs(entity_id, timestamp DESC) WHERE entity_type = 'PRODUCT';
```

---

## 5. Constraints & Business Rules

### Unique Constraints
```sql
ALTER TABLE products ADD CONSTRAINT uq_sku UNIQUE (sku);
ALTER TABLE product_barcodes ADD CONSTRAINT uq_barcode_value UNIQUE (barcode_value);
ALTER TABLE product_skus ADD CONSTRAINT uq_sku_value UNIQUE (sku_value);
```

### Check Constraints
```sql
ALTER TABLE products ADD CONSTRAINT ck_cost_positive CHECK (standard_cost >= 0);
ALTER TABLE products ADD CONSTRAINT ck_price_positive CHECK (standard_retail_price >= 0);
ALTER TABLE product_skus ADD CONSTRAINT ck_sku_cost_positive CHECK (unit_cost >= 0);
ALTER TABLE product_skus ADD CONSTRAINT ck_sku_price_positive CHECK (unit_price >= 0);
```

---

## 6. Migration Strategy

### Flyway Versioning
```
V2.0__Initialize_product_schema.sql
V2.1__Add_barcodes_and_skus.sql
V2.2__Add_attributes_and_images.sql
V2.3__Add_audit_logs.sql
V2.4__Add_performance_indexes.sql
```

### Bulk Import Considerations
- Batch insert up to 1000 records per transaction
- Disable indexes during bulk import, rebuild after
- Use COPY command for CSV imports

---

## 7. Future Analytics Considerations

### Data Warehouse Exports
- Daily product catalog snapshot
- Category hierarchy snapshots
- Product price history
- Barcode scan analytics
- SKU performance metrics

### ML Feature Inputs
- Product attributes for recommendations
- Price elasticity analysis
- Category classification
- Product lifecycle stage
- Scan frequency patterns

### Business Intelligence
- Product profitability analysis
- Category performance metrics
- Slow-moving inventory detection
- New product adoption tracking
- Barcode scan efficiency

---

## 8. Scalability Considerations

### Partitioning Strategy

**audit_logs (Time-based)**:
```sql
PARTITION BY RANGE (DATE_TRUNC('month', timestamp))
```

**product_barcodes (Hash)**:
```sql
PARTITION BY HASH (product_id)
```

### Query Optimization
- Full-text search on product_name: Consider pg_trgm extension
- Category hierarchy: Materialized view for fast lookups
- Image URLs: CDN integration with URL versioning

---

## 9. Monitoring & Observability

### Key Metrics
- Total active products
- Product creation rate
- Barcode scan rate
- Category distribution
- Discontinued products percentage

### Alerts
- Duplicate barcode detection
- Missing category for product
- SKU pricing inconsistencies

---

## Summary

**Total Tables**: 8  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Soft Deletes**: Yes  
**Catalog Size Support**: 1M+ products  

