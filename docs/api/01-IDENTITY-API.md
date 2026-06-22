# Identity Service API

**Base URL:** `/api/v1/identity`  
**Service Port:** 8001  
**Authentication:** JWT Bearer Token (for all endpoints except login/register)  
**Authorization:** RBAC - Role-based permissions  
**Status:** Core Service - Required for all operations

---

## Overview

The Identity Service provides user authentication, authorization, role management, and access control across the SmartStock AI platform.

---

## Authentication & Authorization

### Global Requirements

All endpoints (except `/login` and `/register`) require:

```
Authorization: Bearer <jwt_token>
```

**JWT Token Structure:**
```json
{
  "sub": "user-123",
  "username": "john.warehouse.manager",
  "email": "john@company.com",
  "roles": ["WAREHOUSE_MANAGER"],
  "permissions": [
    "inventory:read",
    "inventory:write:warehouse-W01",
    "stock:adjust",
    "reports:view"
  ],
  "warehouseIds": ["W01", "W02"],
  "iat": 1718900000,
  "exp": 1718903600,
  "iss": "smartstock-auth",
  "aud": "smartstock-api"
}
```

### Role-Based Permissions

- **SYSTEM_ADMIN**: Full system access
- **WAREHOUSE_MANAGER**: Warehouse operations and staff management
- **INVENTORY_OPERATOR**: Stock operations
- **SUPPLIER_MANAGER**: Supplier and purchase order management
- **REPORTER**: Read-only analytics and reporting
- **AUDITOR**: Audit log access only

---

## Endpoints

### 1. User Login

**Endpoint:** `POST /api/v1/identity/auth/login`  
**Authentication:** None (public endpoint)  
**Rate Limit:** 5 requests/minute per IP

