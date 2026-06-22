-- V1__initial_schema.sql
-- Initial schema for Identity Service
-- Creates tables for users, roles, permissions, and tokens

-- Create ENUM types
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
CREATE TYPE audit_action AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'REFRESH_TOKEN');

-- Permissions table
CREATE TABLE permissions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT permissions_code_check CHECK (code ~* '^[A-Z0-9_]+$')
);

CREATE INDEX idx_perm_code ON permissions(code);
CREATE INDEX idx_perm_active ON permissions(active);

-- Roles table
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT roles_name_check CHECK (name ~* '^[A-Z0-9_]+$')
);

CREATE INDEX idx_role_name ON roles(name);
CREATE INDEX idx_role_active ON roles(active);

-- Role-Permission junction table
CREATE TABLE role_permissions (
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_perms_role ON role_permissions(role_id);
CREATE INDEX idx_role_perms_perm ON role_permissions(permission_id);

-- Users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT users_username_check CHECK (LENGTH(username) >= 3)
);

CREATE INDEX idx_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_username ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_active ON users(active);
CREATE INDEX idx_created_at ON users(created_at);

-- User-Role junction table
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token ON refresh_tokens(token) WHERE revoked = FALSE;
CREATE INDEX idx_user_id ON refresh_tokens(user_id) WHERE revoked = FALSE;
CREATE INDEX idx_expiry ON refresh_tokens(expires_at) WHERE revoked = FALSE;

-- Audit logs table
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    action audit_action NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(36),
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_resource ON audit_logs(resource);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- Insert default permissions
INSERT INTO permissions (id, code, description, resource, action, active, created_at) VALUES
    ('perm-001', 'USER_READ', 'Read user information', 'user', 'read', TRUE, CURRENT_TIMESTAMP),
    ('perm-002', 'USER_CREATE', 'Create new user', 'user', 'create', TRUE, CURRENT_TIMESTAMP),
    ('perm-003', 'USER_UPDATE', 'Update user information', 'user', 'update', TRUE, CURRENT_TIMESTAMP),
    ('perm-004', 'USER_DELETE', 'Delete user', 'user', 'delete', TRUE, CURRENT_TIMESTAMP),
    ('perm-005', 'ROLE_MANAGE', 'Manage roles and permissions', 'role', 'manage', TRUE, CURRENT_TIMESTAMP),
    ('perm-006', 'PRODUCT_READ', 'Read products', 'product', 'read', TRUE, CURRENT_TIMESTAMP),
    ('perm-007', 'PRODUCT_WRITE', 'Create/Edit products', 'product', 'write', TRUE, CURRENT_TIMESTAMP),
    ('perm-008', 'INVENTORY_READ', 'Read inventory', 'inventory', 'read', TRUE, CURRENT_TIMESTAMP),
    ('perm-009', 'INVENTORY_WRITE', 'Update inventory', 'inventory', 'write', TRUE, CURRENT_TIMESTAMP),
    ('perm-010', 'ADMIN_FULL_ACCESS', 'Full system access', 'system', 'admin', TRUE, CURRENT_TIMESTAMP);

-- Insert default roles
INSERT INTO roles (id, name, description, active, created_at, updated_at) VALUES
    ('role-admin', 'ADMIN', 'System administrator with full access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-manager', 'MANAGER', 'Manager with supervisory access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-user', 'USER', 'Standard user with basic access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('role-viewer', 'VIEWER', 'Read-only access', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Assign permissions to roles
INSERT INTO role_permissions (role_id, permission_id, created_at) VALUES
    -- Admin gets all permissions
    ('role-admin', 'perm-001', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-002', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-003', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-004', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-005', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-006', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-007', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-008', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-009', CURRENT_TIMESTAMP),
    ('role-admin', 'perm-010', CURRENT_TIMESTAMP),
    -- Manager gets most permissions
    ('role-manager', 'perm-001', CURRENT_TIMESTAMP),
    ('role-manager', 'perm-006', CURRENT_TIMESTAMP),
    ('role-manager', 'perm-007', CURRENT_TIMESTAMP),
    ('role-manager', 'perm-008', CURRENT_TIMESTAMP),
    ('role-manager', 'perm-009', CURRENT_TIMESTAMP),
    -- User gets basic permissions
    ('role-user', 'perm-001', CURRENT_TIMESTAMP),
    ('role-user', 'perm-006', CURRENT_TIMESTAMP),
    ('role-user', 'perm-008', CURRENT_TIMESTAMP),
    -- Viewer gets read-only
    ('role-viewer', 'perm-001', CURRENT_TIMESTAMP),
    ('role-viewer', 'perm-006', CURRENT_TIMESTAMP),
    ('role-viewer', 'perm-008', CURRENT_TIMESTAMP);
