# 1. Identity Service

**Bounded Context:** Identity & Access Management  
**Database:** `identity_db` (PostgreSQL)  
**Port:** 8001  
**Team:** Identity & Security  

---

## Purpose

The Identity Service is the authoritative source for user authentication, authorization, and access control across the SmartStock AI platform. It manages the complete lifecycle of user credentials, roles, permissions, and tokens.

**Not Responsible For:**
- Business logic of other domains
- Product data
- Inventory data
- Order processing

---

## Responsibilities

### 1. User Management
- Create/read/update/deactivate users
- User profile management
- Email verification
- Account activation/deactivation

### 2. Authentication
- Login/logout workflows
- JWT token generation and validation
- Token refresh mechanism
- Multi-factor authentication (future)

### 3. Authorization & Access Control
- Role definition and management (RBAC)
- Permission definition and assignment
- Role-to-permission mapping
- Scope management (tenant, warehouse, user-level)

### 4. Security
- Password hashing (BCrypt)
- Password reset workflows
- Account lockout policies
- API key management (for service-to-service)

### 5. Audit & Compliance
- Login/logout tracking
- Failed login attempts
- Password change history
- Permission change audit trail

---

## Database Ownership

**Schema:** `identity_db`

**Core Tables:**
```sql
-- Users
users (
  id UUID PRIMARY KEY,
  email VARCHAR UNIQUE NOT NULL,
  password_hash VARCHAR NOT NULL,
  full_name VARCHAR,
  status ENUM ('active', 'inactive', 'locked', 'suspended'),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
)

-- Roles
roles (
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)

-- Permissions
permissions (
  id UUID PRIMARY KEY,
  name VARCHAR UNIQUE NOT NULL,
  description TEXT,
  resource VARCHAR,
  action VARCHAR,
  created_at TIMESTAMP
)

-- User-Role assignments
user_roles (
  id UUID PRIMARY KEY,
  user_id UUID FOREIGN KEY,
  role_id UUID FOREIGN KEY,
  assigned_at TIMESTAMP
)

-- Role-Permission assignments
role_permissions (
  id UUID PRIMARY KEY,
  role_id UUID FOREIGN KEY,
  permission_id UUID FOREIGN KEY,
  assigned_at TIMESTAMP
)

-- API Keys
api_keys (
  id UUID PRIMARY KEY,
  service_name VARCHAR NOT NULL,
  key_hash VARCHAR NOT NULL,
  created_at TIMESTAMP,
  expires_at TIMESTAMP
)
```

---

## Events Published

### 1. UserCreated
**When:** New user registered  
**Consumers:** Audit Service, Notification Service  

### 2. UserActivated
**When:** User account activated

### 3. UserDeactivated
**When:** User account deactivated  
**Consumers:** All Services (invalidate sessions)  

### 4. PasswordChanged
**When:** User changes password  

### 5. RoleAssigned
**When:** Role assigned to user  

### 6. RoleRemoved
**When:** Role removed from user  

### 7. PermissionChanged
**When:** Permissions modified  

### 8. LoginAttempted
**When:** User login attempt (success/failure)  
**Consumers:** Audit Service

---

## Events Consumed

None. Identity Service is foundational.

---

## REST APIs

**Base URL:** `/api/v1`

### Authentication
- `POST /auth/register` - Register new user
- `POST /auth/login` - Login and get JWT
- `POST /auth/refresh` - Refresh token
- `POST /auth/logout` - Logout
- `GET /auth/validate` - Validate token

### User Management
- `GET /users/{userId}` - Get user details
- `PUT /users/{userId}` - Update user
- `DELETE /users/{userId}` - Deactivate user
- `GET /users` - List users (paginated)

### Roles & Permissions
- `GET /roles` - List roles
- `POST /roles` - Create role
- `POST /users/{userId}/roles/{roleId}` - Assign role
- `DELETE /users/{userId}/roles/{roleId}` - Remove role

---

## Dependencies

**Synchronous Calls:** None (foundational service)

**External:**
- PostgreSQL
- BCrypt library
- JWT library
- Spring Security

---

## Future Scalability

### Horizontal Scaling
- Stateless service, scale with multiple instances
- Database read replicas for queries

### Caching
- Role/permission mappings in Redis (5-min TTL)
- User profiles in Redis (10-min TTL)

### Multi-Tenancy
- Add tenant_id to all tables
- Scope all queries by tenant

### Compliance
- Every auth attempt → Audit Service event
- Enable SOX/GDPR compliance reporting

---

## Deployment Checklist

- [ ] `identity_db` PostgreSQL created
- [ ] Database migrations applied
- [ ] Default roles created
- [ ] JWT secret configured
- [ ] BCrypt cost configured
- [ ] HTTPS certificates configured
- [ ] Rate limiting at API Gateway
- [ ] Monitoring/alerting configured
- [ ] Audit logging configured
- [ ] Redis caching configured

