# ADR-0014: Testing Strategy - Unit, Integration, and Contract Tests

## Status
Accepted

## Context
In a distributed microservices architecture, testing becomes significantly more complex:
- **Local Testing Insufficient**: Unit tests in isolation don't catch inter-service issues
- **Database Testing**: Tests need real databases or sophisticated mocks
- **Async Testing**: Event-driven workflows difficult to test
- **Contract Testing**: Ensure services honor agreed-upon APIs
- **Integration Testing**: Must test with real infrastructure (Kafka, Redis, PostgreSQL)

Without comprehensive testing strategy:
- Hidden bugs reach production
- Service changes break dependent services
- Refactoring becomes risky
- Debugging in production becomes necessary
- Team confidence in deployments low

The challenge is balancing **test coverage** (comprehensive) with **test execution time** (fast feedback loop).

## Decision
Implement **Three-Level Testing Strategy**:

### 1. **Level 1: Unit Tests (Fast, Isolated)**

**Scope**: Test business logic in isolation
- Input/output of individual functions/methods
- No external dependencies (databases, APIs, etc.)
- Mock all dependencies (cache, API clients, message queues)
- Run in seconds
- Enable rapid feedback during development

**Target Coverage**: 80%+ of business logic

**Example: Inventory Service**
```java
@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {
  
  @Mock private InventoryRepository repository;
  @Mock private ProductClient productClient;
  @Mock private KafkaTemplate<String, Event> kafka;
  
  @InjectMocks private InventoryService service;
  
  @Test
  void shouldAdjustInventory() {
    // Arrange
    UUID inventoryId = UUID.randomUUID();
    when(repository.findById(inventoryId)).thenReturn(
      Optional.of(new Inventory(inventoryId, 100))
    );
    
    // Act
    service.adjustInventory(inventoryId, 25);
    
    // Assert
    verify(repository).update(argThat(inv -> inv.getQuantity() == 125));
    verify(kafka).send(argThat(msg -> 
      msg.getPayload().getEventType() == EventType.INVENTORY_ADJUSTED
    ));
  }
  
  @Test
  void shouldThrowWhenNegativeQuantity() {
    // Arrange
    UUID inventoryId = UUID.randomUUID();
    
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () ->
      service.adjustInventory(inventoryId, -10)
    );
  }
}
```

### 2. **Level 2: Integration Tests (Moderate Speed, Real Dependencies)**

**Scope**: Test service with real databases, caches, message queues
- Test against real PostgreSQL (in Docker)
- Test Kafka message publishing/consumption
- Test Redis caching behavior
- Test repository layer logic
- Test API endpoints with Spring TestClient
- Run in seconds to minutes
- Catch configuration and integration bugs

**Target Coverage**: 50%+ of critical paths

**Example: Inventory Service Integration Test**
```java
@SpringBootTest
@Testcontainers
public class InventoryServiceIntegrationTest {
  
  @Container static PostgreSQLContainer<?> postgres = 
    new PostgreSQLContainer<>("postgres:15-alpine");
  
  @Container static KafkaContainer kafka = 
    new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
  
  @Autowired private TestRestTemplate rest;
  @Autowired private InventoryRepository repository;
  @Autowired private KafkaTemplate<String, Event> kafka;
  @Autowired private TestcontainersListener listener;
  
  @Test
  void shouldCreateAdjustmentAndPublishEvent() throws InterruptedException {
    // Arrange
    UUID inventoryId = UUID.randomUUID();
    Inventory inv = new Inventory(inventoryId, 100);
    repository.save(inv);
    
    AdjustmentRequest request = new AdjustmentRequest(25, "Recount");
    
    // Act
    ResponseEntity<AdjustmentResponse> response = rest.postForEntity(
      "/api/v1/inventory/{id}/adjust",
      request,
      AdjustmentResponse.class,
      inventoryId
    );
    
    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    
    Inventory updated = repository.findById(inventoryId).orElseThrow();
    assertThat(updated.getQuantity()).isEqualTo(125);
    
    // Verify event published
    listener.awaitMessage("inventory.events", 1, Duration.ofSeconds(5));
  }
}
```

### 3. **Level 3: Contract Tests (Slow, Multi-Service)**

**Scope**: Verify service-to-service communication contracts
- Test that service implements expected API contract
- Verify request/response format
- Test error scenarios
- Verify event contracts (schema, required fields)
- Run less frequently (before deployments)
- Catch breaking changes before they reach production

**Example: Product Service Provides Contract**
```java
@SpringBootTest
public class ProductServiceContractTest {
  
  @Autowired private TestRestTemplate rest;
  
  @Test
  @PactTestFor(providerName = "ProductService", 
               consumerName = "InventoryService")
  void shouldReturnProductDetails(MockServer mockServer) {
    // Arrange: Define expected contract
    PactDslJsonBody expectedBody = new PactDslJsonBody()
      .stringType("id", "prod-123")
      .stringType("name", "Widget")
      .decimalType("price", 9.99)
      .stringType("sku", "SKU-001");
    
    mockServer
      .given("product prod-123 exists")
      .uponReceiving("request for product details")
      .path("/api/v1/products/prod-123")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(expectedBody)
      .toPact();
    
    // Act
    ResponseEntity<Product> response = rest.getForEntity(
      "/api/v1/products/prod-123",
      Product.class
    );
    
    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getName()).isEqualTo("Widget");
  }
}
```

**Event Contract Testing**
```java
public class InventoryEventContractTest {
  
  @Test
  void stockInEventShouldContainRequiredFields() {
    // Verify event schema
    EventSchema schema = eventRegistry.getSchema("StockIn", 1);
    
    assertThat(schema.getRequiredFields()).containsExactlyInAnyOrder(
      "eventId", "eventType", "productId", "warehouseId", 
      "quantity", "timestamp", "userId", "correlationId"
    );
    
    assertThat(schema.getFieldType("quantity")).isEqualTo(FieldType.INTEGER);
    assertThat(schema.getFieldType("timestamp")).isEqualTo(FieldType.TIMESTAMP);
  }
}
```

