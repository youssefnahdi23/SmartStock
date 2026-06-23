# SmartStock Identity Service

Enterprise-grade authentication and authorization service for the SmartStock AI platform.

## Overview

The Identity Service is responsible for:
- User authentication via JWT tokens
- Role-Based Access Control (RBAC)
- User and role management
- Token refresh and revocation
- Audit logging of security events
- Failed login attempt tracking with account lockout

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.3.1
- **Security**: Spring Security with JWT (RS256 algorithm)
- **Database**: PostgreSQL with JPA/Hibernate
- **Migrations**: Flyway
- **Password Hashing**: BCrypt (cost factor 12)
- **Testing**: JUnit 5, Mockito, TestContainers

### Key Components

#### Security Layer
- **JwtTokenProvider**: Generates, validates, and parses JWT tokens using RS256
- **CustomUserDetailsService**: Spring Security integration with user data
- **SecurityConfig**: Spring Security configuration for JWT-based authentication

#### Service Layer
- **AuthenticationService**: Login, token refresh, logout, token validation
- **UserService**: User CRUD operations and role assignment
- **RoleService**: Role and permission management
- **PasswordService**: Password validation, hashing, and history tracking
- **LoginAttemptService**: Failed login tracking and account lockout
- **AuditService**: Immutable audit logging for security events

#### Data Layer
- **Entities**: User, Role, Permission, RefreshToken, PasswordHistory, FailedLoginAttempt, AuditLog
- **Repositories**: JPA repositories for all entities with optimized queries

#### API Layer
- **AuthController**: POST /api/v1/auth/login, /api/v1/auth/refresh, /api/v1/auth/logout
- **UserController**: CRUD operations for user management
- **RoleController**: Role and permission queries

## Security Features

### Authentication Flow
1. User submits credentials (username/password) to `/api/v1/auth/login`
2. Service validates credentials against bcrypt-hashed password in database
3. Checks failed login attempts and account lockout status
4. Verifies user is active and password not expired
5. Generates JWT access token (1 hour expiration) and refresh token (30 days expiration)
6. Stores refresh token hash in database
7. Returns both tokens to client

### JWT Token Structure

**Access Token (RS256 signed, 1 hour validity)**
```json
{
  "sub": "user-uuid",
  "username": "john.warehouse.manager",
  "email": "john@company.com",
  "roles": ["WAREHOUSE_MANAGER"],
  "permissions": ["inventory:read", "inventory:write:warehouse-W01"],
  "iat": 1718900000,
  "exp": 1718903600,
  "iss": "smartstock-auth",
  "aud": "smartstock-api"
}
```

**Refresh Token (RS256 signed, 30 days validity)**
```json
{
  "sub": "user-uuid",
  "type": "refresh",
  "iat": 1718900000,
  "exp": 1721492000,
  "iss": "smartstock-auth"
}
```

### Role Hierarchy

| Role | Level | Permissions |
|------|-------|-------------|
| SYSTEM_ADMIN | 1 | All system permissions |
| WAREHOUSE_MANAGER | 2 | Warehouse inventory, reports, staff management |
| INVENTORY_OPERATOR | 3 | Stock movements, adjustments, counts |
| SUPPLIER_MANAGER | 4 | Supplier management, purchase orders |
| REPORTER | 5 | Read-only access to all data, report generation |
| AUDITOR | 6 | Access to audit logs only |

### Password Policy
- Minimum 12 characters
- Must contain: uppercase, lowercase, digit, special character
- Expires after 90 days
- Cannot reuse last 5 passwords
- 5 failed attempts lock account for 30 minutes
- Stored as bcrypt hash (cost factor 12)

### Audit Logging
All security events are logged immutably:
- Successful login with user ID, timestamp, IP address
- Failed login attempts with reason
- User creation/modification/deletion
- Role and permission changes
- Token refresh and revocation
- Account lockouts

## Database Schema

### Core Tables
- **users**: User accounts with authentication data
- **roles**: Role definitions with hierarchy levels
- **permissions**: Fine-grained permissions (resource:action:scope)
- **role_permissions**: Many-to-many relationship
- **user_roles**: Many-to-many relationship with optional warehouse scope

