# ADR-0011: Caching Strategy - Redis for Performance and Consistency

## Status
Accepted

## Context
SmartStock AI faces caching challenges across different scenarios:
- **Read-Heavy Workloads**: Product catalog queries (thousands of concurrent reads)
- **Hot Data**: Frequently accessed inventory levels (warehouse managers refresh frequently)
- **Session Management**: Desktop client and API sessions
- **Rate Limiting**: Tracking API request counts per user
- **Distributed Locks**: Prevent double-processing of events
- **Cache Invalidation**: Ensuring data consistency when updates occur

Without a caching strategy:
- Database under excessive load for read requests
- Slow response times for users
- High latency for critical operations
- Difficult to implement rate limiting
- Expensive database replication to handle scale

The challenge is balancing **performance** (cache everything) with **consistency** (cache nothing).

## Decision
Implement **Redis-Based Distributed Cache** with event-driven invalidation:

### 1. **Redis Deployment Model**

**Development/Testing**: Single Redis instance
- Simple setup (docker run -p 6379:6379 redis:latest)
- Suitable for local development

**Staging/Production**: Redis Cluster
- Cluster Mode enabled (16 shards, 3 replicas each)
- Automatic failover and rebalancing
- Persistence enabled (RDB + AOF)
- Backup: Scheduled snapshots (daily), replication to backup Redis

### 2. **Caching Layers**

**Layer 1: HTTP Caching (API Gateway)**
- Cache HTTP responses at Gateway level
- Cache-Control headers: `public, max-age=300` (5 minutes)
- Suitable for: Product catalog, warehouse list, category list
- Invalidated: When product service publishes ProductUpdated event

**Layer 2: Application-Level Caching (Services)**
- Cache objects within services
- TTL-based expiration (prevents stale data)
- Event-driven invalidation (explicit)

```java
// Product Service caches product objects
@Service
public class ProductService {
  private final RedisTemplate<String, Product> cache;
  
  @Cacheable(value = "products", key = "#productId")
  public Product getProductById(UUID productId) {
    // Query database only on cache miss
    return repository.findById(productId).orElseThrow();
  }
  
  @CacheEvict(value = "products", key = "#productId")
  public void updateProduct(UUID productId, UpdateProductRequest request) {
    // Update database
    Product updated = repository.update(productId, request);
    // Publish event to invalidate caches in other services
    publishEvent(new ProductUpdated(productId, updated));
  }
}
```

**Layer 3: Session Storage (Redis)**
- Desktop client sessions
- API refresh tokens
- User preferences
- TTL: 8 hours for sessions, 30 days for refresh tokens

### 3. **Cache Key Naming Convention**

Consistent, hierarchical naming prevents collisions:
```
products:{productId}              -- Product object
product:catalog:{categoryId}      -- Products in category
inventory:{productId}:{warehouseId} -- Stock level
warehouse:{warehouseId}           -- Warehouse details
user:session:{sessionId}          -- User session
rate-limit:{userId}               -- Request count for user
lock:order-process:{orderId}      -- Distributed lock
```

### 4. **Invalidation Strategy**

**Event-Driven Invalidation**
When a business event is published, dependent caches are invalidated:

```
ProductCreated Event
├─ Invalidate: products:{productId}
└─ Invalidate: product:catalog:{categoryId}

StockIn Event
├─ Invalidate: inventory:{productId}:{warehouseId}
└─ Invalidate: warehouse:{warehouseId} (summary cache)

ProductUpdated Event
├─ Invalidate: products:{productId}
├─ Invalidate: product:catalog:{oldCategory}
└─ Invalidate: product:catalog:{newCategory}
```

**Implementation**
```java
@Component
public class CacheInvalidationListener {
  @KafkaListener(topics = "events.product")
  public void handleProductEvent(ProductEvent event) {
    switch (event.getType()) {
      case CREATED, UPDATED -> {
        cache.delete("products:" + event.getProductId());
        cache.delete("product:catalog:" + event.getCategoryId());
      }
      case DELETED -> {
        cache.delete("products:" + event.getProductId());
      }
    }
  }
}
```

### 5. **Cache Allocation Strategy**

**Caching Decisions** (what to cache, what not to cache)

**Cache (High Read, Low Update)**
- Product catalog (update 1x/month, read 1000x/day)
- Warehouse list (static, updated rarely)
- User roles/permissions (updated occasionally)
- Category list (static)
- Supplier list (updated weekly)

**Don't Cache (Frequently Updated)**
- Real-time inventory (updated constantly during operations)
- Orders (state changes frequently)
- User activity (audit purposes)

**Conditional Caching (Complex Decision)**
- Inventory snapshots: Cache for 1 minute (balance consistency with performance)
- Warehouse utilization: Cache for 5 minutes
- Supplier performance: Cache for 1 hour

### 6. **Consistency Guarantees**

**Strong Consistency** (for critical data)
- Inventory levels: Read-through cache (always check DB)
- Order status: Write-through cache (update cache and DB together)

**Eventual Consistency** (for non-critical data)
- Product catalog: Cache-aside pattern (lazy load, event-driven invalidation)
- Warehouse details: TTL-based (expires after 5 minutes)