### 4. **Test Pyramid**

```
        /\
       /  \  E2E Tests (slow, test full system)
       5% (if needed)
      /    \
     /______\
     /\      \
    /  \      \ Contract Tests (verify service boundaries)
    / 15%\     \
   /______\____\
   /\          \
  /  \          \ Integration Tests (real DB, cache, messaging)
  / 35%\         \
 /______\________\
 /\              \
/  \              \ Unit Tests (isolated, mocked)
/ 50%\             \
/______\____________\
```

**Test Execution Times**
- Unit tests: < 1 second (10,000+ tests OK)
- Integration tests: 5-30 seconds
- Contract tests: 10-60 seconds
- Full pipeline: < 2 minutes (fast feedback)

### 5. **Testing Best Practices**

**Test Naming Convention**
```
shouldDoX_WhenYOccurs_ThenZHappens

Examples:
- shouldReserveStock_WhenAvailableQuantitySufficient_ThenReservationCreated
- shouldThrow_WhenInsufficientStock_ThenReservationFails
- shouldPublishEvent_WhenStockAdjusted_ThenEventPublishedToKafka
```

**Test Data Strategy**
```java
@Component
public class TestDataBuilder {
  public Product productWithDefaults() {
    return new Product()
      .id(UUID.randomUUID())
      .name("Test Product")
      .sku("TEST-SKU")
      .price(new BigDecimal("9.99"));
  }
  
  public Inventory inventoryWithDefaults() {
    return new Inventory()
      .id(UUID.randomUUID())
      .productId(UUID.randomUUID())
      .warehouseId(UUID.randomUUID())
      .quantity(100);
  }
}
```

**Async Testing**
```java
@Test
void shouldProcessEventuallyConsistentData() throws InterruptedException {
  // Act
  service.publishEvent(new StockIn(...));
  
  // Assert: Wait for eventual consistency
  await()
    .atMost(Duration.ofSeconds(5))
    .pollInterval(Duration.ofMillis(100))
    .untilAsserted(() -> {
      Inventory updated = repository.findById(inventoryId).orElseThrow();
      assertThat(updated.getQuantity()).isEqualTo(150);
    });
}
```

### 6. **Continuous Integration Pipeline**

```
Commit → Compile → Unit Tests → Integration Tests → Code Quality
                       ↓            ↓                    ↓
                    < 10s         < 30s             < 5s
                     Fail?        Fail?              Fail?
                       ↓            ↓                  ↓
                     STOP         STOP              STOP
                       
    Success → Contract Tests → Package → Deploy to Staging
                  < 60s           < 5s
                  Fail? ──→ STOP
                  
    Staging Tests (smoke tests, performance tests, security scan)
                         ↓
                     Success?
                         ↓
              Manual Approval (for production)
                         ↓
           Deploy to Production (blue-green)
                         ↓
              Smoke Tests in Production
```

## Alternatives Considered

### Option 1: Unit Tests Only
No integration or contract tests

**Pros:**
- Fast execution
- Simple setup

**Cons:**
- Hidden bugs in integration
- Breaking changes reach production
- False confidence (tests pass, production fails)

### Option 2: E2E Tests Only
Test entire system end-to-end

**Pros:**
- Comprehensive coverage
- Catches real issues

**Cons:**
- Extremely slow (5+ minutes per test)
- Difficult to diagnose failures
- Flaky (timing issues, external services)
- Not suitable for rapid feedback loop

### Option 3: No Automated Tests
Manual testing before release

**Pros:**
- Simple (no test code to maintain)

**Cons:**
- Extremely slow
- Errors reached production
- Regression bugs missed
- Unacceptable for enterprise

## Consequences

### Positive
- **Bug Prevention**: Comprehensive tests catch issues before production
- **Regression Prevention**: Refactoring safe; tests verify no breakage
- **Confidence**: Team confident in deployments
- **Documentation**: Tests document expected behavior
- **Contract Verification**: Service contracts verified before deployment
- **Design Feedback**: Hard-to-test code indicates poor design
- **Rapid Feedback**: Unit tests run in seconds

### Negative
- **Development Time**: Writing tests requires effort
- **Test Maintenance**: Tests must be maintained as code changes
- **Complex Setup**: Integration tests require infrastructure
- **Slowness**: Full pipeline takes minutes
- **Brittleness**: Integration tests more flaky

### Trade-offs
- **Coverage vs. Speed**: Accept slower tests for higher confidence
- **Effort vs. Reliability**: Accept effort writing tests for fewer production bugs

## Future Considerations

1. **Mutation Testing**: Verify test quality
   - Introduce random mutations into code
   - Verify tests catch mutations
   - Identify weak tests

2. **Performance Testing**: Verify system meets latency/throughput targets
   - Load test individual services
   - Load test entire system
   - Identify bottlenecks

3. **Security Testing**: Automated security checks
   - OWASP Top 10 scan
   - Dependency scanning for vulnerabilities
   - SQL injection testing

4. **Chaos Testing**: Verify resilience
   - Randomly kill services
   - Introduce network latency
   - Verify system degrades gracefully

## Implementation Guidance

- Unit tests run in CI/CD for every commit
- Integration tests run nightly (slower feedback acceptable)
- Contract tests run before production deployment
- Test code quality monitored (same standards as production code)
- Minimum 80% coverage for business logic
- PactFlow for contract testing (cross-team verification)
- Testcontainers for infrastructure (PostgreSQL, Kafka, Redis)
- Mockito for unit test mocking
