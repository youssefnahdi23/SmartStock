# ADR-0013: Resilience Patterns - Circuit Breakers, Retries, and Timeout Management

## Status
Accepted

## Context
In a distributed microservices architecture, partial failures are inevitable:
- **Transient Failures**: Network hiccup (recovered in milliseconds)
- **Cascading Failures**: One service down triggers failures in dependent services
- **Resource Exhaustion**: Database connection pool exhausted causes timeouts
- **Thundering Herd**: All clients retry simultaneously after failure

Without resilience patterns, a single service failure cascades to entire system:
```
Customer Service down
  ↓
Order Service fails (depends on Customer Service)
  ↓
Reporting Service fails (depends on Order Service)
  ↓
Desktop Client hangs (waiting for response)
  ↓
Users experience outage
```

Resilience patterns are essential to **contain failures** and **enable graceful degradation**.

## Decision
Implement **Multi-Layer Resilience Strategy** using proven patterns:

### 1. **Retry Pattern**

**Strategy**: Automatically retry transient failures with exponential backoff

**Implementation**
```java
@Service
public class ProductService {
  private final RestTemplate restTemplate;
  
  @Retryable(
    value = { TemporaryServiceException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2.0)
  )
  public Product getProductFromRemote(UUID productId) {
    // Retry on failure: 100ms, 200ms, 400ms
    return restTemplate.getForObject(
      "http://product-service/api/v1/products/{id}",
      Product.class,
      productId
    );
  }
  
  @Recover
  public Product handleRetryFailure(TemporaryServiceException e, UUID productId) {
    // After 3 retries fail, use fallback
    return getProductFromCache(productId);
  }
}
```

