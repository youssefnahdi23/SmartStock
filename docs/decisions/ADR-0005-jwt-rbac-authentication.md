# ADR-0005: Authentication and Authorization Strategy - JWT with Role-Based Access Control

## Status
Accepted

## Context
SmartStock AI is an enterprise system handling sensitive inventory and financial data. The authentication and authorization system must:
- **Secure**: Prevent unauthorized access and data breaches
- **Scalable**: Support thousands of concurrent users across multiple warehouses
- **Stateless**: Allow horizontal scaling of services without session affinity
- **Fine-Grained**: Support role-based and possibly attribute-based access control
- **Standards-Compliant**: Follow industry best practices (OAuth2, OpenID Connect where appropriate)
- **Operational**: Low maintenance burden, standard tools

The system must support:
- Multiple user roles (Admin, Warehouse Manager, Inventory Operator, Supplier Manager, Reporter, Auditor)
- Multi-warehouse operations where users have different permissions per warehouse
- API authentication for desktop client and future mobile apps
- Audit trail of access to sensitive data
- Token expiration and refresh for security

## Decision
Implement **JWT-Based Authentication with Role-Based Access Control (RBAC)** layered with an API Gateway:

### 1. **Authentication Flow**
```
1. User Login
   POST /auth/login
   Body: { username, password }
   
2. Identity Service validates credentials (bcrypt hashing)
   
3. Identity Service returns JWT token
   {
     "accessToken": "eyJ...",
     "refreshToken": "eyJ...",
     "expiresIn": 3600,
     "tokenType": "Bearer"
   }
   
4. Client stores tokens securely (httpOnly cookies or secure storage)
   
5. Client includes token in Authorization header: "Bearer eyJ..."
   
6. API Gateway validates JWT signature and expiration
   
7. Request routed to service with decoded token context
```

### 2. **JWT Token Structure**
Access Token (short-lived, 1 hour):
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

Refresh Token (long-lived, 30 days):
```json
{
  "sub": "user-123",
  "type": "refresh",
  "iat": 1718900000,
  "exp": 1721492000,
  "iss": "smartstock-auth"
}
```

### 3. **Role Hierarchy**
```
SYSTEM_ADMIN
├── Full access to all services
├── User and role management
├── System configuration
└── Audit log access

WAREHOUSE_MANAGER
├── Read/write inventory for assigned warehouses
├── View reports for assigned warehouses
├── Manage warehouse zones and locations
└── Supervise warehouse staff

INVENTORY_OPERATOR
├── Stock in/out operations
├── Stock adjustments
├── Inventory counts
└── View inventory (read-only for other warehouses)

SUPPLIER_MANAGER
├── Supplier management
├── Purchase order creation
├── Delivery tracking
└── Supplier performance reports

REPORTER
├── Read-only access to all data
├── Dashboard and report generation
└── Data export (non-sensitive)

AUDITOR
├── Read-only access to audit logs
├── No operational access
└── Compliance reporting
```

### 4. **Permission Model**
Fine-grained permissions (resource:action:scope):
```
inventory:read                    -- Read inventory data globally
inventory:write                   -- Create/update inventory globally
inventory:write:warehouse-W01     -- Limited to warehouse W01
inventory:adjust                  -- Adjust stock
stock:reserve                     -- Reserve stock for orders
supplier:read                     -- Read supplier data
order:create                      -- Create purchase/sales orders
report:generate                   -- Generate reports
audit:read                        -- Access audit logs
user:manage                       -- Create/modify users
system:config                     -- System configuration
```

### 5. **API Gateway Security**
All requests flow through API Gateway which:
- Validates JWT signature using Identity Service's public key
- Checks token expiration
- Enforces HTTPS/TLS
- Implements rate limiting (prevent brute force attacks)
- Logs all API access for audit trail
- Routes authenticated request to appropriate service
- Adds correlation ID and user context to request headers

### 6. **Password Security**
- Minimum 12 characters, complexity requirements
- Hashed with bcrypt (cost factor: 12)
- Password history (prevent reuse of last 5 passwords)
- Password expiration: 90 days
- Failed login attempts: 5 attempts locks account for 30 minutes
- MFA (multi-factor authentication) optional for admin accounts

## Alternatives Considered

### Option 1: OAuth2 with OpenID Connect
Industry-standard delegation protocol

**Pros:**
- Supports third-party identity providers
- Well-established security standards
- Good for federated identity scenarios

**Cons:**
- More complex implementation
- Requires external identity provider
- Overkill for single-organization system
- Additional operational overhead
- Latency for token validation

### Option 2: Session-Based Authentication (Traditional Sessions)
Server maintains session state for each user

