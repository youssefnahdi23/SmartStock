# ADR-0008: API Gateway and Cross-Cutting Concerns Strategy

## Status
Accepted

## Context
In a microservices architecture, services need consistent handling of cross-cutting concerns:
- **Authentication/Authorization**: All requests must be authenticated and authorized
- **Rate Limiting**: Prevent abuse and ensure fair resource allocation
- **Request Logging/Tracing**: Track requests for debugging and monitoring
- **Response Transformation**: Standardize error responses across services
- **CORS/SSL**: Enforce HTTPS and handle cross-origin requests
- **Request Routing**: Route requests to appropriate service

If each service implements these concerns independently:
- Code duplication across services
- Inconsistent behavior (different rate limits, different error formats)
- Difficult to change policy globally
- Security gaps if any service misses security concern

The API Gateway pattern centralizes these concerns, but introduces:
- Single point of failure
- Additional network hop (latency)
- Operational complexity

## Decision
Implement a **Centralized API Gateway** that handles all cross-cutting concerns:

### 1. **API Gateway Responsibilities**

**Authentication & Authorization**
- Validate JWT tokens for all requests
- Extract user context and add to request headers
- Enforce role-based permissions
- Return 401/403 for unauthorized requests

**Rate Limiting**
- Per-user rate limits (100 requests/minute default)
- Per-IP rate limits to prevent brute force attacks
- Per-endpoint limits based on operation cost
- Implement token bucket algorithm with Redis backend

**Request/Response Transformation**
- Log all requests with correlation IDs
- Add standard headers (Request-ID, User-ID, Trace-ID)
- Transform errors to standardized format:
  ```json
  {
    "timestamp": "2026-06-20T12:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Product ID must be a valid UUID",
    "path": "/products/invalid-id",
    "traceId": "abc123def456"
  }
  ```

**Request Routing**
- Route `/products/*` to Product Service
- Route `/inventory/*` to Inventory Service
- Route `/orders/*` to Order Service
- Support service discovery (services register dynamically)

**Load Balancing**
- Round-robin across service instances
- Health checks to detect unavailable instances
- Circuit breaker for failing services

**Security**
- Enforce HTTPS/TLS
- Validate request format
- Reject oversized requests (DDoS protection)

### 2. **Gateway Implementation**

**Framework**: Spring Cloud Gateway
- Built on Spring Boot (consistent with services)
- Reactive (non-blocking, high throughput)
- Filter-based architecture for cross-cutting concerns
- Support for custom filters

