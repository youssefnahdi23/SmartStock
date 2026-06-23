CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    hierarchy_rank INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name),
    CONSTRAINT ck_roles_name CHECK (name ~ '^[A-Z_]+$'),
    CONSTRAINT ck_roles_rank CHECK (hierarchy_rank >= 0)
);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    permission_key VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL,
    resource VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    scope VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_permissions_key UNIQUE (permission_key),
    CONSTRAINT ck_permissions_key CHECK (permission_key ~ '^[a-z]+:[a-z]+:[a-z0-9_-]+$')
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    locked_until TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    password_expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '90 days'),
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_username CHECK (char_length(username) BETWEEN 3 AND 100),
    CONSTRAINT ck_users_email CHECK (email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$'),
    CONSTRAINT ck_users_password_hash CHECK (char_length(password_hash) >= 60)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_warehouse_access (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    warehouse_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, warehouse_id)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_id VARCHAR(64) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    issued_ip VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT uq_refresh_tokens_id UNIQUE (token_id)
);

CREATE TABLE password_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE failed_login_attempts (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    first_attempt_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    lock_expires_at TIMESTAMPTZ,
    last_ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_failed_login_attempts_username UNIQUE (username),
    CONSTRAINT ck_failed_login_attempts_count CHECK (attempt_count >= 0)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    outcome VARCHAR(32) NOT NULL,
    details TEXT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    request_id VARCHAR(100),
    correlation_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_active ON roles (active);
CREATE INDEX idx_permissions_active ON permissions (active);
CREATE INDEX idx_users_active ON users (active);
CREATE INDEX idx_users_locked_until ON users (locked_until);
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
CREATE INDEX idx_user_warehouse_access_warehouse_id ON user_warehouse_access (warehouse_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);
CREATE INDEX idx_refresh_tokens_revoked_at ON refresh_tokens (revoked_at);
CREATE INDEX idx_password_history_user_id_created_at ON password_history (user_id, created_at DESC);
CREATE INDEX idx_failed_login_attempts_lock_expires_at ON failed_login_attempts (lock_expires_at);
CREATE INDEX idx_audit_logs_user_id_created_at ON audit_logs (user_id, created_at DESC);
CREATE INDEX idx_audit_logs_event_type_created_at ON audit_logs (event_type, created_at DESC);
CREATE INDEX idx_audit_logs_correlation_id ON audit_logs (correlation_id);

INSERT INTO permissions (id, permission_key, description, resource, action, scope) VALUES
('10000000-0000-0000-0000-000000000001', 'system:manage:global', 'Manage global system configuration and privileged identity actions', 'system', 'manage', 'global'),
('10000000-0000-0000-0000-000000000002', 'user:manage:global', 'Create, update, deactivate, and assign roles to users', 'user', 'manage', 'global'),
('10000000-0000-0000-0000-000000000003', 'role:manage:global', 'Create and update roles', 'role', 'manage', 'global'),
('10000000-0000-0000-0000-000000000004', 'permission:manage:global', 'Create and update permissions', 'permission', 'manage', 'global'),
('10000000-0000-0000-0000-000000000005', 'audit:read:global', 'Read identity audit records', 'audit', 'read', 'global'),
('10000000-0000-0000-0000-000000000006', 'inventory:read:global', 'Read inventory data across warehouses', 'inventory', 'read', 'global'),
('10000000-0000-0000-0000-000000000007', 'inventory:write:warehouse', 'Create and update inventory within assigned warehouses', 'inventory', 'write', 'warehouse'),
('10000000-0000-0000-0000-000000000008', 'inventory:adjust:warehouse', 'Adjust inventory within assigned warehouses', 'inventory', 'adjust', 'warehouse'),
('10000000-0000-0000-0000-000000000009', 'warehouse:manage:warehouse', 'Manage warehouse structure and assignments', 'warehouse', 'manage', 'warehouse'),
('10000000-0000-0000-0000-00000000000a', 'supplier:read:global', 'Read supplier data', 'supplier', 'read', 'global'),
('10000000-0000-0000-0000-00000000000b', 'supplier:manage:global', 'Manage supplier relationships', 'supplier', 'manage', 'global'),
('10000000-0000-0000-0000-00000000000c', 'order:create:global', 'Create purchase and sales orders', 'order', 'create', 'global'),
('10000000-0000-0000-0000-00000000000d', 'order:read:global', 'Read order data', 'order', 'read', 'global'),
('10000000-0000-0000-0000-00000000000e', 'report:generate:global', 'Generate operational and compliance reports', 'report', 'generate', 'global'),
('10000000-0000-0000-0000-00000000000f', 'report:read:global', 'Read generated reports and dashboards', 'report', 'read', 'global');

INSERT INTO roles (id, name, description, hierarchy_rank) VALUES
('20000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN', 'Full identity, security, audit, and platform administration access', 0),
('20000000-0000-0000-0000-000000000002', 'WAREHOUSE_MANAGER', 'Warehouse operational control with inventory and reporting permissions', 10),
('20000000-0000-0000-0000-000000000003', 'INVENTORY_OPERATOR', 'Operational inventory execution within assigned warehouses', 20),
('20000000-0000-0000-0000-000000000004', 'SUPPLIER_MANAGER', 'Supplier lifecycle and procurement collaboration access', 30),
('20000000-0000-0000-0000-000000000005', 'REPORTER', 'Read-only business reporting and data visibility', 40),
('20000000-0000-0000-0000-000000000006', 'AUDITOR', 'Read-only compliance and audit access', 50);

INSERT INTO role_permissions (role_id, permission_id) VALUES
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000009'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000a'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000b'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000c'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000d'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000e'),
('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-00000000000f'),

('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000009'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-00000000000c'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-00000000000d'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-00000000000e'),
('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-00000000000f'),

('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000007'),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000008'),
('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-00000000000d'),

('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000a'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000b'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000c'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000d'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000e'),
('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-00000000000f'),

('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000006'),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-00000000000a'),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-00000000000d'),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-00000000000e'),
('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-00000000000f'),

('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000005'),
('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-00000000000f');
