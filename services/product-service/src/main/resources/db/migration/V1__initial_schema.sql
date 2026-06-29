-- Product Service — Initial Schema
-- V1: Core product tables

-- ── Extensions ───────────────────────────────────────────────────────────────

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ── Categories ────────────────────────────────────────────────────────────────

CREATE TABLE categories (
    id                 VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    category_name      VARCHAR(255) NOT NULL,
    description        TEXT,
    parent_category_id VARCHAR(36)  REFERENCES categories(id),
    category_level     INT          NOT NULL DEFAULT 0,
    sort_order         INT          NOT NULL DEFAULT 0,
    icon               VARCHAR(255),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(36)  NOT NULL,
    updated_by         VARCHAR(36)  NOT NULL,
    CONSTRAINT uq_category_name UNIQUE (category_name),
    CONSTRAINT ck_category_level CHECK (category_level BETWEEN 0 AND 5)
);

CREATE INDEX idx_categories_name      ON categories(category_name);
CREATE INDEX idx_categories_parent    ON categories(parent_category_id);
CREATE INDEX idx_categories_is_active ON categories(is_active) WHERE is_active = TRUE;

-- ── Products ──────────────────────────────────────────────────────────────────

CREATE TABLE products (
    id                    VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::text,
    sku                   VARCHAR(255)   NOT NULL,
    product_name          VARCHAR(500)   NOT NULL,
    product_description   TEXT,
    manufacturer          VARCHAR(255),
    brand                 VARCHAR(255),
    unit_of_measure       VARCHAR(50)    NOT NULL DEFAULT 'PIECE',
    standard_cost         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    standard_retail_price DECIMAL(12, 2) NOT NULL DEFAULT 0,
    weight                DECIMAL(10, 3),
    weight_unit           VARCHAR(10),
    length                DECIMAL(10, 3),
    width                 DECIMAL(10, 3),
    height                DECIMAL(10, 3),
    dimension_unit        VARCHAR(10),
    reorder_level         INT            NOT NULL DEFAULT 10,
    reorder_quantity      INT            NOT NULL DEFAULT 50,
    max_stock             INT            NOT NULL DEFAULT 0,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    is_discontinued       BOOLEAN        NOT NULL DEFAULT FALSE,
    discontinued_at       TIMESTAMPTZ,
    lifecycle_status      VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMPTZ,
    created_by            VARCHAR(36)    NOT NULL,
    updated_by            VARCHAR(36)    NOT NULL,
    CONSTRAINT uq_sku                  UNIQUE (sku),
    CONSTRAINT ck_product_name         CHECK (product_name <> ''),
    CONSTRAINT ck_positive_cost        CHECK (standard_cost >= 0),
    CONSTRAINT ck_positive_price       CHECK (standard_retail_price >= 0),
    CONSTRAINT ck_lifecycle_status     CHECK (lifecycle_status IN ('ACTIVE', 'OBSOLETE', 'DISCONTINUED', 'ARCHIVED'))
);

CREATE INDEX idx_products_sku        ON products(sku);
CREATE INDEX idx_products_name       ON products(product_name);
CREATE INDEX idx_products_active     ON products(is_active)       WHERE is_active = TRUE;
CREATE INDEX idx_products_lifecycle  ON products(lifecycle_status);
CREATE INDEX idx_products_created_at ON products(created_at DESC);
CREATE INDEX idx_products_not_deleted ON products(deleted_at)     WHERE deleted_at IS NULL;
CREATE INDEX idx_products_name_trgm  ON products USING gin (product_name gin_trgm_ops);

-- ── Product Categories (M:N) ─────────────────────────────────────────────────

CREATE TABLE product_categories (
    id          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    product_id  VARCHAR(36) NOT NULL REFERENCES products(id)   ON DELETE CASCADE,
    category_id VARCHAR(36) NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_category UNIQUE (product_id, category_id)
);

