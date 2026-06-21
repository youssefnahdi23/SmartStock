# ADR-0015: Data Consistency and Saga Pattern for Distributed Transactions

## Status
Accepted

## Context
In a distributed microservices architecture, ACID transactions spanning multiple services are impossible. A single business operation often requires changes across multiple services:

**Example: Create Sales Order**
```
Sales Order Service: Create order (status = PENDING)
  ↓
Inventory Service: Reserve stock (deduct from available)
  ↓
Warehouse Service: Allocate location for picking
  ↓
Notification Service: Send confirmation email
```

If any step fails (e.g., inventory insufficient), the entire operation must rollback. With ACID transactions impossible (each service owns database), how do we maintain data consistency?

This is the **distributed transaction problem**. Bad solutions:
- **Two-Phase Commit (2PC)**: Requires locking; blocks services; poor scalability
- **Distributed ACID**: Not available in PostgreSQL + microservices
- **No Coordination**: Accept inconsistency (dangerous for inventory)

The **Saga Pattern** is the industry-standard solution: coordinate multi-step processes with compensating transactions (rollback logic).

## Decision
Implement **Saga Pattern with Choreography** for distributed transactions:

### 1. **Saga Types**

**Choreography (Event-Driven)**
```
Sales Order Service publishes: OrderCreated
           ↓
Inventory Service consumes: OrderCreated
  - Reserves stock
  - Publishes: StockReserved or StockReservationFailed
           ↓
Warehouse Service consumes: StockReserved
  - Allocates location
  - Publishes: LocationAllocated or AllocationFailed
           ↓
Notification Service consumes: LocationAllocated
  - Sends email
  - Publishes: EmailSent

If any service fails, publishes failure event
           ↓
Other services consume failure event
           ↓
Compensate (rollback)
```

**Orchestration (Centralized Coordinator)**
```
Saga Orchestrator (new service)
       ↓
1. Call Sales Order Service: CreateOrder
2. Wait for response
3. Call Inventory Service: ReserveStock
4. If fails: Call Inventory Service: CancelReservation (compensate)
5. Continue...
```

**Decision**: Use **Choreography** (event-driven)
- Better decoupling (no central coordinator)
- Better scalability (no bottleneck)
- Better resilience (coordinator itself could fail)
- Simpler debugging (events logged)

### 2. **Saga Pattern: Sales Order Example**

**Step 1: Initiate Order**
```java
@Service
public class SalesOrderService {
  @Transactional
  public Order createOrder(CreateOrderRequest request) {
    // Create order locally (COMMITTED immediately)
    Order order = new Order(request.getCustomerId(), 
      OrderStatus.PENDING, request.getLineItems());
    
    repository.save(order); // Local DB commit
    
    // Publish event (triggers saga)
    eventPublisher.publish(new OrderCreated(
      order.getId(),
      order.getCustomerId(),
      order.getLineItems()
    ));
    
    return order;
  }
}
```

**Step 2: Reserve Inventory**
```java
@Component
public class OrderCreatedHandler {
  @KafkaListener(topics = "events.order")
  public void handleOrderCreated(OrderCreated event) {
    try {
      // Try to reserve stock
      inventoryService.reserveStock(
        event.getOrderId(),
        event.getLineItems()
      );
      
      // If success, publish event
      eventPublisher.publish(new StockReserved(
        event.getOrderId(),
        event.getLineItems()
      ));
    } catch (InsufficientStockException e) {
      // If fails, publish failure event
      eventPublisher.publish(new StockReservationFailed(
        event.getOrderId(),
        e.getMessage()
      ));
    }
  }
}
```

**Step 3: Allocate Warehouse Location**
```java
@Component
public class StockReservedHandler {
  @KafkaListener(topics = "events.inventory")
  public void handleStockReserved(StockReserved event) {
    try {
      // Allocate location
      warehouseService.allocateLocation(
        event.getOrderId(),
        event.getLineItems()
      );
      
      // If success, publish event
      eventPublisher.publish(new LocationAllocated(
        event.getOrderId(),
        event.getLineItems()
      ));
    } catch (AllocationFailedException e) {
      // If fails, publish failure event
      eventPublisher.publish(new AllocationFailed(
        event.getOrderId(),
        e.getMessage()
      ));
    }
  }
}
```

**Step 4: Handle Failure and Compensate**
```java
@Component
public class SagaCompensationHandler {
  @KafkaListener(topics = "events.*.failed")
  public void handleSagaFailure(SagaFailureEvent event) {
    // Compensate (rollback) all previous steps
    
    if (event instanceof StockReservationFailed) {
      // Order was created but stock couldn't be reserved
      // Compensation: Cancel order
      salesOrderService.cancelOrder(event.getOrderId());
      
      // Notify user
      notificationService.sendOrderCancelledEmail(event.getOrderId());
    }
    
    if (event instanceof AllocationFailed) {
      // Stock was reserved but location couldn't be allocated
      // Compensation: Release inventory reservation
      inventoryService.cancelReservation(event.getOrderId());
      
      // Cancel order
      salesOrderService.cancelOrder(event.getOrderId());
      
      // Notify user
      notificationService.sendOrderCancelledEmail(event.getOrderId());
    }
  }
}
```

### 3. **Handling Idempotency**

Distributed systems need idempotent handlers (safe to process same event multiple times):

