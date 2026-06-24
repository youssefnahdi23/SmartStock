-- V1__initial_schema.sql
-- Identity Service: complete schema for authentication, RBAC, tokens, and audit
-- Follows database spec docs/database/1-IDENTITY-SERVICE.md

-- ============================================================
-- PERMISSIONS
-- ============================================================
CREATE TABLE permissions (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    resource    VARCHAR(255) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_permission_action CHECK (action IN ('CREATE','READ','UPDATE','DELETE','EXECUTE','MANAGE','READ_WRITE'))
);

CREATE INDEX idx_permissions_resource  ON permissions(resource);
CREATE INDEX idx_permissions_action    ON permissions(action);
CREATE INDEX idx_permissions_is_active ON permissions(is_active);

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id             VARCHAR(36)  PRIMARY KEY,
    name           VARCHAR(255) NOT NULL UNIQUE,
    description    TEXT,
    parent_role_id VARCHAR(36)  REFERENCES roles(id),
    is_system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_name           ON roles(name);
CREATE INDEX idx_roles_is_active      ON roles(is_active);
CREATE INDEX idx_roles_parent_role_id ON roles(parent_role_id);

-- ============================================================
-- ROLE → PERMISSION MAPPING
-- ============================================================
CREATE TABLE role_permissions (
    role_id       VARCHAR(36) NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id       ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id                     VARCHAR(36)  PRIMARY KEY,
    username               VARCHAR(100) NOT NULL UNIQUE,
    email                  VARCHAR(255) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    first_name             VARCHAR(255) NOT NULL,
    last_name              VARCHAR(255) NOT NULL,
    phone_number           VARCHAR(20),
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified         BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login_at          TIMESTAMPTZ,
    password_changed_at    TIMESTAMPTZ,
    failed_login_attempts  INT          NOT NULL DEFAULT 0,
    locked_until           TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at             TIMESTAMPTZ,
    CONSTRAINT chk_password_hash_not_empty CHECK (password_hash <> ''),
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_username_length CHECK (LENGTH(username) >= 3)
);

CREATE INDEX idx_users_email      ON users(email)    WHERE deleted_at IS NULL;
CREATE INDEX idx_users_username   ON users(username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_is_active  ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- ============================================================
-- USER → ROLE MAPPING
-- ============================================================
CREATE TABLE user_roles (
    user_id     VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id         VARCHAR(36)  PRIMARY KEY,
    token      VARCHAR(500) NOT NULL UNIQUE,
    user_id    VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token      ON refresh_tokens(token)      WHERE revoked = FALSE;
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id)    WHERE revoked = FALSE;
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at) WHERE revoked = FALSE;

-- ============================================================
-- PASSWORD RESET TOKENS
-- ============================================================
CREATE TABLE password_reset_tokens (
    id         VARCHAR(36)  PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    user_id    VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prt_token      ON password_reset_tokens(token)   WHERE used = FALSE;
CREATE INDEX idx_prt_user_id    ON password_reset_tokens(user_id) WHERE used = FALSE;
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);

-- ============================================================
-- USER SESSIONS (for logout / token tracking)
-- ============================================================
CREATE TABLE user_sessions (
    id               VARCHAR(36)  PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash       VARCHAR(255) NOT NULL UNIQUE,
    ip_address       VARCHAR(45),
    user_agent       TEXT,
    issued_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMPTZ  NOT NULL,
    revoked_at       TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_sessions_user_id    ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);
CREATE INDEX idx_user_sessions_revoked_at ON user_sessions(revoked_at);

-- ============================================================
-- AUDIT LOGS (immutable)
-- ============================================================
CREATE TABLE audit_logs (
    id             VARCHAR(36)  PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      VARCHAR(36),
    actor_id       VARCHAR(36)  REFERENCES users(id) ON DELETE SET NULL,
    action_type    VARCHAR(50)  NOT NULL,
    old_values     JSONB,
    new_values     JSONB,
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    status         VARCHAR(50)  NOT NULL DEFAULT 'SUCCESS',
    error_message  TEXT,
    timestamp      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(36),
    request_id     VARCHAR(36),
    CONSTRAINT chk_audit_action_type CHECK (
        action_type IN (
            'LOGIN','LOGOUT','LOGIN_FAILED','PASSWORD_CHANGE','PASSWORD_RESET',
            'ROLE_ASSIGNED','ROLE_REVOKED','PERMISSION_GRANTED','PERMISSION_REVOKED',
            'SESSION_REVOKED','ACCOUNT_LOCKED','ACCOUNT_UNLOCKED','USER_CREATED',
            'USER_UPDATED','USER_DEACTIVATED','USER_REACTIVATED'
        )
    )
);

CREATE INDEX idx_audit_logs_entity      ON audit_logs(entity_type, entity_id, timestamp);
CREATE INDEX idx_audit_logs_actor_id    ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_timestamp   ON audit_logs(timestamp);
CREATE INDEX idx_audit_logs_event_type  ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_corr_id     ON audit_logs(correlation_id);

-- Prevent updates/deletes on audit_logs (immutability enforcement)
CREATE OR REPLACE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE OR REPLACE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