**Pros:**
- Simpler development model
- Easy to revoke access (delete session)
- Familiar pattern

**Cons:**
- Requires sticky sessions (violates microservices statelessness)
- Doesn't scale horizontally without shared session store (Redis)
- More memory overhead on servers
- CSRF vulnerabilities if not implemented carefully
- Difficult with desktop and mobile clients

### Option 3: API Keys for Service-to-Service Only
Different mechanisms for users vs. services

**Pros:**
- Simpler for internal service communication
- Easy to revoke specific keys

**Cons:**
- Requires managing two authentication systems
- Key storage and rotation burden
- Less suitable for user-facing applications
- Higher operational complexity

## Consequences

### Positive
- **Stateless Services**: API Gateway validates tokens; services don't maintain session state
- **Horizontal Scalability**: Services can scale without session affinity requirements
- **Mobile/Desktop Ready**: JWT works across all client platforms (web, mobile, desktop)
- **Fine-Grained Control**: Permission model supports complex authorization requirements
- **Audit Trail**: Access logs associate actions with users for compliance
- **Token Expiration**: Short-lived tokens limit damage from stolen tokens
- **Refresh Token Rotation**: Long-lived refresh tokens can be revoked without forcing re-login
- **Standard Protocol**: JWT is widely understood; reduces vendor lock-in
- **Performance**: Stateless validation faster than session store lookup
- **Multi-Warehouse Support**: Permissions can be scoped to specific warehouses

### Negative
- **Token Size**: JWT tokens larger than session cookies (increases bandwidth)
- **Revocation Delay**: Revoking tokens requires waiting for expiration (blacklist adds complexity)
- **Key Management**: Must securely distribute public keys for token validation
- **Clock Skew**: Token validation sensitive to server clock synchronization
- **Complex Permissions**: Fine-grained permission model requires careful design
- **Scope Creep**: Easy to over-engineer authorization; temptation to add complex conditions
- **Debugging**: Harder to debug authorization issues without request context
- **Token Theft**: If token stolen, attacker has access until expiration

### Trade-offs
- **Simplicity vs. Security**: Accept token size overhead for stateless operation
- **Revocation Capability vs. Complexity**: Token blacklist adds complexity to implement immediate revocation
- **User Experience vs. Security**: Short token lifetime improves security but increases re-authentication burden
- **Flexibility vs. Understandability**: Complex permission model powerful but harder to reason about

## Future Considerations

1. **Multi-Factor Authentication (MFA)**: Implement for admin and high-privilege accounts
   - Time-based one-time passwords (TOTP) via authenticator apps
   - Security keys (FIDO2/WebAuthn) for physical security devices

2. **Token Blacklist**: Implement Redis-backed token blacklist for immediate revocation
   - Logout immediately revokes token
   - High-risk actions require token revalidation
   - Account deletion immediately revokes all tokens

3. **Attribute-Based Access Control (ABAC)**: Extend beyond role-based to attribute-based
   - Support dynamic permissions based on user attributes (department, location)
   - More complex but more flexible than RBAC alone

4. **Single Sign-On (SSO)**: Integrate with corporate identity provider (Active Directory, Okta)
   - SAML 2.0 or OpenID Connect protocol
   - Unified authentication across enterprise systems

5. **Zero-Trust Security Model**: Assume no trust by default
   - Verify every request regardless of network location
   - Principle of least privilege for all permissions
   - Continuous verification rather than one-time authentication

6. **Audit Logging Enhancement**: Log sensitive operations
   - Failed authentication attempts (track potential attacks)
   - Permission changes
   - Access to sensitive reports or exports
   - Data deletions or modifications

7. **Rate Limiting**: Implement sophisticated rate limiting
   - Per-user limits to prevent abuse
   - Per-IP limits to prevent brute force
   - Per-endpoint limits based on operation cost

8. **Encryption**: Encrypt sensitive data in transit and at rest
   - TLS 1.3 for all network communication
   - Database encryption for personally identifiable information (PII)
   - Symmetric encryption for data at rest

## Implementation Guidance

- Identity Service uses bcrypt with cost factor 12 for password hashing
- Token signing uses RS256 (RSA) with strong keys (4096-bit)
- Public keys refreshed periodically; old keys maintained briefly for rotation
- API Gateway caches public keys with short TTL (5 minutes)
- Desktop client (JavaFX) uses secure credential storage (OS keychain)
- Refresh tokens invalidated on logout or suspicious activity
- Failed login attempts logged and monitored for attack detection
- Passwords never logged or transmitted in clear text
- All authentication endpoints require HTTPS/TLS
- Permission checks performed in Gateway; services can assume authenticated user
- Services document required permissions in API documentation
- Audit Service logs all access to sensitive data for compliance review
