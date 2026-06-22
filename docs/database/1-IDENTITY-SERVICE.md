# Database Specification: Identity Service

**Service**: Identity Service  
**Purpose**: User authentication, authorization, and role management  
**Database**: PostgreSQL (dedicated)  
**Version**: 1.0  
**Last Updated**: 2026-06-22  

---

## 1. Database Schema Overview

The Identity Service manages user authentication, credentials, roles, permissions, and audit trails for all SmartStock operations.

### High-Level Architecture
```
users
├── user_roles (M:N relationship)
└── user_sessions

roles
├── role_permissions (M:N relationship)
└── roles (hierarchy)

permissions
└── permission_resources (M:N relationship)

audit_logs
```

---

## 2. Tables Specification

### 2.1 `users` Table
**Purpose**: Store user accounts and authentication credentials

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT password_hash_not_empty CHECK (password_hash != ''),
    CONSTRAINT valid_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);
```

**Audit Fields**: created_at, updated_at, deleted_at, created_by, updated_by
**Indexes**: username, email, is_active, deleted_at, created_at
**Analytics**: User growth patterns, security events

---

### 2.2 `roles` Table
**Purpose**: Define authorization roles with hierarchical structure

```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    parent_role_id UUID REFERENCES roles(id),
    is_system_role BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id)
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: name, is_active, parent_role_id
**Analytics**: Role hierarchy analysis, organizational structure

---

### 2.3 `permissions` Table
**Purpose**: Define granular permissions for resource access

```sql
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    resource VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    CONSTRAINT valid_action CHECK (action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'EXECUTE'))
);
```

**Audit Fields**: created_at, updated_at, created_by, updated_by
**Indexes**: resource, action, is_active
**Analytics**: Permission usage patterns, compliance reporting

---

### 2.4 `user_roles` Table
**Purpose**: Map users to roles (many-to-many relationship)

```sql
CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    assigned_by UUID NOT NULL REFERENCES users(id),
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_active_user_role UNIQUE (user_id, role_id) WHERE revoked_at IS NULL
);
```

**Audit Fields**: assigned_at, assigned_by, revoked_at, revoked_by, created_at, updated_at
**Indexes**: user_id, role_id, revoked_at
**Analytics**: Access control audit trail, role change history

---

### 2.5 `role_permissions` Table
**Purpose**: Map roles to permissions (many-to-many relationship)

```sql
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    granted_by UUID NOT NULL REFERENCES users(id),
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT unique_active_role_permission UNIQUE (role_id, permission_id) WHERE revoked_at IS NULL
);
```

**Audit Fields**: granted_at, granted_by, revoked_at, revoked_by, created_at, updated_at
**Indexes**: role_id, permission_id, revoked_at

---

### 2.6 `user_sessions` Table
**Purpose**: Track active user sessions for JWT token validation

```sql
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    ip_address INET,
    user_agent TEXT,
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    device_id VARCHAR(255),
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

**Audit Fields**: issued_at, revoked_at, last_activity_at, created_at, updated_at
**Indexes**: user_id, token_hash, expires_at, revoked_at
**Retention**: Delete expired sessions after 7 days
**Analytics**: Session patterns, login timing analysis

---

### 2.7 `audit_logs` Table
**Purpose**: Immutable audit trail of all authentication and authorization changes

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    actor_id UUID REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    correlation_id UUID,
    request_id UUID,
    CONSTRAINT valid_action_type CHECK (action_type IN ('LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'PASSWORD_CHANGE', 'PASSWORD_RESET', 'ROLE_ASSIGNED', 'ROLE_REVOKED', 'PERMISSION_GRANTED', 'PERMISSION_REVOKED', 'SESSION_REVOKED', 'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED'))
);
```

**Audit Fields**: timestamp (immutable), actor_id, correlation_id, request_id
**Immutability**: No UPDATE/DELETE operations allowed
**Indexes**: entity_type_id, actor_id, timestamp, event_type, correlation_id
**Retention**: Indefinite (compliance requirement)
**Analytics**: Security analytics, forensic investigation, compliance reporting

---

## 3. Relationships & Foreign Keys

```
users (1) ----→ (M) user_roles ----→ (M) roles
                                        ↓ (1)
                                    role_permissions ----→ (M) permissions

users (1) ----→ (M) user_sessions
users (1) ----→ (M) audit_logs
```

**Cascade Rules**: Delete user → Delete user_roles and user_sessions (CASCADE)

---

## 4. Indexing Strategy

### Primary Indexes
- `idx_users_email`: Query users by email
- `idx_users_username`: Query users by username
- `idx_user_roles_user_id`: Find user roles
- `idx_role_permissions_role_id`: Find role permissions
- `idx_audit_logs_timestamp`: Time-range queries

### Composite Indexes
- `idx_user_roles_active(user_id, revoked_at)`: Active roles
- `idx_audit_logs_entity(entity_type, entity_id, timestamp)`: Audit trail

---

## 5. Migration Strategy

### Flyway Versioning
```
V1.0__Initialize_identity_schema.sql
V1.1__Add_user_sessions_table.sql
V1.2__Add_audit_logs_table.sql
V1.3__Add_performance_indexes.sql
```

### Zero-Downtime Migration
- Add new columns as NULLABLE
- Populate in background
- Add constraints after completion
- Drop old columns in separate migration

---

## 6. Future Analytics Considerations

### Data Warehouse Exports
- Daily user growth snapshots
- Role distribution analysis
- Permission usage patterns
- Session activity patterns

### ML Feature Inputs
- User lifecycle stages (active, inactive, risk)
- Role adoption velocity
- Failed login anomaly detection
- Unusual login location/time detection

### Compliance Reporting
- User access certification
- Role usage audit
- Segregation of duties violations
- Audit trail completeness

---

## 7. Security & Compliance

### Data Protection
- Passwords: BCrypt hashing (never plaintext)
- Audit logs: Encrypted at rest
- TLS: All connections mandatory

### Access Control
- Service account: Minimal permissions
- Application user: No direct DB access
- Admin: All operations audited

---

## 8. Monitoring & Observability

### Key Metrics
- Active sessions
- Login success rate
- Failed login attempts
- Role assignment velocity
- Audit log volume

### Alerts
- Failed login spike (> 10 in 5 min)
- Session creation spike
- Audit log write lag

---

## Summary

**Total Tables**: 7  
**Total Indexes**: 15+  
**Audit Coverage**: 100%  
**Soft Deletes**: Yes  
**Compliance**: Full immutable audit trail  