**Distributed Locks** (prevent double-processing)
```java
// Ensure stock adjustment happens exactly once
public void adjustInventory(UUID inventoryId, int quantity) {
  String lockKey = "lock:adjust-inventory:" + inventoryId;
  
  if (cache.setIfAbsent(lockKey, "locked", Duration.ofSeconds(30))) {
    try {
      // Perform adjustment
      inventory.adjust(quantity);
      publishEvent(new InventoryAdjusted(inventoryId, quantity));
    } finally {
      cache.delete(lockKey);
    }
  } else {
    // Another thread already processing; wait and retry
    throw new ConcurrentModificationException("Inventory being adjusted");
  }
}
```

### 7. **Monitoring and Metrics**

**Cache Hit Ratio**
- Track: hits / (hits + misses)
- Goal: > 80% for product catalog, > 60% for inventory
- Alert if < 50% (indicates cache misconfiguration)

**Cache Memory Usage**
- Monitor Redis memory consumption
- Alert if > 80% of allocated memory
- Implement LRU eviction policy (evict least recently used)

**Cache Invalidation Lag**
- Measure time between event and cache invalidation
- Goal: < 100ms
- Alert if > 1 second (indicates event processing delay)

## Alternatives Considered

### Option 1: No Caching (Database Only)
All requests go directly to database

**Pros:**
- Always consistent data
- Simple to implement
- No cache invalidation complexity

**Cons:**
- Database cannot scale to thousands of concurrent requests
- High latency (each request hits disk)
- Expensive database replication
- Cannot support rate limiting or session management
- Limits system scalability

### Option 2: Application-Level Caching (No Redis)
Embed cache in application process (Spring Cache with local HashMap)

**Pros:**
- Simpler initially (no Redis cluster)
- Fast access (in-process)

**Cons:**
- Cannot share cache between service instances
- Difficult to invalidate cache across instances
- High memory overhead (replicate cache in each instance)
- No distributed locks
- Cannot share sessions between app servers

### Option 3: CDN Caching (CloudFront, Akamai)
Cache content at edge locations

**Pros:**
- Global edge presence
- Excellent for static content
- Reduces origin load

**Cons:**
- Expensive
- Not suitable for dynamic content
- Long cache invalidation delays
- Not suitable for real-time inventory
- Difficult to handle regional compliance

## Consequences

### Positive
- **Performance**: Dramatic reduction in response times (10x-100x faster)
- **Database Relief**: Reduce database load by 80%+
- **Scalability**: Support thousands of concurrent users
- **Session Management**: Distributed sessions for load balancing
- **Rate Limiting**: Efficient request counting across services
- **Distributed Locks**: Prevent race conditions across services
- **Cost Reduction**: Reduced database replication costs
- **Availability**: Cache provides read availability during DB maintenance
- **User Experience**: Sub-100ms responses for cached data

### Negative
- **Additional Infrastructure**: Redis cluster to operate
- **Consistency Challenges**: Stale cache can show outdated data
- **Invalidation Complexity**: Must invalidate cache whenever data changes
- **Memory Cost**: Cache requires significant memory allocation
- **Failure Impact**: Redis down means degraded performance (fallback to DB)
- **Debugging Difficulty**: Harder to debug consistency issues
- **TTL Decisions**: Wrong TTL causes either stale data or poor cache hit ratio

### Trade-offs
- **Consistency vs. Performance**: Accept eventual consistency for better performance
- **Simplicity vs. Scalability**: Accept cache complexity for 10x scalability
- **Infrastructure Cost vs. Database Cost**: Invest in Redis to reduce database cost

## Future Considerations

1. **Cache-Aside vs. Write-Through**: Different strategies for different scenarios
   - Cache-aside: Good for reads, complex invalidation
   - Write-through: Better consistency, slower writes

2. **Bloom Filters**: Prevent cache lookups for non-existent items
   - Check if item might exist before querying DB
   - Reduce database queries for missing items

3. **Tiered Caching**: Combine local and distributed caches
   - Local cache (in-process) for very hot data
   - Distributed cache (Redis) for shared data
   - Reduces Redis load

4. **Cache Warming**: Pre-populate cache on startup
   - Load all products on cache startup
   - Improve hit ratio from minute 1
   - Ensure critical data always available

5. **Conditional Caching**: Cache based on request characteristics
   - Cache only if result has many items (skip small results)
   - Cache only for read-only API calls
   - Don't cache for internal service calls

6. **Cache Compression**: Compress cached objects
   - Reduce memory usage
   - Trade CPU for memory

## Implementation Guidance

- Use Spring Data Redis with RedisTemplate for Java services
- Implement cache-aside pattern: check cache first, fallback to database
- Set TTL on all cache entries (prevent unbounded growth)
- Monitor cache hit ratio and adjust TTL accordingly
- Implement cache invalidation listeners for all Kafka events
- Use distributed locks for critical sections
- Implement circuit breaker: if Redis down, fall back to direct DB access
- Profile cache allocation: measure hit ratios weekly
- Document caching decisions in service README
- Rate limiting implemented via Redis INCR with TTL