**Retry Configuration**
- **Transient Errors**: Connection timeout, 503 Service Unavailable, 504 Gateway Timeout
- **Non-Transient**: 400 Bad Request, 401 Unauthorized, 404 Not Found (don't retry)
- **Backoff**: Exponential with jitter to prevent thundering herd
- **Max Retries**: 3 (balance between resilience and latency)

### 2. **Circuit Breaker Pattern**

**Strategy**: Stop calling failing service; fail fast; recover when service recovers

**States**
```
CLOSED (normal operation)
  ↓ (after N failures)
OPEN (fail immediately, don't call service)
  ↓ (after timeout period)
HALF_OPEN (test if service recovered)
  ↓ (if success)
CLOSED (back to normal)
  ↓ (if still failing)
OPEN (back to failing fast)
```

**Implementation**
```java
@Service
@CircuitBreaker(
  name = "inventory-service",
  config = "inventoryCircuitBreakerConfig"
)
public class InventoryClient {
  public StockLevel getInventory(UUID productId, UUID warehouseId) {
    return restTemplate.getForObject(
      "http://inventory-service/api/v1/inventory/{productId}/{warehouseId}",
      StockLevel.class,
      productId,
      warehouseId
    );
  }
}

@Configuration
public class CircuitBreakerConfiguration {
  @Bean
  public CircuitBreakerConfigCustomizer inventoryCircuitBreakerConfig() {
    return config -> config
      .circuitBreakerConfig()
      .failureRateThreshold(50) // Open after 50% failures
      .slowCallRateThreshold(50) // Open if 50% calls take > 2s
      .slowCallDurationThreshold(Duration.ofSeconds(2))
      .waitDurationInOpenState(Duration.ofSeconds(30)) // Try recovery after 30s
      .permittedNumberOfCallsInHalfOpenState(3) // Test with 3 calls
      .minimumNumberOfCalls(10); // Evaluate after 10 calls
  }
}
```

**Metrics**
- Monitor: % of calls that hit circuit breaker
- Alert: If circuit open for > 5 minutes (service degradation)
- Action: Page on-call engineer

### 3. **Timeout Pattern**

**Strategy**: Fail fast rather than wait indefinitely for response

**Configuration**
```java
@Configuration
public class HttpClientConfiguration {
  @Bean
  public RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = 
      new HttpComponentsClientHttpRequestFactory();
    
    // Connection timeout: time to establish TCP connection
    factory.setConnectTimeout(1000); // 1 second
    
    // Read timeout: time waiting for response
    factory.setReadTimeout(3000); // 3 seconds
    
    // Total timeout: request + all retries
    return new RestTemplate(factory);
  }
}
```

**Timeout Strategy**
```
Connection Timeout: 1 second (establish TCP connection)
Read Timeout: 3 seconds (waiting for response from server)
Request Timeout: 5 seconds (max time for entire request)

If timeout occurs:
  → Retry with exponential backoff (if transient)
  → Open circuit breaker (if repeated)
  → Fail fast (after circuit open)
```

### 4. **Bulkhead Pattern**

**Strategy**: Isolate resources to prevent one failure from consuming all resources

**Thread Pool Isolation**
```java
@Service
public class OrderService {
  private final ExecutorService inventoryThreadPool = 
    Executors.newFixedThreadPool(10);
  
  private final ExecutorService warehouseThreadPool = 
    Executors.newFixedThreadPool(5);
  
  public Order createOrder(OrderRequest request) {
    // Reserve inventory (10 threads)
    Future<Void> inventoryReserve = inventoryThreadPool.submit(() -> 
      inventoryService.reserveStock(request.getLineItems())
    );
    
    // Allocate warehouse location (5 threads)
    Future<Void> warehouseAllocate = warehouseThreadPool.submit(() -> 
      warehouseService.allocateLocation(request.getLineItems())
    );
    
    // If inventory thread pool exhausted, inventory fails but warehouse works
    // If warehouse thread pool exhausted, warehouse waits but inventory continues
  }
}
```

**Connection Pool Isolation**
```yaml
spring:
  datasource:
    # Primary operations database
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 1000
  
  # Separate pool for reporting queries
  reporting-datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000
```

### 5. **Fallback Pattern**

**Strategy**: Provide degraded service when primary service unavailable

**Examples**
```java
// Fallback to cached data
@Retryable(maxAttempts = 3)
public Product getProduct(UUID productId) {
  return productService.get(productId);
}

@Recover
public Product fallback(Exception e, UUID productId) {
  // Try cache first
  Product cached = cache.get(productId);
  if (cached != null) {
    return cached; // Return stale data
  }
  
  // If cache miss, throw error
  throw new ServiceUnavailableException("Product service unavailable");
}

// Fallback to default value
public List<Supplier> getSuppliers() {
  try {
    return supplierService.getAll();
  } catch (Exception e) {
    log.warn("Supplier service unavailable, returning empty list");
    return Collections.emptyList();
  }
}

// Fallback to async processing
public void processOrder(Order order) {
  try {
    // Try synchronous processing
    warehouseService.allocateLocation(order);
  } catch (TimeoutException e) {
    // Fall back to async queue
    asyncQueue.enqueue(() -> warehouseService.allocateLocation(order));
    return order.asProcessing();
  }
}
```

### 6. **Rate Limiting (Backpressure)**

**Strategy**: Prevent overwhelming dependent services

**Implementation**
```java
@Service
public class InventoryService {
  private final RateLimiter rateLimiter = 
    RateLimiter.create(100); // 100 requests/second
  
  public void adjustStock(InventoryAdjustment adjustment) {
    if (!rateLimiter.tryAcquire()) {
      throw new TooManyRequestsException("Rate limit exceeded");
    }
    
    // Process request
    doAdjustStock(adjustment);
  }
}
```

**Per-User Rate Limiting**
```java
@Component
public class PerUserRateLimiter {
  private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
  
  public void checkLimit(String userId) {
    RateLimiter limiter = limiters.computeIfAbsent(userId, 
      u -> RateLimiter.create(10)); // 10 reqs/sec per user
    
    if (!limiter.tryAcquire()) {
      throw new TooManyRequestsException("User rate limit exceeded");
    }
  }
}
```

## Alternatives Considered

### Option 1: No Resilience (Let Failures Cascade)
No retry, circuit breaker, or timeout

**Pros:**
- Simpler code initially

**Cons:**
- Single service failure cascades to entire system
- User experiences total outage
- Cascading failures very difficult to recover from
- Unacceptable for enterprise system

### Option 2: Synchronous Request-Reply Only
Make all calls synchronous without resilience

**Pros:**
- Simpler mental model

**Cons:**
- No decoupling between services
- Failures cascade immediately
- Impossible to scale

## Consequences

### Positive
- **Fault Containment**: Failures isolated; don't cascade
- **Graceful Degradation**: System continues at reduced capacity
- **Fast Failure**: Fail fast rather than wait indefinitely
- **Automatic Recovery**: Circuit breaker enables recovery
- **Resource Protection**: Bulkheads prevent resource exhaustion
- **User Experience**: System continues serving users despite partial failures
- **Operational Resilience**: Team can debug issues without massive outages

### Negative
- **Complexity**: Multiple patterns to learn and implement
- **Debugging Difficulty**: Retries and circuit breakers make debugging harder
- **Stale Data**: Fallbacks may return outdated information
- **Latency**: Retries with backoff increase latency
- **Configuration Burden**: Retry policies, timeouts, thresholds must be tuned
- **Cascading Delays**: Timeout in one layer can cascade to others

### Trade-offs
- **Simplicity vs. Resilience**: Accept complexity for fault tolerance
- **Latency vs. Reliability**: Accept latency increases for better recovery

## Future Considerations

1. **Chaos Engineering**: Intentionally fail components to test resilience
   - Kill instances randomly
   - Induce network latency
   - Simulate database failures
   - Verify system degrades gracefully

2. **Adaptive Retry**: Learn optimal retry parameters
   - Measure latency distributions
   - Auto-adjust timeout and retry counts
   - Improve over time

3. **Service Mesh**: Implement resilience at network layer (Istio, Linkerd)
   - Transparent retries without code changes
   - Circuit breaker at network level
   - Distributed tracing for debugging

4. **Bulkhead as a Service**: Dedicated execution isolation service
   - External service manages thread pools
   - Reduces per-service configuration

## Implementation Guidance

- Use Resilience4j library for circuit breakers, retries, timeouts
- Configure reasonable defaults; override for specific services
- Monitor circuit breaker state; alert if open > 5 minutes
- Implement fallbacks for critical paths
- Test resilience behavior (mock failures, verify degradation)
- Document timeout and retry policies in service README
- All external calls should have timeout configured
- Circuit breaker metrics exposed for monitoring