**Configuration Example**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/products/**
          filters:
            - RewritePath=/products(?<segment>/?.*), /api/v1$\{segment}
            - name: RateLimit
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
            - name: CircuitBreaker
              args:
                name: productCircuitBreaker
        
        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/inventory/**
          filters:
            - RewritePath=/inventory(?<segment>/?.*), /api/v1$\{segment}
            - name: RateLimit
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
```

### 3. **Standard Response Headers**
All responses include:
```
Request-ID: abc123def456              -- Unique request identifier
Trace-ID: trace789xyz123              -- Distributed trace ID
User-ID: user-456                     -- Authenticated user
X-API-Version: v1                     -- API version
Cache-Control: max-age=300            -- Caching directives
```

### 4. **Error Response Standardization**
All services return errors in unified format through gateway:

**400 Bad Request**
```json
{
  "timestamp": "2026-06-20T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Inventory quantity must be a positive integer",
  "path": "/inventory/adjust",
  "traceId": "abc123"
}
```

**401 Unauthorized**
```json
{
  "timestamp": "2026-06-20T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/products",
  "traceId": "abc123"
}
```

**404 Not Found**
```json
{
  "timestamp": "2026-06-20T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found: prod-123",
  "path": "/products/prod-123",
  "traceId": "abc123"
}
```

### 5. **High Availability**
- API Gateway deployed across multiple instances
- Load balancer (nginx, HAProxy) distributes traffic
- Health checks ensure only healthy instances receive traffic
- Graceful shutdown: drain existing connections before terminating

## Alternatives Considered

### Option 1: No API Gateway (Service-to-Service API Contract)
Each service handles cross-cutting concerns independently

**Pros:**
- Simpler deployment (no additional service)
- Lower latency (direct service calls)
- Service autonomy

**Cons:**
- Code duplication (each service implements auth, logging, rate limiting)
- Inconsistent behavior across services
- Difficult to enforce global policies
- Higher risk of security gaps
- Client must handle service discovery
- Complex testing without gateway

### Option 2: Lightweight API Gateway (Kong, Tyk)
Specialized API gateway products

**Pros:**
- Feature-rich
- Large ecosystem of plugins
- Dedicated support

**Cons:**
- Operational complexity (separate system to learn)
- Additional dependency to manage
- Licensing costs for enterprise features
- Integration complexity with Spring ecosystem
- Less control over behavior

### Option 3: Service Mesh (Istio, Linkerd)
Implement cross-cutting concerns at network layer

**Pros:**
- Transparent to applications
- Powerful traffic management
- Enhanced observability

**Cons:**
- Significant operational complexity
- Requires Kubernetes
- High resource overhead
- Steep learning curve
- Premature optimization (overkill for Phase 1)

## Consequences

### Positive
- **Centralized Policy**: Rate limits, timeouts, retries defined in one place
- **Security**: Single point to enforce authentication and authorization
- **Consistency**: All responses use same error format
- **Observability**: All requests logged and traced
- **Load Balancing**: Services scale behind gateway without client awareness
- **Service Independence**: Services focus on business logic; gateway handles infrastructure
- **DDoS Protection**: Rate limiting and request filtering at gateway
- **Version Management**: Can support multiple API versions simultaneously
- **Easier Debugging**: Correlation IDs track requests end-to-end
- **Standards Compliance**: Enforce consistent API standards

### Negative
- **Additional Latency**: Extra network hop adds milliseconds to each request
- **Single Point of Failure**: Gateway down means all services unavailable
- **Operational Complexity**: Additional service to monitor and maintain
- **Configuration Burden**: Routing rules must be maintained
- **Resource Overhead**: Gateway uses memory and CPU
- **Debugging Complexity**: Additional layer to troubleshoot
- **Vendor Lock-in**: Spring Cloud Gateway specifics hard to replace
- **Testing Overhead**: Must test gateway in addition to services

### Trade-offs
- **Simplicity vs. Consistency**: Accept gateway complexity for consistent behavior
- **Latency vs. Security**: Accept small latency increase for centralized security
- **Availability vs. Centralization**: Single gateway is bottleneck; mitigate with replication
- **Autonomy vs. Standardization**: Services less autonomous but behavior more predictable

## Future Considerations

1. **API Versioning**: Support multiple API versions simultaneously
   - Routes like `/api/v1/products` and `/api/v2/products`
   - Deprecate old versions over time
   - Backward compatibility for clients

2. **Request Transformation**: Add custom logic
   - Add tenant ID to multi-tenant deployments
   - Transform request format if services use different models
   - Decrypt encrypted payloads

3. **Response Caching**: Cache frequently accessed data
   - Cache product catalog (low frequency updates)
   - Cache warehouse list
   - Dramatically improve response times

4. **GraphQL Gateway**: Add GraphQL layer on top of REST APIs
   - Allow clients to query exactly what they need
   - Reduce over-fetching and under-fetching
   - Better developer experience

5. **Blue-Green Deployment Support**: Route to different service versions
   - Test new versions without affecting all users
   - Gradual traffic shifting
   - Fast rollback if issues found

6. **Request Validation**: Schema-based request validation
   - Fail fast if request format invalid
   - Reduce validation logic in services
   - Consistent error messages

7. **Mock Responses**: Support mock mode for testing
   - Return pre-configured responses
   - Useful for testing without backend
   - Support development and QA environments

## Implementation Guidance

- API Gateway implemented as Spring Cloud Gateway service
- Authentication via filter that validates JWT tokens
- Rate limiting uses Redis for distributed state
- All routes documented in OpenAPI/Swagger
- Gateway deployed with same reliability guarantees as other services
- Configuration externalized to application.yml
- Health endpoint exposes gateway and service health
- Metrics exposed for Prometheus monitoring
- Circuit breakers configured with sensible defaults (3 failures = open, 30 second timeout)
- Request logging includes full payload for debugging (with PII redaction)
- Graceful shutdown waits for in-flight requests