```java
@Component
public class StockReservedHandler {
  @KafkaListener(topics = "events.inventory")
  public void handleStockReserved(StockReserved event) {
    // Check if already processed (idempotency key)
    if (warehouseService.isAllocationProcessed(event.getOrderId())) {
      log.info("Allocation already processed for order: {}", event.getOrderId());
      return; // Idempotent: ignore duplicate event
    }
    
    try {
      warehouseService.allocateLocation(event.getOrderId(), 
        event.getLineItems());
      
      // Mark as processed
      warehouseService.markAsProcessed(event.getOrderId());
      
      eventPublisher.publish(new LocationAllocated(
        event.getOrderId(),
        event.getLineItems()
      ));
    } catch (AllocationFailedException e) {
      eventPublisher.publish(new AllocationFailed(
        event.getOrderId(),
        e.getMessage()
      ));
    }
  }
}
```

### 4. **Eventual Consistency**

During saga execution, services temporarily inconsistent:

```
Time 0: OrderCreated event
  - Order Service: order = PENDING (consistent)
  - Inventory Service: inventory unchanged (inconsistent)

Time 1: StockReserved event
  - Order Service: order = PENDING
  - Inventory Service: reserved stock = true (now consistent)
  - Warehouse Service: location unallocated (inconsistent)

Time 2: LocationAllocated event
  - All services consistent

If StockReservationFailed occurs at Time 1.5:
  - All services compensate
  - Return to consistent state (order cancelled)
```

Users must understand this transient inconsistency:
- Dashboard shows "Order Processing" during saga
- Email confirms "We'll send tracking info shortly"
- Long-running operations show status updates

### 5. **Saga Completion Patterns**

**Happy Path** (all steps succeed)
```
OrderCreated → StockReserved → LocationAllocated → OrderConfirmed
```

**Failure Path** (failure triggers compensation)
```
OrderCreated → StockReserved → StockReservationFailed
  → Release Order (compensation)
  → Send CancelledEmail
```

**Timeout Path** (step takes too long)
```
OrderCreated → StockReserved → [timeout waiting for LocationAllocated]
  → Timeout triggers compensation
  → Release Inventory
  → Cancel Order
```

### 6. **Dead Letter Queue for Failed Sagas**

```java
@Component
public class FailedSagaHandler {
  @KafkaListener(topics = "saga.deadletter")
  public void handleFailedSaga(FailedSagaEvent event) {
    // Log failure for manual intervention
    log.error("Saga failed after all retries: {}", event.getSagaId());
    
    // Alert operations team
    alertService.sendAlert(
      "Saga " + event.getSagaId() + " failed permanently. Manual intervention required."
    );
    
    // Store for debugging
    deadLetterRepository.save(new DeadLetterEntry(
      event.getSagaId(),
      event.getFailureReason(),
      Instant.now()
    ));
  }
}
```

## Alternatives Considered

### Option 1: Two-Phase Commit (2PC)
Distributed ACID transactions

**Pros:**
- Strong consistency
- Familiar pattern

**Cons:**
- Blocking locks (poor performance)
- Cannot scale
- Difficult to debug
- Not supported by microservices
- Single point of failure (transaction coordinator)

### Option 2: Accept Data Inconsistency
No coordination, eventual consistency

**Pros:**
- Simple
- High performance

**Cons:**
- Inventory might be double-sold
- Orders created for unavailable products
- Unacceptable for mission-critical operations

### Option 3: Orchestration (Saga Orchestrator Service)
Central service coordinates saga

**Pros:**
- Easier to understand (explicit flow)
- Simpler debugging

**Cons:**
- Single point of failure (orchestrator down = all sagas blocked)
- Orchestrator becomes bottleneck
- More complex to scale
- Tight coupling (orchestrator knows all services)

## Consequences

### Positive
- **Distributed Consistency**: Multi-service operations maintain logical consistency
- **Scalability**: No blocking locks; services scale independently
- **Resilience**: Failure isolated to specific saga; doesn't cascade
- **Decoupling**: Services don't call each other; communicate via events
- **Auditability**: Events provide complete audit trail of saga execution
- **Rollback Capability**: Compensations provide explicit rollback logic

### Negative
- **Complexity**: Developers must understand saga coordination
- **Eventual Consistency**: Temporary inconsistency visible to users
- **Debugging Difficulty**: Multi-step sagas hard to diagnose
- **Timeout Handling**: Must decide timeout values for each step
- **Idempotency Burden**: Each service must implement idempotency
- **Distributed Tracing Essential**: Cannot debug without good tracing

### Trade-offs
- **Consistency vs. Performance**: Accept eventual consistency for scalability
- **Simplicity vs. Decoupling**: Accept saga complexity for service independence

## Future Considerations

1. **Saga Timeouts**: Implement timeout policies
   - Hard timeout: Saga fails after X seconds
   - Soft timeout: Saga waits longer before compensating

2. **Saga State Machine**: Explicit state machines for complex sagas
   - Define allowed state transitions
   - Prevent invalid operations

3. **Nested Sagas**: Sagas calling other sagas
   - Handle compensation hierarchy
   - Ensure consistency across levels

4. **Monitoring Dashboards**: Visualize saga execution
   - Track saga progress
   - Identify bottlenecks
   - Monitor failure rates

## Implementation Guidance

- Use choreography (event-driven) not orchestration
- Implement idempotency for all saga participants
- Store idempotency keys to detect duplicate processing
- Design compensations for every action
- Set reasonable timeouts for each step
- Log all saga events for auditing
- Monitor saga success/failure rates
- Test failure scenarios explicitly
- Document saga flow in architecture documentation