### Supporting Tables
- **refresh_tokens**: Issued refresh tokens with revocation status
- **password_history**: Password audit trail (last 5)
- **failed_login_attempts**: Failed login tracking per username
- **audit_logs**: Immutable security event log (JSONB details)

All tables include soft deletes (deleted_at), timestamps (created_at, updated_at), and appropriate indexes for performance.

## Configuration

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=smartstock_identity
DB_USER=smartstock
DB_PASSWORD=smartstock

# JWT
JWT_SECRET_KEY=your-secret-key-minimum-256-bits
JWT_ACCESS_TOKEN_EXPIRATION=3600
JWT_REFRESH_TOKEN_EXPIRATION=2592000

# Server
SERVER_PORT=8001

# Security
CORS_ORIGIN_DESKTOP=http://localhost:3000
```

### application.yml Settings
```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION}
  refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION}
  issuer: smartstock-auth
  audience: smartstock-api

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate
```

## API Endpoints

### Authentication
```bash
# Login
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "john.warehouse.manager",
  "password": "SecurePassword123!"
}

Response:
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "user_id": "uuid",
  "username": "john.warehouse.manager",
  "roles": ["WAREHOUSE_MANAGER"]
}

# Refresh Token
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refresh_token": "eyJ..."
}

# Logout
POST /api/v1/auth/logout
Authorization: Bearer eyJ...

# Validate Token
GET /api/v1/auth/validate
Authorization: Bearer eyJ...
```

### User Management
```bash
# Create User
POST /api/v1/users
Content-Type: application/json

{
  "username": "john.warehouse.manager",
  "email": "john@company.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Manager",
  "roleNames": ["WAREHOUSE_MANAGER"]
}

# Get User
GET /api/v1/users/{userId}
Authorization: Bearer eyJ...

# Update User
PUT /api/v1/users/{userId}
Authorization: Bearer eyJ...

# Assign Role
POST /api/v1/users/{userId}/roles/{roleName}
Authorization: Bearer eyJ...

# Remove Role
DELETE /api/v1/users/{userId}/roles/{roleName}
Authorization: Bearer eyJ...

# List All Users (SYSTEM_ADMIN only)
GET /api/v1/users
Authorization: Bearer eyJ...
```

### Role Management
```bash
# List All Roles
GET /api/v1/roles
Authorization: Bearer eyJ...

# Get Role Details
GET /api/v1/roles/{roleId}
Authorization: Bearer eyJ...
```

## Testing

### Unit Tests
- AuthenticationServiceTest: Login flows, token validation
- PasswordServiceTest: Password validation and policy enforcement
- LoginAttemptServiceTest: Failed attempt tracking and lockout

### Running Tests
```bash
mvn test
```

### Integration Tests
Uses TestContainers with PostgreSQL for realistic database testing.

```bash
mvn test -DargLine="-Dspring.profiles.active=test"
```

## Building and Running

### Build
```bash
mvn clean package
```

### Run Locally
```bash
java -jar target/smartstock-identity-service-1.0.0.jar
```

With Docker:
```bash
docker build -t smartstock-identity-service .
docker run -e DB_HOST=postgres -e DB_USER=smartstock -e DB_PASSWORD=smartstock \
  -p 8001:8001 smartstock-identity-service
```

## Compliance with ADRs

This implementation strictly adheres to:
- **ADR-0005-jwt-rbac-authentication**: JWT with RS256, RBAC, token expiration/refresh
- **ADR-0001-microservices-architecture**: Independent service with own database
- **ADR-0003-database-per-service**: Dedicated PostgreSQL database
- **ADR-0009-observability**: Structured logging, metrics, audit trails

## Future Enhancements

As per ADR-0005 Future Considerations:
1. Multi-Factor Authentication (MFA) with TOTP
2. Token blacklist for immediate revocation
3. Attribute-Based Access Control (ABAC)
4. SSO integration (SAML 2.0 / OpenID Connect)
5. Zero-Trust Security Model implementation
6. Rate limiting enhancements
7. Encryption at rest for sensitive data

## License

MIT License - Copyright (c) 2026 SmartStock AI