CREATE INDEX idx_product_categories_product  ON product_categories(product_id);
CREATE INDEX idx_product_categories_category ON product_categories(category_id);
CREATE INDEX idx_product_categories_primary  ON product_categories(product_id, is_primary);

-- ── Product Barcodes ──────────────────────────────────────────────────────────

CREATE TABLE product_barcodes (
    id                 VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    product_id         VARCHAR(36)  NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    barcode_value      VARCHAR(255) NOT NULL,
    barcode_type       VARCHAR(50)  NOT NULL,
    barcode_format     VARCHAR(50)  NOT NULL DEFAULT 'EAN13',
    qr_code_data       TEXT,
    qr_code_image_url  VARCHAR(500),
    barcode_image_url  VARCHAR(500),
    is_primary         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    scanned_count      INT          NOT NULL DEFAULT 0,
    last_scanned_at    TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         VARCHAR(36)  NOT NULL,
    CONSTRAINT uq_barcode_value     UNIQUE (barcode_value),
    CONSTRAINT ck_valid_barcode_type CHECK (barcode_type IN ('EAN13', 'EAN8', 'UPCA', 'UPCE', 'CODE128', 'CODE39', 'QR'))
);

CREATE INDEX idx_barcodes_value      ON product_barcodes(barcode_value);
CREATE INDEX idx_barcodes_product_id ON product_barcodes(product_id);
CREATE INDEX idx_barcodes_type       ON product_barcodes(barcode_type);

-- ── Product Attributes ───────────────────────────────────────────────────────

CREATE TABLE product_attributes (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    product_id      VARCHAR(36)  NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_name  VARCHAR(255) NOT NULL,
    attribute_value VARCHAR(500) NOT NULL,
    attribute_type  VARCHAR(50)  NOT NULL DEFAULT 'TEXT',
    sort_order      INT          NOT NULL DEFAULT 0,
    is_searchable   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(36)  NOT NULL
);

CREATE INDEX idx_attributes_product_id  ON product_attributes(product_id);
CREATE INDEX idx_attributes_name        ON product_attributes(attribute_name);
CREATE INDEX idx_attributes_searchable  ON product_attributes(is_searchable) WHERE is_searchable = TRUE;

-- ── Product SKUs (variants) ──────────────────────────────────────────────────

CREATE TABLE product_skus (
    id           VARCHAR(36)    PRIMARY KEY DEFAULT gen_random_uuid()::text,
    product_id   VARCHAR(36)    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_value    VARCHAR(255)   NOT NULL,
    sku_name     VARCHAR(255)   NOT NULL,
    variant_code VARCHAR(100),
    size         VARCHAR(50),
    color        VARCHAR(50),
    unit_cost    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    unit_price   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    weight       DECIMAL(10, 3),
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(36)    NOT NULL,
    updated_by   VARCHAR(36)    NOT NULL,
    CONSTRAINT uq_sku_value     UNIQUE (sku_value),
    CONSTRAINT ck_sku_cost      CHECK (unit_cost >= 0),
    CONSTRAINT ck_sku_price     CHECK (unit_price >= 0)
);

CREATE INDEX idx_skus_sku_value  ON product_skus(sku_value);
CREATE INDEX idx_skus_product_id ON product_skus(product_id);
CREATE INDEX idx_skus_is_active  ON product_skus(is_active) WHERE is_active = TRUE;

-- ── Product Images ───────────────────────────────────────────────────────────

CREATE TABLE product_images (
    id            VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    product_id    VARCHAR(36)  NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url     VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    image_type    VARCHAR(50)  NOT NULL DEFAULT 'PRODUCT',
    display_order INT          NOT NULL DEFAULT 0,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    alt_text      VARCHAR(500),
    file_size     INT,
    uploaded_by   VARCHAR(36)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_images_product_id ON product_images(product_id);
CREATE INDEX idx_images_primary    ON product_images(product_id, is_primary);