**Request Body:**
```json
{
  "username": "john.warehouse.manager",
  "password": "SecurePassword123!"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": "user-123",
      "username": "john.warehouse.manager",
      "email": "john@company.com",
      "firstName": "John",
      "lastName": "Manager",
      "roles": ["WAREHOUSE_MANAGER"],
      "warehouseIds": ["W01", "W02"],
      "active": true
    }
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `400 INVALID_CREDENTIALS` - Username or password incorrect
- `401 ACCOUNT_LOCKED` - Account locked after failed attempts
- `422 VALIDATION_FAILED` - Required fields missing or invalid

---

### 2. User Logout

**Endpoint:** `POST /api/v1/identity/auth/logout`  
**Authentication:** Required (JWT Bearer Token)

**Response (200 OK):**
```json
{
  "data": {
    "message": "Logout successful"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 3. Refresh Access Token

**Endpoint:** `POST /api/v1/identity/auth/refresh`  
**Authentication:** Not required (token in body)

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `401 INVALID_REFRESH_TOKEN` - Refresh token expired or invalid
- `403 REFRESH_TOKEN_REVOKED` - Refresh token has been revoked

---

### 4. Register New User

**Endpoint:** `POST /api/v1/identity/users/register`  
**Authentication:** None (public endpoint)

**Request Body:**
```json
{
  "username": "jane.operator",
  "email": "jane@company.com",
  "password": "SecurePassword123!",
  "firstName": "Jane",
  "lastName": "Operator",
  "roleIds": ["role-inv-operator"]
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "user-124",
    "username": "jane.operator",
    "email": "jane@company.com",
    "firstName": "Jane",
    "lastName": "Operator",
    "roles": [
      {
        "id": "role-inv-operator",
        "name": "INVENTORY_OPERATOR",
        "description": "Inventory operation permissions"
      }
    ],
    "active": true,
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `400 USER_ALREADY_EXISTS` - Username or email already in use
- `422 VALIDATION_FAILED` - Invalid input (weak password, invalid email, etc.)

---

### 5. Get User Profile

**Endpoint:** `GET /api/v1/identity/users/{userId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** User can view own profile; admins can view any user

**Response (200 OK):**
```json
{
  "data": {
    "id": "user-123",
    "username": "john.warehouse.manager",
    "email": "john@company.com",
    "firstName": "John",
    "lastName": "Manager",
    "roles": ["WAREHOUSE_MANAGER"],
    "permissions": ["inventory:read", "inventory:write:warehouse-W01", "warehouse:manage"],
    "warehouseIds": ["W01", "W02"],
    "active": true,
    "lastLoginAt": "2026-06-20T10:00:00Z",
    "createdAt": "2026-01-15T08:30:00Z",
    "updatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `401 UNAUTHORIZED` - Invalid or expired token
- `403 INSUFFICIENT_PERMISSIONS` - Cannot access other user profiles
- `404 USER_NOT_FOUND` - User does not exist

---

### 6. Update User Profile

**Endpoint:** `PUT /api/v1/identity/users/{userId}`  
**Authentication:** Required (JWT Bearer Token)

**Request Body:**
```json
{
  "firstName": "Jonathan",
  "lastName": "Manager",
  "email": "jonathan@company.com"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "id": "user-123",
    "username": "john.warehouse.manager",
    "email": "jonathan@company.com",
    "firstName": "Jonathan",
    "lastName": "Manager",
    "active": true,
    "updatedAt": "2026-06-20T12:05:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:05:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 7. Change Password

**Endpoint:** `POST /api/v1/identity/users/{userId}/change-password`  
**Authentication:** Required (JWT Bearer Token)

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewSecurePassword456!"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "message": "Password changed successfully"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `400 INVALID_CURRENT_PASSWORD` - Current password is incorrect
- `400 PASSWORD_REUSED` - Password cannot be one of last 5 passwords
- `422 VALIDATION_FAILED` - New password does not meet requirements

---

### 8. Request Password Reset

**Endpoint:** `POST /api/v1/identity/auth/forgot-password`  
**Authentication:** None (public endpoint)  
**Rate Limit:** 3 requests/hour per email

**Request Body:**
```json
{
  "email": "john@company.com"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "message": "Password reset email sent"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `404 USER_NOT_FOUND` - User with email not found

---

### 9. Reset Password with Token

**Endpoint:** `POST /api/v1/identity/auth/reset-password`  
**Authentication:** None (token provided in request)

**Request Body:**
```json
{
  "token": "reset-token-abc123",
  "newPassword": "NewSecurePassword456!"
}
```

**Response (200 OK):**
```json
{
  "data": {
    "message": "Password reset successful"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `400 INVALID_RESET_TOKEN` - Token is invalid or expired
- `422 VALIDATION_FAILED` - New password does not meet requirements

---

### 10. List Users (Paginated)

**Endpoint:** `GET /api/v1/identity/users`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `user:admin:read` (admin only)

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20, max: 100) - Results per page
- `sort` (string) - Field and direction (e.g., "createdAt,desc")
- `role` (string) - Filter by role name
- `active` (boolean) - Filter by active status
- `search` (string) - Search username or email

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "user-123",
      "username": "john.warehouse.manager",
      "email": "john@company.com",
      "firstName": "John",
      "lastName": "Manager",
      "roles": ["WAREHOUSE_MANAGER"],
      "active": true,
      "lastLoginAt": "2026-06-20T10:00:00Z",
      "createdAt": "2026-01-15T08:30:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 45,
    "totalPages": 3,
    "traceId": "trace-123"
  }
}
```

**Errors:**
- `403 INSUFFICIENT_PERMISSIONS` - User cannot list all users

---

### 11. Deactivate User

**Endpoint:** `POST /api/v1/identity/users/{userId}/deactivate`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `user:admin:write` (admin only)

**Response (200 OK):**
```json
{
  "data": {
    "id": "user-123",
    "username": "john.warehouse.manager",
    "active": false,
    "deactivatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 12. Reactivate User

**Endpoint:** `POST /api/v1/identity/users/{userId}/reactivate`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `user:admin:write` (admin only)

**Response (200 OK):**
```json
{
  "data": {
    "id": "user-123",
    "username": "john.warehouse.manager",
    "active": true,
    "reactivatedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 13. Assign Role to User

**Endpoint:** `POST /api/v1/identity/users/{userId}/roles`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `role:assign` (admin only)

**Request Body:**
```json
{
  "roleId": "role-warehouse-mgr",
  "warehouseIds": ["W01", "W02"]
}
```

**Response (200 OK):**
```json
{
  "data": {
    "userId": "user-123",
    "roleId": "role-warehouse-mgr",
    "roleName": "WAREHOUSE_MANAGER",
    "warehouseIds": ["W01", "W02"],
    "assignedAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 14. Remove Role from User

**Endpoint:** `DELETE /api/v1/identity/users/{userId}/roles/{roleId}`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `role:revoke` (admin only)

**Response (200 OK):**
```json
{
  "data": {
    "message": "Role removed successfully",
    "userId": "user-123",
    "roleId": "role-warehouse-mgr"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

### 15. Get All Roles

**Endpoint:** `GET /api/v1/identity/roles`  
**Authentication:** Required (JWT Bearer Token)

**Query Parameters:**
- `page` (integer, default: 0) - Page number
- `size` (integer, default: 20) - Results per page

**Response (200 OK):**
```json
{
  "data": [
    {
      "id": "role-admin",
      "name": "SYSTEM_ADMIN",
      "description": "Full system access",
      "permissions": ["*"],
      "createdAt": "2026-01-01T00:00:00Z"
    },
    {
      "id": "role-warehouse-mgr",
      "name": "WAREHOUSE_MANAGER",
      "description": "Warehouse management permissions",
      "permissions": ["inventory:read", "inventory:write:warehouse-*", "warehouse:manage"],
      "createdAt": "2026-01-01T00:00:00Z"
    }
  ],
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "page": 0,
    "size": 20,
    "total": 6,
    "traceId": "trace-123"
  }
}
```

---

### 16. Create Role (Admin)

**Endpoint:** `POST /api/v1/identity/roles`  
**Authentication:** Required (JWT Bearer Token)  
**Authorization:** `role:admin:create` (admin only)

**Request Body:**
```json
{
  "name": "WAREHOUSE_AUDITOR",
  "description": "Warehouse inventory auditor role",
  "permissions": ["inventory:read", "inventory:audit", "report:generate"]
}
```

**Response (201 Created):**
```json
{
  "data": {
    "id": "role-warehouse-auditor",
    "name": "WAREHOUSE_AUDITOR",
    "description": "Warehouse inventory auditor role",
    "permissions": ["inventory:read", "inventory:audit", "report:generate"],
    "createdAt": "2026-06-20T12:00:00Z"
  },
  "meta": {
    "timestamp": "2026-06-20T12:00:00Z",
    "traceId": "trace-123"
  }
}
```

---

## Data Transfer Objects

### UserDTO
```typescript
{
  id: UUID;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  roles: string[];
  permissions: string[];
  warehouseIds: UUID[];
  lastLoginAt: ISO8601DateTime;
  createdAt: ISO8601DateTime;
  updatedAt: ISO8601DateTime;
}
```

### RoleDTO
```typescript
{
  id: UUID;
  name: string;
  description: string;
  permissions: string[];
  createdAt: ISO8601DateTime;
}
```

### AuthTokenResponse
```typescript
{
  accessToken: JWT;
  refreshToken: JWT;
  expiresIn: number;
  tokenType: "Bearer";
  user: UserDTO;
}
```

---

## Standard Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| UNAUTHORIZED | 401 | Missing or invalid authentication token |
| INVALID_CREDENTIALS | 401 | Username/password incorrect |
| INSUFFICIENT_PERMISSIONS | 403 | User lacks required permissions |
| ACCOUNT_LOCKED | 401 | Account locked due to failed attempts |
| VALIDATION_FAILED | 422 | Input validation error |
| RATE_LIMITED | 429 | Too many requests |
| USER_NOT_FOUND | 404 | User does not exist |
| USER_ALREADY_EXISTS | 400 | Username or email already in use |
| PASSWORD_REUSED | 400 | Password cannot be one of last 5 |
| INVALID_RESET_TOKEN | 400 | Password reset token invalid/expired |
| INVALID_REFRESH_TOKEN | 401 | Refresh token invalid/expired |

---

## Standard Response Format

**Success:**
```json
{
  "data": {},
  "meta": {
    "timestamp": "ISO8601",
    "traceId": "string"
  }
}
```

**Error:**
```json
{
  "errors": [
    {
      "code": "ERROR_CODE",
      "message": "Human-readable message",
      "field": "fieldName"
    }
  ],
  "meta": {
    "timestamp": "ISO8601",
    "traceId": "string",
    "status": 400
  }
}
```

---

## Events Published

- `UserCreated` - New user account created
- `UserActivated` - User account activated
- `UserDeactivated` - User account deactivated
- `PasswordChanged` - User password changed
- `RoleAssigned` - User assigned a role
- `RoleRevoked` - User role revoked
- `LoginSuccessful` - User successfully logged in
- `LoginFailed` - User login failed
- `AccountLocked` - Account locked due to failed attempts

---

## Implementation Notes

1. Passwords minimum 12 characters (uppercase, lowercase, number, special character)
2. JWT signed with RS256 (RSA 4096-bit keys)
3. Access tokens expire in 1 hour
4. Refresh tokens expire in 30 days
5. Failed login lockout: 5 attempts, 30-minute lockout
6. Password expiration: 90 days
7. All endpoints require HTTPS/TLS 1.3+
8. Rate limiting enforced at API Gateway level
9. All authentication/authorization changes logged for audit
10. Sensitive data never logged (passwords, tokens, sensitive PII)
