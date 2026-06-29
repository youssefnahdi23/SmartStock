-- Customer Service — Full Schema
-- Port 8006 | DB: customer_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─── customers ────────────────────────────────────────────────────────────────
CREATE TABLE customers (
    id                              VARCHAR(36)       PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    customer_code                   VARCHAR(100)      NOT NULL,
    customer_name                   VARCHAR(255)      NOT NULL,
    customer_type                   VARCHAR(50)       NOT NULL DEFAULT 'RETAIL',
    company_name                    VARCHAR(255),
    industry                        VARCHAR(100),
    business_registration_number    VARCHAR(100),
    tax_id                          VARCHAR(100),
    website_url                     VARCHAR(500),
    email_address                   VARCHAR(255),
    phone_number                    VARCHAR(20),
    payment_terms                   VARCHAR(100),
    preferred_currency              VARCHAR(3)        DEFAULT 'USD',
    credit_limit                    DECIMAL(15, 2),
    current_credit_balance          DECIMAL(15, 2)    DEFAULT 0,
    total_orders                    INT               DEFAULT 0,
    total_spent                     DECIMAL(15, 2)    DEFAULT 0,
    average_order_value             DECIMAL(12, 2)    DEFAULT 0,
    lifetime_value                  DECIMAL(15, 2)    DEFAULT 0,
    customer_rating                 DECIMAL(3, 2),
    first_order_date                DATE,
    last_order_date                 DATE,
    segment                         VARCHAR(50)       DEFAULT 'STANDARD',
    risk_rating                     VARCHAR(50)       DEFAULT 'LOW',
    account_manager_id              VARCHAR(36),
    is_active                       BOOLEAN           NOT NULL DEFAULT true,
    is_verified                     BOOLEAN           NOT NULL DEFAULT false,
    suspension_reason               TEXT,
    suspended_at                    TIMESTAMP WITH TIME ZONE,
    resume_date                     DATE,
    notes                           TEXT,
    created_by                      VARCHAR(36)       NOT NULL,
    updated_by                      VARCHAR(36)       NOT NULL,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_customer_code     UNIQUE (customer_code),
    CONSTRAINT customer_name_not_empty CHECK (customer_name != ''),
    CONSTRAINT valid_customer_type CHECK (customer_type IN ('RETAIL', 'WHOLESALE', 'DISTRIBUTOR', 'CORPORATE', 'GOVERNMENT')),
    CONSTRAINT valid_risk_rating CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_customers_code     ON customers(customer_code);
CREATE INDEX idx_customers_name     ON customers(customer_name);
CREATE INDEX idx_customers_type     ON customers(customer_type);
CREATE INDEX idx_customers_active   ON customers(is_active);
CREATE INDEX idx_customers_segment  ON customers(segment);
CREATE INDEX idx_customers_risk     ON customers(risk_rating);

-- ─── customer_contacts ────────────────────────────────────────────────────────
CREATE TABLE customer_contacts (
    id                          VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    customer_id                 VARCHAR(36)   NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    contact_name                VARCHAR(255)  NOT NULL,
    contact_title               VARCHAR(100),
    email_address               VARCHAR(255),
    phone_number                VARCHAR(20),
    mobile_number               VARCHAR(20),
    contact_type                VARCHAR(50)   DEFAULT 'GENERAL',
    is_primary                  BOOLEAN       NOT NULL DEFAULT false,
    is_active                   BOOLEAN       NOT NULL DEFAULT true,
    preferred_contact_method    VARCHAR(50),
    last_contacted_at           TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT contact_name_not_empty CHECK (contact_name != '')
);

CREATE INDEX idx_contacts_customer      ON customer_contacts(customer_id);
CREATE INDEX idx_contacts_primary       ON customer_contacts(customer_id, is_primary) WHERE is_active = true;

-- ─── customer_addresses ───────────────────────────────────────────────────────
CREATE TABLE customer_addresses (
    id              VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    customer_id     VARCHAR(36)   NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    label           VARCHAR(100),
    address_line1   TEXT          NOT NULL,
    address_line2   TEXT,
    city            VARCHAR(100)  NOT NULL,
    state_province  VARCHAR(100),
    postal_code     VARCHAR(20),
    country_code    VARCHAR(2)    NOT NULL DEFAULT 'US',
    address_type    VARCHAR(50)   NOT NULL DEFAULT 'SHIPPING',
    is_default      BOOLEAN       NOT NULL DEFAULT false,
    is_active       BOOLEAN       NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_address_type CHECK (address_type IN ('BILLING', 'SHIPPING', 'SERVICE', 'OTHER'))
);

CREATE INDEX idx_addresses_customer ON customer_addresses(customer_id);
CREATE INDEX idx_addresses_type     ON customer_addresses(customer_id, address_type) WHERE is_active = true;

-- ─── customer_preferences ─────────────────────────────────────────────────────
CREATE TABLE customer_preferences (
    id                              VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    customer_id                     VARCHAR(36)   NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    receive_email_communications    BOOLEAN       DEFAULT true,
    receive_sms_communications      BOOLEAN       DEFAULT false,
    receive_order_notifications     BOOLEAN       DEFAULT true,
    receive_promotional_offers      BOOLEAN       DEFAULT true,
    preferred_contact_method        VARCHAR(50)   DEFAULT 'EMAIL',
    preferred_communication_time    VARCHAR(50),
    newsletter_subscribed           BOOLEAN       DEFAULT true,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_preferences_customer ON customer_preferences(customer_id);
