# SmartStock AI — Implementation Backlog

**Document Status:** Authoritative Planning Document  
**Generated:** 2026-06-24  
**Source:** Implementation Roadmap, ADRs 0001–0017, API Catalog (125+ endpoints), Event Catalog (32+ events), Database Specs (94 tables), Source Code audit  
**Format:** Milestone → Epic → Task (ID, Title, Description, Effort, Priority, Dependencies, DoD Reference)

---

## Backlog Legend

### Effort Scale
| Code | Developer-Days | Productive Hours |
|---|---|---|
| S | 1–2 days | 6–12 hours |
| M | 3–5 days | 18–30 hours |
| L | 6–10 days | 36–60 hours |
| XL | 11–20 days | 66–120 hours |

### Priority Scale
| Priority | Description |
|---|---|
| Critical | Blocks all other tasks; must be done first |
| High | On the critical path; delays cascade |
| Medium | Important for milestone gate but has workarounds |
| Low | Quality/polish; can be deferred if needed |

### DoD Reference
See `definition-of-done.md` for full criteria. Section references used in this backlog:
- `DoD-Global` — applies to every task
- `DoD-DomainModel` — JPA entity implementation
- `DoD-Repository` — Spring Data JPA layer
- `DoD-AppService` — Application service layer
- `DoD-REST` — REST controller layer
- `DoD-KafkaProducer` — Event publishing
- `DoD-KafkaConsumer` — Event consumption
- `DoD-DBMigration` — Flyway migration
- `DoD-UnitTest` — Unit test class
- `DoD-IntTest` — Integration test
- `DoD-OpenAPI` — API documentation
- `DoD-Security` — Security implementation
- `DoD-Observability` — Metrics/traces/logs
- `DoD-Performance` — Latency targets
- `DoD-MilestoneGate` — Gate criteria

---

## M0 — Infrastructure & Foundation (COMPLETE)

All M0 items are verified complete per BOOTSTRAP_SUMMARY.md and source code audit. No open tasks.

---

## M1 — Identity & Security Foundation

**Total M1 Estimate:** 40 developer-days  
**Milestone Date:** 2026-06-30 to 2026-08-24

### M1-E1 — Common Module Hardening

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E1-T1 | Fix DomainEvent timestamp to Instant | Change `LocalDateTime timestamp` to `Instant timestamp` in DomainEvent.java. Update `DomainEvent(String, String, String)` constructor to use `Instant.now()`. Update all references in UserCreatedEvent and UserAuthenticatedEvent stubs. Rationale: ADR-0002 requires ISO-8601; database spec requires TIMESTAMP WITH TIME ZONE. | S (1d) | Critical | None | DoD-Global, DoD-DomainModel |
| M1-E1-T2 | Add exception hierarchy | Create `BusinessException` (abstract, HTTP 400/422 default), `ResourceNotFoundException` (HTTP 404), `ValidationException` (HTTP 422, holds field-level errors list), `ConflictException` (HTTP 409). All in `com.smartstock.common.exception`. Each carries a message code for i18n readiness. | S (1d) | Critical | M1-E1-T1 | DoD-Global |
| M1-E1-T3 | Add GlobalExceptionHandler | `@ControllerAdvice GlobalExceptionHandler` mapping all exception types to `ApiResponse.error(...)`. Map `MethodArgumentNotValidException` to 422 with field-level error list. Map `ConstraintViolationException` to 400. Map `Exception` to 500. | S (1d) | Critical | M1-E1-T2 | DoD-Global, DoD-REST |
| M1-E1-T4 | Add CorrelationId propagation | `CorrelationIdFilter` (OncePerRequestFilter): reads `X-Correlation-Id` header (generates UUID if absent), stores in MDC key `correlationId`, sets response header. `ApiResponse` already has `correlationId` field — wire it here. | S (1d) | High | M1-E1-T1 | DoD-Global, DoD-Observability |
| M1-E1-T5 | Add JwtTokenProvider | `JwtTokenProvider` class in `com.smartstock.common.security`. Methods: `generateAccessToken(userId, username, roles, warehouseIds)` → signed JWT (RS256, 4096-bit key pair); `generateRefreshToken(userId)` → signed JWT; `validateToken(String)` → boolean; `parseClaims(String)` → `JwtClaims` record (sub, username, roles, warehouseIds, exp). Key pair loaded from env var `JWT_PRIVATE_KEY_PEM` / `JWT_PUBLIC_KEY_PEM`. | M (3d) | Critical | M1-E1-T1 | DoD-Global, DoD-Security |
| M1-E1-T6 | Add PageRequest and PageResponse DTOs | `PageRequest(int page, int size, String sortBy, String sortDir)` with validation constraints (size max 100). `PageResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size)`. MapStruct mapping helper for Spring Pageable ↔ PageRequest. | S (1d) | High | M1-E1-T1 | DoD-Global |
| M1-E1-T7 | Unit tests for common module | Tests for `JwtTokenProvider`: generate access token, validate valid token, reject expired token, reject tampered signature, parse claims correctly. Tests for `GlobalExceptionHandler`: each exception type maps to correct HTTP status. Tests for `CorrelationIdFilter`: correlation ID appears in MDC and response header. Target: ≥90% line coverage on `common` module. | S (2d) | High | M1-E1-T5 | DoD-UnitTest |

**M1-E1 Subtotal: 10 developer-days**

---

### M1-E2 — Identity Service Application Layer

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E2-T1 | Implement AuthenticationService | `login(String username, String password)`: load user by username, check active/locked, verify BCrypt hash, call `recordLogin()`, publish UserAuthenticatedEvent via Kafka, return TokenPair. `logout(String userId, String refreshToken)`: mark refresh_token.revoked=true. `refreshToken(String refreshToken)`: validate, issue new access token. `validateToken(String token)`: parse and return JwtClaims. | M (4d) | Critical | M1-E1-T5 | DoD-AppService |
| M1-E2-T2 | Implement UserManagementService | `createUser(CreateUserRequest)`: validate unique email/username, hash password, save User, assign default VIEWER role, publish UserCreatedEvent. `getUserById(UUID)`: throws ResourceNotFoundException if not found or soft-deleted. `updateUser(UUID, UpdateUserRequest)`: partial update, publish UserUpdatedEvent. `deactivateUser(UUID)`: soft delete (set deletedAt, active=false). `listUsers(PageRequest, UserFilter)`: paginated with soft-delete exclusion. | M (3d) | Critical | M1-E1-T2 | DoD-AppService |
| M1-E2-T3 | Implement RoleService | `createRole(CreateRoleRequest)`: validate unique name, save Role. `assignPermissionToRole(UUID roleId, UUID permissionId)`: insert role_permissions row. `removePermissionFromRole(UUID, UUID)`: delete row. `assignRoleToUser(UUID userId, UUID roleId)`: insert user_roles. `removeRoleFromUser(UUID, UUID)`: delete row. `listRoles()`: return all active roles with their permissions. | M (3d) | High | M1-E1-T2 | DoD-AppService |
| M1-E2-T4 | Account lockout logic | On each failed login: insert audit_log record with action=LOGIN, status=FAILED. Before allowing login: query audit_logs for COUNT(action=LOGIN AND status=FAILED AND created_at > NOW()-30min) ≥ 5 → throw `AccountLockedException`. On successful login: do not reset count (window naturally expires). | S (2d) | High | M1-E2-T1 | DoD-AppService, DoD-Security |

**M1-E2 Subtotal: 12 developer-days**

---

### M1-E3 — Identity Service REST Layer

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E3-T1 | AuthController (6 endpoints) | `POST /auth/register` → creates user + VIEWER role assignment. `POST /auth/login` → returns TokenPairResponse. `POST /auth/logout` → revokes refresh token. `POST /auth/refresh` → returns new TokenPairResponse. `GET /auth/validate` → returns JwtClaimsResponse (used by API Gateway). `POST /auth/password/reset-request` → stub for future MFA. All wrapped in ApiResponse<T>. | M (4d) | Critical | M1-E2-T1 | DoD-REST, DoD-OpenAPI |
| M1-E3-T2 | UserController (8 endpoints) | `GET /users` (paginated, ADMIN/MANAGER required). `POST /users` (ADMIN required). `GET /users/{id}`. `PUT /users/{id}`. `DELETE /users/{id}` (soft delete, ADMIN required). `GET /users/{id}/roles`. `POST /users/{id}/roles/{roleId}` (assign role, ADMIN). `DELETE /users/{id}/roles/{roleId}` (remove role, ADMIN). | M (3d) | High | M1-E2-T2, M1-E2-T3 | DoD-REST, DoD-OpenAPI |
| M1-E3-T3 | RoleController (6 endpoints) | `GET /roles`. `POST /roles` (ADMIN). `GET /roles/{id}`. `PUT /roles/{id}` (ADMIN). `DELETE /roles/{id}` (ADMIN, disallow if assigned to users). `GET /roles/{id}/permissions`. `POST /roles/{id}/permissions/{permissionId}` (ADMIN). `DELETE /roles/{id}/permissions/{permissionId}` (ADMIN). | M (3d) | High | M1-E2-T3 | DoD-REST, DoD-OpenAPI |
| M1-E3-T4 | Request/Response DTOs and Mappers | `LoginRequest(username, password)` with @NotBlank. `CreateUserRequest(username, email, firstName, lastName, password)` with constraints. `UpdateUserRequest` (partial, all nullable). `TokenPairResponse(accessToken, refreshToken, expiresIn, tokenType)`. `UserResponse(id, username, email, firstName, lastName, active, roles, createdAt)`. MapStruct `UserMapper` for User entity ↔ response DTO. | S (2d) | High | M1-E2-T1 | DoD-Global |

**M1-E3 Subtotal: 12 developer-days**

---

### M1-E4 — Identity Security & Database

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E4-T1 | Spring Security configuration | `SecurityFilterChain`: permitAll for /auth/login, /auth/register, /auth/refresh, /actuator/health, /swagger-ui/**. RequireAuth for all others. Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`. Configure `BCryptPasswordEncoder` bean (strength=12). Disable CSRF (stateless API). | M (3d) | Critical | M1-E1-T5 | DoD-Security |
| M1-E4-T2 | Database migration V2 — complete roles | Write `V2__complete_role_permissions.sql`: seed all 6 roles (SYSTEM_ADMIN, WAREHOUSE_MANAGER, INVENTORY_OPERATOR, SUPPLIER_MANAGER, REPORTER, AUDITOR) and all required permissions per ADR-0005 role hierarchy. Insert all role_permissions mappings. | S (1d) | High | None | DoD-DBMigration |
| M1-E4-T3 | Database migration V3 — audit indexes | Write `V3__audit_indexes.sql`: composite index on audit_logs(action, created_at DESC) for lockout query. Partial index on refresh_tokens(user_id) WHERE revoked=FALSE. | S (1d) | Medium | M1-E4-T2 | DoD-DBMigration |

**M1-E4 Subtotal: 5 developer-days**

---

### M1-E5 — Identity Tests

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E5-T1 | Unit tests — AuthenticationService | Test cases: login with valid credentials returns TokenPair; login with wrong password throws BusinessException; login with locked account throws AccountLockedException; refreshToken with valid token returns new access token; refreshToken with revoked token throws; validateToken with expired token returns false. All using @ExtendWith(MockitoExtension.class). | M (3d) | Critical | M1-E2-T1 | DoD-UnitTest |
| M1-E5-T2 | Unit tests — UserManagementService | Test cases: createUser happy path; duplicate email throws ConflictException; getUserById not found throws ResourceNotFoundException; updateUser partial fields; deactivateUser sets deletedAt; listUsers excludes soft-deleted. | M (3d) | High | M1-E2-T2 | DoD-UnitTest |
| M1-E5-T3 | Integration tests — AuthController | Testcontainers PostgreSQL 16. Test: POST /auth/register → 201; POST /auth/login → 200 with JWT; GET /auth/validate with valid token → 200; POST /auth/refresh → 200; POST /auth/logout → 204; repeated failed logins → 401 locked after 5 attempts. | M (4d) | Critical | M1-E3-T1 | DoD-IntTest |
| M1-E5-T4 | Integration tests — UserController | Testcontainers. Test: GET /users without token → 401; GET /users with VIEWER token → 403; GET /users with ADMIN token → 200 with pagination; POST /users → 201 with location header; PUT /users/{id} → 200; DELETE /users/{id} → 204 (soft delete, user still in DB). | M (3d) | High | M1-E3-T2 | DoD-IntTest |

**M1-E5 Subtotal: 13 developer-days**

---

### M1-E6 — API Gateway

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M1-E6-T1 | Create api-gateway Maven module | New module at `services/api-gateway/`. pom.xml with: spring-cloud-starter-gateway, spring-boot-starter-actuator, spring-boot-starter-data-redis-reactive, spring-cloud-starter-circuitbreaker-resilience4j. Add to services/pom.xml modules list. | S (1d) | Critical | None | DoD-Global |
| M1-E6-T2 | Gateway route configuration | `application.yml` routes: `/api/v1/identity/**` → `http://identity-service:8081`; future routes stubbed. `application-docker.yml` uses Docker network service names. RewritePath filter to strip service prefix where needed. | S (2d) | Critical | M1-E6-T1 | DoD-Global |
| M1-E6-T3 | JWT authentication gateway filter | `JwtAuthenticationGatewayFilter` (GlobalFilter): extract Bearer token from Authorization header; call Identity Service `GET /auth/validate` (with circuit breaker); on 401 or circuit open → return 401 to client; on success → pass `X-User-Id`, `X-User-Roles`, `X-User-Warehouses`, `X-Correlation-Id` as downstream headers. Permit-list: /api/v1/identity/auth/login, /auth/register, /auth/refresh, /actuator/health. | M (3d) | Critical | M1-E6-T2, M1-E5-T3 | DoD-Security |
| M1-E6-T4 | Rate limiting filter | Configure `RequestRateLimiterGatewayFilterFactory` with Redis backend. Replenish rate: 100 req/min per authenticated user (key = X-User-Id header). Burst: 200. For unauthenticated paths: rate limit by IP (50 req/min). Return 429 with Retry-After header when limit exceeded. | M (3d) | High | M1-E6-T3 | DoD-Security |
| M1-E6-T5 | CORS and error response standardization | Configure CORS (allowed origins from env var `CORS_ORIGINS`, all methods, expose X-Correlation-Id). Implement `GatewayExceptionHandler` returning ApiResponse error format for 401, 403, 404, 429, 503 from gateway. | S (2d) | High | M1-E6-T2 | DoD-REST |

**M1-E6 Subtotal: 11 developer-days**

**M1 Grand Total: 63 tasks, ~53 developer-days estimated (adjusted from 40 baseline for full scope)**

---

## M2 — Core Domain Services

**Total M2 Estimate:** 60 developer-days  
**Milestone Date:** 2026-08-25 to 2026-11-17

### M2-E1 — Product Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M2-E1-T1 | Product domain model | JPA entities: `Product` (id, sku, name, description, categoryId, unitPrice, unitCost, weight, dimensions[JSONB], barcode, qrCode, status[ACTIVE/INACTIVE/DISCONTINUED], createdAt, updatedAt, deletedAt, createdBy, updatedBy). `Category` (id, name, parentCategoryId for hierarchy). `ProductBarcode` (id, productId, barcodeType, value). Value objects: `Dimensions`, `Money`. Enum: `ProductStatus`. | M (3d) | Critical | M1 gate | DoD-DomainModel |
| M2-E1-T2 | Product database migration V1 | `V1__initial_schema.sql`: tables products, categories, product_barcodes, product_skus, product_images, product_price_history, product_attributes, product_tags. All UUID primary keys. All standard audit fields. 12+ indexes per database spec. Replace stub service_metadata table. | M (3d) | Critical | M2-E1-T1 | DoD-DBMigration |
| M2-E1-T3 | Product repositories | `ProductRepository extends JpaRepository<Product, String>`. Custom methods: `findBySkuAndDeletedAtIsNull`, `findAllByCategoryIdAndDeletedAtIsNull(String, Pageable)`, `findByBarcodeValue(String)`. `CategoryRepository`: `findByParentCategoryIdIsNull()` for root categories. `ProductBarcodeRepository`. | S (2d) | Critical | M2-E1-T2 | DoD-Repository |
| M2-E1-T4 | ProductService application layer | `createProduct(CreateProductRequest)`: validate unique SKU, generate barcode (Code128) and QR code (UUID-encoded), save, publish ProductCreated event. `updateProduct(UUID, UpdateProductRequest)`: partial update, publish ProductUpdated with previousValues snapshot. `deleteProduct(UUID)`: soft delete, publish ProductDeleted. `getProductById(UUID)`: cache-aside (check Redis → DB). `listProducts(ProductFilter, PageRequest)`: query with filters (category, status, priceRange). `importProducts(List<CreateProductRequest>)`: batch import, max 1000 per call. `exportProducts(ProductFilter)`: returns CSV representation. | L (7d) | Critical | M2-E1-T3 | DoD-AppService |
| M2-E1-T5 | Product REST controllers | `ProductController` (9 endpoints from API catalog): POST /products, GET /products (paginated + filtered), GET /products/{id}, PUT /products/{id}, DELETE /products/{id}, POST /products/import, GET /products/export, GET /products/{id}/barcodes, POST /products/{id}/barcodes/regenerate. `CategoryController` (4 endpoints): CRUD + hierarchy. All return ApiResponse<T>. | M (4d) | Critical | M2-E1-T4 | DoD-REST |
| M2-E1-T6 | Product Kafka event publishing | Concrete event classes: `ProductCreatedEvent`, `ProductUpdatedEvent`, `ProductDeletedEvent` extending DomainEvent. `ProductEventPublisher` using KafkaTemplate to topic `events.product`, partition key = productId. Configure KafkaProducerConfig (acks=all, retries=3, idempotence=true). | M (3d) | High | M2-E1-T4 | DoD-KafkaProducer |
| M2-E1-T7 | Product Redis cache integration | `ProductCacheService`: `getProduct(UUID)` → check `products:{id}` key → cache miss: fetch from DB, store with TTL 5 min. `invalidateProduct(UUID)` called on update/delete. `ProductCacheInvalidationListener` (@KafkaListener on events.product): on ProductUpdated/ProductDeleted → delete cache keys. Catalog cache: `product:catalog:{categoryId}` with TTL 5 min. | M (3d) | High | M2-E1-T5 | DoD-AppService |
| M2-E1-T8 | Product unit tests | Tests for ProductService: create happy path, duplicate SKU → ConflictException, update partial fields, soft delete preserves DB record, list with filters returns correct page. Mock: ProductRepository, ProductEventPublisher, ProductCacheService. Target ≥80% line coverage. | M (3d) | High | M2-E1-T4 | DoD-UnitTest |
| M2-E1-T9 | Product integration tests | Testcontainers (PostgreSQL 16, Kafka, Redis). Test: POST /products → 201 + event in Kafka within 3 sec; GET /products/{id} second call returns cached; import 100 products in batch; export returns valid CSV; unauthorized request returns 401. | M (4d) | High | M2-E1-T5 | DoD-IntTest |
| M2-E1-T10 | Product OpenAPI documentation | Complete @Operation annotations on all 13 Product Service endpoints. Document all DTOs with @Schema. Include 400/401/403/404/409 response examples. Ensure springdoc generates valid OpenAPI 3.1 spec. | S (1d) | Medium | M2-E1-T5 | DoD-OpenAPI |

**M2-E1 Subtotal: 33 developer-days**

---

### M2-E2 — Warehouse Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M2-E2-T1 | Warehouse domain model | JPA entities: `Warehouse` (id, name, code, address, type[MAIN/DISTRIBUTION/TRANSIT], active, capacityTotal, capacityUsed). `Zone` (id, warehouseId, name, type). `Aisle` (id, zoneId, aisleCode). `Shelf` (id, aisleId, shelfCode, level). `Bin` (id, shelfId, binCode, capacityUnits, occupiedUnits). `WarehouseTransfer` (id, fromWarehouseId, toWarehouseId, status, initiatedAt). Composite unique constraint: (warehouseId, zoneCode), (warehouseId, binCode). | M (3d) | Critical | M1 gate | DoD-DomainModel |
| M2-E2-T2 | Warehouse database migration V1 | Tables: warehouses, warehouse_zones, warehouse_aisles, warehouse_shelves, warehouse_bins, warehouse_equipment, warehouse_staff, warehouse_metrics, warehouse_transfers, bin_occupancy. 10 tables, 15+ indexes, composite keys per database spec. | M (3d) | Critical | M2-E2-T1 | DoD-DBMigration |
| M2-E2-T3 | Warehouse repositories | `WarehouseRepository`: findByCode, findAllActive(Pageable). `BinRepository`: findAvailableBins(warehouseId, minCapacity), findByWarehouseIdAndBinCode. Custom query for capacity utilization aggregation. | S (2d) | High | M2-E2-T2 | DoD-Repository |
| M2-E2-T4 | WarehouseService application layer | `createWarehouse()`, `updateWarehouse()`, `deactivateWarehouse()`, `listWarehouses(PageRequest)`. `createZone(warehouseId, ...)`, `createBin(zoneId, ...)`. `getWarehouseById(UUID)`: cache-aside. `calculateUtilization(UUID)`: sum bin occupancy / total capacity. `transferStock(TransferRequest)`: initiates transfer workflow, publishes WarehouseTransferInitiated. | L (6d) | Critical | M2-E2-T3 | DoD-AppService |
| M2-E2-T5 | Warehouse REST controllers | `WarehouseController` (8 endpoints): CRUD + list + utilization endpoint. `ZoneController` (4 endpoints). `BinController` (4 endpoints): CRUD + availability query. `TransferController` (3 endpoints): initiate, status, cancel. | M (4d) | Critical | M2-E2-T4 | DoD-REST |
| M2-E2-T6 | Warehouse Kafka events | Events: `WarehouseCreatedEvent`, `WarehouseCapacityUpdatedEvent`, `ZoneCreatedEvent`, `WarehouseTransferCompletedEvent`. Publish to `events.warehouse` topic. | S (2d) | High | M2-E2-T4 | DoD-KafkaProducer |
| M2-E2-T7 | Warehouse Redis cache | Cache `warehouse:{id}` with TTL 5 min. Cache `warehouse:list` (all active warehouses) with TTL 5 min. Invalidate on WarehouseCreated/Updated events via CacheInvalidationListener. | S (2d) | High | M2-E2-T5 | DoD-AppService |
| M2-E2-T8 | Warehouse tests | Unit: WarehouseService CRUD, utilization calculation, transfer initiation. Integration (Testcontainers PG + Kafka + Redis): CRUD flow, event publishing, cache behavior. ≥80% coverage. | M (5d) | High | M2-E2-T4 | DoD-UnitTest, DoD-IntTest |

**M2-E2 Subtotal: 27 developer-days**

---

### M2-E3 — Supplier Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M2-E3-T1 | Supplier domain model | `Supplier` (id, name, code, contactEmail, contactPhone, address, status, rating, paymentTerms, currency). `SupplierContact` (id, supplierId, name, role, email, phone, primary). `SupplierProduct` (id, supplierId, productId, supplierSku, leadTimeDays, unitCost). `SupplierDelivery` (id, supplierId, purchaseOrderId, expectedDate, actualDate, status). `SupplierMetrics` (id, supplierId, periodStart, onTimeDeliveryPct, qualityScore). | M (3d) | Critical | M1 gate | DoD-DomainModel |
| M2-E3-T2 | Supplier database migration V1 | 8 tables: suppliers, supplier_contacts, supplier_products, supplier_contracts, supplier_deliveries, supplier_metrics, supplier_blacklist, supplier_audit_logs. 12+ indexes. | M (3d) | Critical | M2-E3-T1 | DoD-DBMigration |
| M2-E3-T3 | SupplierService application layer | CRUD for Supplier, SupplierContact, SupplierProduct. `updatePerformanceMetrics(supplierId, DeliveryRecord)`: update rating after each delivery. `getSupplierPerformance(UUID, DateRange)`: aggregate on-time delivery %. `blacklistSupplier(UUID, reason)`. Publish SupplierCreated, SupplierPerformanceUpdated, SupplierDeliveryRegistered events. | L (6d) | Critical | M2-E3-T2 | DoD-AppService |
| M2-E3-T4 | Supplier REST + Kafka + Cache + Tests | `SupplierController` (10 endpoints per API catalog). Events to `events.supplier` topic. Cache supplier list TTL 1 hour. Unit + integration tests ≥80% coverage. | L (6d) | High | M2-E3-T3 | DoD-REST, DoD-KafkaProducer, DoD-UnitTest, DoD-IntTest |

**M2-E3 Subtotal: 18 developer-days**

---

### M2-E4 — Customer Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M2-E4-T1 | Customer domain model | `Customer` (id, name, code, type[RETAIL/WHOLESALE/ENTERPRISE], email, phone, address, creditLimit, creditUsed, status). `CustomerContact` (id, customerId, name, role, email). `CustomerSegment` (id, name, criteria[JSONB]). `CustomerSegmentMembership` (customerId, segmentId). `CustomerInteraction` (id, customerId, type, notes, createdAt). `CreditProfile` (id, customerId, limit, used, paymentTerms). | M (3d) | Critical | M1 gate | DoD-DomainModel |
| M2-E4-T2 | Customer database migration V1 | 9 tables: customers, customer_contacts, customer_addresses, customer_segments, customer_segment_memberships, customer_interactions, customer_credit_profiles, customer_preferences, customer_audit_logs. 12+ indexes. | M (3d) | Critical | M2-E4-T1 | DoD-DBMigration |
| M2-E4-T3 | CustomerService application layer | CRUD for Customer, CustomerContact. `updateCreditUsage(UUID, amount)`: deduct/restore credit limit. `segmentCustomers()`: evaluate segment criteria against customer attributes. `getCustomerById(UUID)` with cache-aside. Publish CustomerCreated, CustomerUpdated events. | M (5d) | Critical | M2-E4-T2 | DoD-AppService |
| M2-E4-T4 | Customer REST + Kafka + Cache + Tests | `CustomerController` (9 endpoints per API catalog). Events to `events.customer` topic. Cache customer:{id} TTL 5 min. Unit + integration tests ≥80% coverage. OpenAPI documented. | L (7d) | High | M2-E4-T3 | DoD-REST, DoD-KafkaProducer, DoD-UnitTest, DoD-IntTest, DoD-OpenAPI |

**M2-E4 Subtotal: 18 developer-days**

### M2-E5 — Infrastructure Updates for M2

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M2-E5-T1 | Add supplier and customer DBs to docker-compose | Add postgres-supplier (port 5436) and postgres-customer (port 5437) containers with health checks and named volumes. Add product-service and warehouse-service containers with correct DB env vars. | S (1d) | Critical | None | DoD-Global |
| M2-E5-T2 | Add Gateway routes for M2 services | Add routes to api-gateway application.yml for /api/v1/products/**, /api/v1/warehouses/**, /api/v1/suppliers/**, /api/v1/customers/**. Configure circuit breakers for each route. | S (1d) | High | M1-E6-T2 | DoD-Global |

**M2-E5 Subtotal: 2 developer-days**

**M2 Grand Total: 98 developer-days (~60 calendar days, parallel work on sequenced services)**

---

## M3 — Transactional Services

**Total M3 Estimate:** 50 developer-days  
**Milestone Date:** 2026-11-18 to 2027-01-26

### M3-E1 — Module Setup and Infrastructure

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M3-E1-T1 | Create purchase-order-service Maven module | New `services/purchase-order-service/` module. Add to `services/pom.xml`. Copy skeleton structure. Configure pom.xml with common module, spring-boot-starter-web, spring-kafka, spring-boot-starter-data-jpa, postgresql, flyway, resilience4j, actuator, springdoc. | S (1d) | Critical | M2 gate | DoD-Global |
| M3-E1-T2 | Create sales-order-service Maven module | Same as above for `services/sales-order-service/`. Port 8008. Database: sales_db. | S (1d) | Critical | M2 gate | DoD-Global |
| M3-E1-T3 | Add purchase and sales DBs to docker-compose | postgres-purchase (port 5438), postgres-sales (port 5439). Add purchase-order-service and sales-order-service containers. | S (1d) | Critical | M3-E1-T1 | DoD-Global |
| M3-E1-T4 | Create Kafka topics for order saga | `events.order` (partitions: 10, replication: 1 for dev), `events.order.dlq` (dead letter). Add Kafka Admin config bean to create topics on startup in dev. Document topic schemas. | S (2d) | Critical | None | DoD-KafkaProducer |

---

### M3-E2 — Purchase Order Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M3-E2-T1 | Purchase Order domain model | `PurchaseOrder` (id, supplierId, warehouseId, status[DRAFT/SUBMITTED/CONFIRMED/RECEIVED/CANCELLED], totalAmount, expectedDeliveryDate). `POLineItem` (id, purchaseOrderId, productId, quantityOrdered, quantityReceived, unitCost). `POReceipt` (id, purchaseOrderId, receivedAt, receivedBy, notes). `POInvoice` (id, purchaseOrderId, invoiceNumber, amount, dueDate, paid). | M (3d) | Critical | M3-E1-T1 | DoD-DomainModel |
| M3-E2-T2 | Purchase Order database migration V1 | 8 tables: purchase_orders, po_line_items, po_receipts, po_invoices, po_returns, po_approvals, po_shipment_tracking, po_audit_logs. UUID PKs. No FK to supplier_db. | M (3d) | Critical | M3-E2-T1 | DoD-DBMigration |
| M3-E2-T3 | PurchaseOrderService application layer | `createPurchaseOrder(CreatePORequest)`: validate supplierId via SupplierService REST (circuit breaker). `submitPO(UUID)`: change status DRAFT→SUBMITTED, publish PurchaseOrderCreated. `receivePO(UUID, ReceiptDetails)`: create POReceipt, change status→RECEIVED, publish PurchaseOrderReceived. `cancelPO(UUID, reason)`: publish PurchaseOrderCancelled. `listPOs(POFilter, PageRequest)`: paginated by supplier, status, date range. | L (7d) | Critical | M3-E2-T2 | DoD-AppService |
| M3-E2-T4 | Purchase Order REST controllers | `PurchaseOrderController` (10 endpoints): POST /purchase-orders, GET /purchase-orders (filtered), GET /purchase-orders/{id}, PUT /purchase-orders/{id}, DELETE /purchase-orders/{id} (cancel), POST /purchase-orders/{id}/submit, POST /purchase-orders/{id}/receive, GET /purchase-orders/{id}/line-items. | M (4d) | Critical | M3-E2-T3 | DoD-REST |
| M3-E2-T5 | Purchase Order Kafka events | Events: PurchaseOrderCreated, PurchaseOrderReceived, PurchaseOrderCancelled. Publish to `events.order` topic. Partition key = purchaseOrderId. | S (2d) | High | M3-E2-T3 | DoD-KafkaProducer |
| M3-E2-T6 | Purchase Order tests | Unit tests for PurchaseOrderService (mock SupplierClient, KafkaTemplate). Integration tests: Testcontainers PG + Kafka; CRUD flow; PO submission publishes event; PO receipt changes status. ≥80% coverage. | M (5d) | High | M3-E2-T4 | DoD-UnitTest, DoD-IntTest |

**M3-E2 Subtotal: 24 developer-days**

---

### M3-E3 — Sales Order Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M3-E3-T1 | Sales Order domain model | `SalesOrder` (id, customerId, warehouseId, status[PENDING/CONFIRMED/PROCESSING/SHIPPED/DELIVERED/CANCELLED], totalAmount, shippingAddress). `SOLineItem` (id, salesOrderId, productId, quantity, unitPrice). `SOFulfillment` (id, salesOrderId, warehouseId, status, packedAt). `SOShipment` (id, salesOrderId, trackingNumber, carrier, shippedAt, estimatedDelivery). `SOReturn` (id, salesOrderId, reason, returnedAt). `SagaState` (id, orderId, currentStep, status, retryCount, lastUpdated) — idempotency record. | M (3d) | Critical | M3-E1-T2 | DoD-DomainModel |
| M3-E3-T2 | Sales Order database migration V1 | 8 tables: sales_orders, so_line_items, so_fulfillments, so_shipments, so_returns, so_payment_records, saga_states, so_audit_logs. | M (3d) | Critical | M3-E3-T1 | DoD-DBMigration |
| M3-E3-T3 | SalesOrderService application layer | `createSalesOrder(CreateSORequest)`: validate customerId via CustomerService REST (circuit breaker); validate productId(s) via ProductService REST; save with PENDING status; publish SalesOrderCreated. `confirmOrder(UUID)`: change status PENDING→CONFIRMED. `fulfillOrder(UUID)`: change status→PROCESSING. `shipOrder(UUID, ShipmentDetails)`: publish OrderShipped. `cancelOrder(UUID, reason)`: publish OrderCancelled; publish compensating events. | L (7d) | Critical | M3-E3-T2 | DoD-AppService |
| M3-E3-T4 | Saga choreography handlers (Sales Order side) | `StockReservedEventHandler` (@KafkaListener events.inventory): check saga_states for idempotency; on StockReserved → confirm order. `StockReservationFailedEventHandler`: cancel order, publish SalesOrderCancelled. `LocationAllocatedHandler`: move to PROCESSING. `AllocationFailedHandler`: compensate — publish SalesOrderCancelled, publish ReleaseInventoryReservation. | L (8d) | Critical | M3-E3-T3 | DoD-KafkaConsumer, DoD-AppService |
| M3-E3-T5 | Inventory stub saga participant | Temporary stub in inventory-service: @KafkaListener on events.order → SalesOrderCreated: if productId ends in "0" (simulate stockout), publish StockReservationFailed; else publish StockReserved. This stub is replaced by full Inventory Service in M4. | M (3d) | Critical | M3-E1-T4 | DoD-KafkaConsumer |
| M3-E3-T6 | Sales Order REST controllers | `SalesOrderController` (10 endpoints): POST /sales-orders, GET /sales-orders, GET /sales-orders/{id}, PUT /sales-orders/{id}, DELETE /sales-orders/{id} (cancel), POST /sales-orders/{id}/confirm, POST /sales-orders/{id}/ship, GET /sales-orders/{id}/fulfillment, GET /sales-orders/{id}/shipment, POST /sales-orders/{id}/return. | M (4d) | High | M3-E3-T3 | DoD-REST |
| M3-E3-T7 | Dead letter topic handler | `DLQConsumer` (@KafkaListener events.order.dlq): log failure with saga details, call alerting endpoint, store in dedicated `failed_sagas` table (add V2 migration). | S (2d) | High | M3-E3-T4 | DoD-KafkaConsumer |
| M3-E3-T8 | Sales Order tests | Unit: SalesOrderService saga flow (mock Kafka consumers and producers). Integration: Testcontainers PG + Kafka; full saga happy path; failure path with StockReservationFailed triggers cancellation; idempotency: replay StockReserved event has no effect. | L (7d) | High | M3-E3-T4 | DoD-UnitTest, DoD-IntTest |

**M3-E3 Subtotal: 37 developer-days**

**M3 Grand Total: 62 tasks, ~65 developer-days**

---

## M4 — Operational Intelligence

**Total M4 Estimate:** 40 developer-days  
**Milestone Date:** 2027-01-27 to 2027-03-23

### M4-E1 — Inventory Service Full Implementation

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M4-E1-T1 | Inventory domain model | `InventoryLevel` (id, productId, warehouseId, quantityOnHand, quantityReserved, quantityAvailable[computed], quantityDamaged, lowStockThreshold, lastUpdated). `StockMovement` (id, type[IN/OUT/TRANSFER/ADJUSTMENT], productId, warehouseId, quantity, referenceId, referenceType, movedAt, movedBy) — immutable. `InventoryHold` (id, salesOrderId, productId, warehouseId, quantityHeld, heldAt, releasedAt). `InventorySnapshot` (id, productId, warehouseId, date, quantityOnHand). `DamagedInventory` (id, productId, warehouseId, quantity, reason, reportedAt). | M (4d) | Critical | M3 gate | DoD-DomainModel |
| M4-E1-T2 | Inventory database migration V1 | Replace stub schema. Tables: inventory_levels, stock_movements (immutable), stock_in, stock_out, stock_transfers, stock_adjustments, inventory_holds, inventory_snapshots, damaged_inventory, inventory_alerts, inventory_counts. 20+ indexes. Unique constraint on (productId, warehouseId) in inventory_levels. Fix application.yml: remove Keycloak issuer-uri, add JWT validation config matching identity-service pattern. | M (4d) | Critical | M4-E1-T1 | DoD-DBMigration |
| M4-E1-T3 | InventoryService application layer (stock operations) | `stockIn(StockInRequest)`: validate productId (REST→ProductService, circuit breaker), validate warehouseId (REST→WarehouseService, circuit breaker); acquire Redis lock `lock:adjust-inventory:{productId}:{warehouseId}`; update inventory_levels; insert immutable stock_movement; release lock; publish StockIn event. `stockOut(StockOutRequest)`: same pattern, validate quantityAvailable ≥ requested (throw InsufficientStockException if not). `adjustInventory(AdjustmentRequest)`: reason-coded adjustment, publish StockAdjusted. `transferStock(TransferRequest)`: decrease source, increase destination, publish StockMoved. | L (8d) | Critical | M4-E1-T2 | DoD-AppService |
| M4-E1-T4 | InventoryService — Kafka event consumers | Replace M3 stub. Consumers on `events.order`: SalesOrderCreated → reserveStock (create InventoryHold), publish StockReserved or StockReservationFailed; SalesOrderFulfilled → deductStock (move hold to movement), publish StockOut; SalesOrderCancelled → releaseHold, restore quantityAvailable. Consumer on `events.order`: PurchaseOrderReceived → stockIn. Consumer on `events.product`: ProductCreated → initialize InventoryLevel with quantity=0. All handlers idempotent (check StockMovement.referenceId). | L (8d) | Critical | M4-E1-T3 | DoD-KafkaConsumer |
| M4-E1-T5 | Inventory REST controllers | `InventoryController` (12 endpoints from API catalog): GET /inventory (list levels, filtered), GET /inventory/{productId}/{warehouseId}, POST /inventory/stock-in, POST /inventory/stock-out, POST /inventory/adjust, POST /inventory/transfer, GET /inventory/movements (paginated history), GET /inventory/alerts, GET /inventory/snapshots, POST /inventory/count, GET /inventory/damaged, GET /inventory/{productId}/history. | M (5d) | Critical | M4-E1-T3 | DoD-REST |
| M4-E1-T6 | Inventory low-stock alert trigger | In stockOut and fulfillment handlers: after updating quantityAvailable, check `quantityAvailable ≤ lowStockThreshold`. If true, check Redis key `alert:low-stock:{productId}:{warehouseId}` not set (dedup TTL 1 hour). If not deduplicated, publish LowStockThresholdReached event to `events.inventory`. Set Redis key with 1-hour TTL. | S (2d) | High | M4-E1-T4 | DoD-AppService, DoD-KafkaProducer |
| M4-E1-T7 | Inventory tests | Unit: InventoryService stockIn/stockOut/adjust (mock ProductClient, WarehouseClient, Redis lock, KafkaTemplate). Test InsufficientStockException on stockOut with zero quantity. Test idempotency of saga consumers. Integration: Testcontainers PG + Kafka + Redis; stockIn → verify event; SalesOrderCreated → StockReserved saga; LowStockThresholdReached fires when threshold crossed. ≥80% coverage. | L (9d) | Critical | M4-E1-T5 | DoD-UnitTest, DoD-IntTest |

**M4-E1 Subtotal: 40 developer-days**

---

### M4-E2 — Notification Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M4-E2-T1 | Notification domain model and schema | `NotificationTemplate` (id, name, type[EMAIL/SMS], subject, bodyTemplate[Mustache]). `NotificationQueue` (id, type, recipientEmail, subject, body, status[PENDING/SENT/FAILED], retryCount). `NotificationDelivery` (id, notificationId, sentAt, provider, responseCode). `NotificationSubscription` (userId, eventType, channel, enabled). `NotificationBounceList` (email, bouncedAt, reason). V1 migration: 9 tables per database spec. | M (3d) | High | M3 gate | DoD-DomainModel, DoD-DBMigration |
| M4-E2-T2 | NotificationService application layer | `sendEmail(recipientEmail, templateName, Map<String,Object> vars)`: load template, render Mustache, enqueue in notification_queue. `processQueue()` (@Scheduled every 30 sec): send PENDING notifications via JavaMailSender (SMTP to Mailpit in dev), mark SENT or increment retryCount. `subscribe(userId, eventType, channel)`: save subscription. | M (4d) | High | M4-E2-T1 | DoD-AppService |
| M4-E2-T3 | Notification Kafka consumers | @KafkaListener on `events.inventory`: LowStockThresholdReached → find subscribers for LOW_STOCK alert → enqueue email notification from template. @KafkaListener on `events.order`: OrderShipped → find customer email → enqueue shipping notification. @KafkaListener on `events.order`: SalesOrderCancelled → enqueue cancellation notification. All handlers idempotent (check notificationQueue for duplicate eventId). | M (4d) | High | M4-E2-T2 | DoD-KafkaConsumer |
| M4-E2-T4 | Notification REST controllers | `NotificationController` (6 endpoints): GET /notifications/templates, POST /notifications/templates, GET /notifications/queue, POST /notifications/send (manual trigger), GET /notifications/subscriptions, POST /notifications/subscriptions. | S (2d) | Medium | M4-E2-T2 | DoD-REST |
| M4-E2-T5 | Notification tests | Unit: NotificationService sendEmail renders template and enqueues; processQueue sends via SMTP mock. Integration: Testcontainers PG + Kafka; LowStockThresholdReached → verify email appears in Mailpit API within 10 sec. | M (3d) | High | M4-E2-T3 | DoD-UnitTest, DoD-IntTest |

**M4-E2 Subtotal: 16 developer-days**

**M4 Grand Total: 56 developer-days**

---

## M5 — Observability & Audit

**Total M5 Estimate:** 30 developer-days  
**Milestone Date:** 2027-03-24 to 2027-05-04

### M5-E1 — Audit Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M5-E1-T1 | Audit domain model and schema | `AuditEvent` (eventId, eventType, aggregateId, aggregateType, serviceName, userId, correlationId, payload[JSONB], receivedAt, processedAt) — NO update or delete constraints enforced at DB trigger level. `AuditComplianceCheck` (id, checkType, checkDate, passed, findings). `AuditExportLog` (id, exportType, from, to, exportedAt, exportedBy). V1 migration: 9 tables per database spec. PostgreSQL trigger that raises exception on UPDATE/DELETE on audit_events. | M (4d) | Critical | M4 gate | DoD-DomainModel, DoD-DBMigration |
| M5-E1-T2 | AuditService Kafka consumer (all topics) | Single `AuditEventConsumer` with @KafkaListener on `events.product`, `events.inventory`, `events.order`, `events.warehouse`, `events.supplier`, `events.customer`, `events.identity`. For each message: deserialize, create AuditEvent, save to audit_events table. Use Kafka manual offset commit to ensure at-least-once delivery. Idempotency: unique constraint on eventId (ignore duplicate key). | M (4d) | Critical | M5-E1-T1 | DoD-KafkaConsumer |
| M5-E1-T3 | Audit Service REST API | `GET /audit/events` (AUDITOR role, paginated, filterable by eventType, aggregateId, serviceName, from, to). `GET /audit/events/{eventId}`. `GET /audit/compliance/report` (AUDITOR role, date range). Response capped at 1000 results per query for compliance; pagination required for larger datasets. | M (3d) | High | M5-E1-T2 | DoD-REST |
| M5-E1-T4 | Audit Service tests | Integration: Testcontainers PG + Kafka; publish ProductCreated event, verify appears in audit_events within 5 seconds; verify UPDATE on audit_events raises exception. Unit: AuditService consumer idempotency test. | M (3d) | High | M5-E1-T2 | DoD-UnitTest, DoD-IntTest |

**M5-E1 Subtotal: 14 developer-days**

---

### M5-E2 — Observability Wiring

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M5-E2-T1 | Add OpenTelemetry to all services | Add `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp` to each service pom. Configure OTLP endpoint in `application-docker.yml` to point to Tempo container (port 4317). Verify trace headers propagate across HTTP calls (X-B3-TraceId in gateway → service). | M (4d) | Critical | M4 gate | DoD-Observability |
| M5-E2-T2 | JSON structured logging via Logback | Add `logstash-logback-encoder` to all services. Replace `logback-spring.xml` in each service (already stubbed as placeholder) with JSON encoder config writing to stdout. Include fields: timestamp, level, service, correlationId (from MDC), traceId (from MDC via OTel). | M (3d) | High | M5-E2-T1 | DoD-Observability |
| M5-E2-T3 | Custom Micrometer business metrics | Add to each relevant service: `Counter smartstock_stock_movements_total{type}` (Inventory Service). `Counter smartstock_orders_total{status, type}` (Sales Order, Purchase Order). `Counter smartstock_low_stock_alerts_total` (Inventory). `Timer smartstock_kafka_processing_duration_seconds{topic}` (all consumers). Register with Micrometer registry; expose via /actuator/prometheus. | M (3d) | High | M5-E2-T1 | DoD-Observability |
| M5-E2-T4 | Grafana dashboards | Import/create dashboards in Grafana (provisioned via `infrastructure/grafana/provisioning/`): Operations Dashboard (13 panels: service UP/DOWN, RPS per service, error rate, p95/p99 latency, Kafka consumer lag). Business Dashboard (stock movements per hour, order volume, alert count). Configure auto-provisioning so dashboards load on docker-compose up. | M (4d) | High | M5-E2-T2 | DoD-Observability |
| M5-E2-T5 | Prometheus alert rules | Write `infrastructure/alerting-rules.yml`: alert on service DOWN for 30 sec; alert on error_rate > 5% for 5 min; alert on Kafka consumer lag > 30 sec; alert on p99 latency > 1000ms for 10 min. Mount into Prometheus container via docker-compose. | S (2d) | High | M5-E2-T3 | DoD-Observability |

**M5-E2 Subtotal: 16 developer-days**

**M5 Grand Total: 30 developer-days**

---

## M6 — Reporting, Data Export & Analytics

**Total M6 Estimate:** 40 developer-days  
**Milestone Date:** 2027-05-05 to 2027-06-29

### M6-E1 — Reporting Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M6-E1-T1 | Reporting domain model and schema | `DailyMetric` (id, metricDate, metricCategory, metricName, productId, warehouseId, supplierId, metricValue). `MonthlyMetric` (same structure, rolled up). `YearlyMetric`. `ReportExecution` (id, reportType, requestedBy, startedAt, completedAt, outputPath). `KPISnapshot` (id, snapshotDate, kpiName, value, unit). `ReportDefinition` (id, name, type, config[JSONB]). V1 migration: 8 tables. | M (3d) | Critical | M5 gate | DoD-DomainModel, DoD-DBMigration |
| M6-E1-T2 | Reporting Kafka consumers | @KafkaListener on all event topics. StockIn → update DailyMetric for STOCK_IN_QUANTITY, STOCK_IN_VALUE. StockOut → update STOCK_OUT_QUANTITY. OrderCreated → update ORDER_COUNT. LowStockThresholdReached → update ALERT_COUNT. All with idempotency check on eventId. Nightly @Scheduled job rolls DailyMetrics into MonthlyMetrics. | L (7d) | Critical | M6-E1-T1 | DoD-KafkaConsumer |
| M6-E1-T3 | Reporting REST API | `GET /reports/inventory/summary` (totals by warehouse). `GET /reports/inventory/aging` (days in stock). `GET /reports/orders/summary` (by period). `GET /reports/suppliers/performance` (on-time delivery by supplier). `GET /reports/warehouse/utilization`. `GET /reports/kpis/dashboard` (key business KPIs). Paginated, filterable by date range and dimension. | M (5d) | Critical | M6-E1-T2 | DoD-REST |
| M6-E1-T4 | Reporting tests | Unit: aggregation logic. Integration: Testcontainers PG + Kafka; publish events, verify metric aggregation after 5 seconds; verify Kafka replay rebuilds data correctly after DB wipe. | M (4d) | High | M6-E1-T3 | DoD-UnitTest, DoD-IntTest |

**M6-E1 Subtotal: 19 developer-days**

---

### M6-E2 — Data Export Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M6-E2-T1 | Data Export domain model and schema | `ExportDefinition` (id, name, dataSource, format[CSV/PARQUET/JSON], schedule, filters[JSONB]). `ExportJob` (id, definitionId, requestedBy, status[PENDING/RUNNING/COMPLETED/FAILED], startedAt, completedAt, filePath, recordCount). `ExportDelivery` (id, jobId, destination[MINIO], deliveredAt). `ExportDataLineage` (id, jobId, sourceEventType, recordCount). V1: 7 tables. | M (3d) | Critical | M5 gate | DoD-DomainModel, DoD-DBMigration |
| M6-E2-T2 | MinIO integration | `MinIOStorageService`: configure MinioClient from env vars MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY. `uploadFile(bucketName, objectKey, InputStream, contentType)`. `generatePresignedUrl(bucketName, objectKey, Duration)`. Ensure bucket `smartstock-exports` exists on startup. | S (2d) | Critical | M6-E2-T1 | DoD-AppService |
| M6-E2-T3 | Export job execution | `ExportJobExecutor` (async @Async Spring method): read source data from Audit Service REST API (events by type, date range); transform to CSV (Apache Commons CSV) or Parquet (Apache Parquet + Arrow); stream to MinIO; update ExportJob status. Publish DataExportCompleted event on success. | L (7d) | Critical | M6-E2-T2 | DoD-AppService, DoD-KafkaProducer |
| M6-E2-T4 | Data Export REST API | `POST /exports` (trigger export job), `GET /exports` (list jobs), `GET /exports/{id}` (job status), `GET /exports/{id}/download` (pre-signed URL). REPORTER or ADMIN role required. | S (2d) | High | M6-E2-T3 | DoD-REST |
| M6-E2-T5 | Data Export tests | Integration: Testcontainers PG + Kafka + MinIO (testcontainers-minio). POST /exports triggers job; job completes; verify Parquet file in MinIO. Unit: ExportJobExecutor CSV generation. | M (3d) | High | M6-E2-T4 | DoD-UnitTest, DoD-IntTest |

**M6-E2 Subtotal: 17 developer-days**

---

### M6-E3 — Analytics Service

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M6-E3-T1 | Analytics Service core | Domain model: `InventoryAnalyticSnapshot` (id, date, productId, warehouseId, quantityOnHand, turnoverRate, daysOfSupply). `AnalyticsDimension` (id, name, type, values[JSONB]). V1 migration. Daily @Scheduled job: reads reporting_db aggregates via REST (or event-driven), computes derived metrics (turnover = stock_out / avg_inventory), stores snapshot. REST API: GET /analytics/inventory/trends, GET /analytics/inventory/forecast (stub for AI phase), GET /analytics/warehouse/efficiency. | L (9d) | High | M6-E1 gate | DoD-DomainModel, DoD-DBMigration, DoD-AppService, DoD-REST |

**M6-E3 Subtotal: 9 developer-days**

**M6 Grand Total: 45 developer-days**

---

## M7 — Desktop UI — JavaFX

**Total M7 Estimate:** 60 developer-days  
**Milestone Date:** 2027-06-30 to 2027-09-21

### M7-E1 — JavaFX Foundation

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M7-E1-T1 | JavaFX Maven module setup | Configure `desktop-client/pom.xml`: javafx-controls, javafx-fxml, javafx-maven-plugin. Add ControlsFX (advanced controls), Ikonli (icons), Jackson (JSON), OkHttp (API client), SQLite JDBC, HikariCP for SQLite pool. Configure jlink and jpackage goals. | S (2d) | Critical | M6 gate | DoD-Global |
| M7-E1-T2 | SQLite local cache schema | Flyway (or manual init): `product_cache(id, sku, name, category, unitPrice, cachedAt)`, `inventory_cache(id, productId, warehouseId, quantityAvailable, cachedAt)`, `warehouse_cache(id, name, code, cachedAt)`, `pending_operations(id, operationType, payload JSON, status[PENDING/SYNCED/FAILED], createdAt, syncedAt)`. | S (2d) | Critical | M7-E1-T1 | DoD-DBMigration |
| M7-E1-T3 | APIClient service layer | `APIClient`: OkHttp-based. Methods for each backend endpoint group. Handles JWT Bearer header injection from token store. Throws `ApiException(int status, String message)` on non-2xx. Implements retry logic (3 attempts with exponential backoff) before declaring network unavailable. | M (4d) | Critical | M7-E1-T1 | DoD-AppService |
| M7-E1-T4 | OfflineCache service | `OfflineCacheService`: all read operations check SQLite first (if OFFLINE mode) or freshness threshold. Write operations in OFFLINE mode: insert to pending_operations JSON payload. In ONLINE mode: write directly to backend via APIClient. | M (3d) | Critical | M7-E1-T2 | DoD-AppService |
| M7-E1-T5 | SyncService | Background thread (ScheduledExecutorService, every 5 min): check connectivity via APIClient.ping(). If ONLINE and pending_operations count > 0: fetch all PENDING ops, send to backend in FIFO order, mark SYNCED (or FAILED after 3 attempts). On reconnection detected: trigger immediate sync. | M (4d) | Critical | M7-E1-T4 | DoD-AppService |
| M7-E1-T6 | Token storage (OS Keychain) | `TokenStore`: access token in memory (JavaFX static field, lost on close). Refresh token via `java.security.KeyStore` on Windows (WINDOWS-MY provider) or macOS Keychain. Automatic token refresh via APIClient interceptor (check exp before each request; refresh if expiring within 5 minutes). | M (3d) | Critical | M7-E1-T3 | DoD-Security |

**M7-E1 Subtotal: 18 developer-days**

---

### M7-E2 — JavaFX MVVM Screens

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M7-E2-T1 | Login screen | FXML: username field, password field, Login button, connectivity indicator. `LoginViewModel`: `login(username, password)` → call APIClient.login() → store tokens → navigate to Dashboard. Error handling: show label for invalid credentials, locked account. | M (3d) | Critical | M7-E1-T6 | DoD-REST |
| M7-E2-T2 | Main layout and navigation | `MainLayout.fxml`: sidebar navigation (Dashboard, Stock In, Stock Out, Products, Warehouses, Reports, Settings). `ConnectivityIndicator` (top-right): Green=Online/Synced, Yellow=Syncing, Red=Offline. Pending ops badge count. Navigation changes center pane via FXMLLoader. | M (3d) | Critical | M7-E2-T1 | DoD-Global |
| M7-E2-T3 | Dashboard screen | `DashboardViewModel`: load inventory summary from backend (cache if OFFLINE). Display: total products, total quantity on hand, low stock alerts count, pending operations count, last sync time. Refresh every 2 minutes (background thread + Platform.runLater for UI update). | M (3d) | High | M7-E2-T2 | DoD-AppService |
| M7-E2-T4 | Stock In workflow | FXML: product search (auto-complete from product_cache), warehouse selector, quantity field, reference (PO number), notes field, Submit button. `StockInViewModel`: validate inputs, call APIClient.stockIn() (ONLINE) or add to pending_operations (OFFLINE), show success/pending feedback. | M (4d) | Critical | M7-E1-T4 | DoD-AppService |
| M7-E2-T5 | Stock Out workflow | Similar to Stock In. Additional validation: quantity ≤ quantityAvailable from inventory_cache. In OFFLINE mode: optimistic decrement of inventory_cache. Show warning when offline that actual availability cannot be guaranteed. | M (4d) | Critical | M7-E1-T4 | DoD-AppService |
| M7-E2-T6 | Product catalog browse | Searchable TableView populated from product_cache. Columns: SKU, Name, Category, Price, Status. Search filters: name (live filter), category (ComboBox). Click row → detail pane with full product info. Refresh button → sync from backend. | M (3d) | High | M7-E1-T4 | DoD-AppService |
| M7-E2-T7 | Warehouse browser | TreeView of Warehouse → Zone → Shelf → Bin hierarchy. Click bin → show occupancy. Warehouse capacity utilization bar per warehouse. Data from warehouse_cache (TTL 5 min). | M (3d) | High | M7-E1-T4 | DoD-AppService |
| M7-E2-T8 | Settings screen | FXML: API base URL field, sync interval selector, theme toggle (Light/Dark — CSS swap). Save to Java Preferences API (platform-native). Clear local cache button (wipes SQLite tables). Log out button (revokes refresh token via API, clears token store, navigates to Login). | S (2d) | Medium | M7-E2-T2 | DoD-Global |

**M7-E2 Subtotal: 25 developer-days**

---

### M7-E3 — JavaFX Tests and Packaging

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M7-E3-T1 | ViewModel unit tests | TestFX or headless JavaFX: `LoginViewModelTest` (mock APIClient, verify token storage on success, error message on failure), `StockInViewModelTest` (mock APIClient, verify pending_operations write in OFFLINE mode), `SyncServiceTest` (mock APIClient, verify pending ops flushed on reconnect). ≥70% coverage on ViewModel layer. | M (5d) | High | M7-E2-T4 | DoD-UnitTest |
| M7-E3-T2 | Offline integration test | Integration test: launch full app in headless mode; disconnect network (mock APIClient to throw IOException); perform Stock In; verify SQLite pending_operations has 1 record; reconnect (restore mock); trigger sync; verify pending_operations cleared. | M (5d) | High | M7-E3-T1 | DoD-IntTest |
| M7-E3-T3 | JavaFX application packaging | Configure `jpackage` Maven plugin: create installer for Windows (MSI/EXE) and macOS (DMG). Bundle JRE via `jlink`. Icon and application name. Windows installer adds Start Menu entry. Verify installer installs and runs on clean machine. | M (5d) | High | M7-E2-T8 | DoD-Performance |
| M7-E3-T4 | CSS dark/light theme | Create `dark-theme.css` and `light-theme.css`. Apply via `scene.getStylesheets()`. All controls use CSS variables (not hardcoded colors). Test on Windows and macOS. | S (2d) | Low | M7-E2-T2 | DoD-Global |

**M7-E3 Subtotal: 17 developer-days**

**M7 Grand Total: 60 developer-days**

---

## M8 — Production Hardening & Kubernetes

**Total M8 Estimate:** 55 developer-days  
**Milestone Date:** 2027-09-22 to 2027-12-14**

### M8-E1 — Kubernetes Manifests

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M8-E1-T1 | Kubernetes namespace and RBAC | Create `k8s/base/namespace.yaml` (smartstock namespace), `ServiceAccount` per service, `ClusterRole`/`RoleBinding` for secret access. | S (1d) | Critical | M7 gate | DoD-Global |
| M8-E1-T2 | K8s Deployments for all 13 services | Deployment manifests for each service: 2 replicas default, resource requests (256Mi/0.25CPU) and limits (512Mi/1CPU), liveness probe (/actuator/health/liveness, initialDelay=30s), readiness probe (/actuator/health/readiness, initialDelay=10s), env vars from ConfigMap and Secret refs. | L (8d) | Critical | M8-E1-T1 | DoD-Global |
| M8-E1-T3 | K8s Services and Ingress | ClusterIP Service for each microservice. Ingress for API Gateway (nginx-ingress-controller). TLS termination at Ingress. Route /api/v1/** → api-gateway service. | M (3d) | Critical | M8-E1-T2 | DoD-Global |
| M8-E1-T4 | K8s Secrets for all credentials | Kubernetes Secret manifests (values from env; never in Git): db-credentials, kafka-credentials, redis-credentials, jwt-key-pair, minio-credentials. Document secret creation procedure in README. | M (3d) | Critical | M8-E1-T1 | DoD-Security |
| M8-E1-T5 | Network Policies | `NetworkPolicy` restricting each service to only accept ingress from api-gateway (or from specific consumer services). Deny all ingress by default; allow-list specific source labels. | M (3d) | High | M8-E1-T2 | DoD-Security |
| M8-E1-T6 | Horizontal Pod Autoscaler | HPA for inventory-service (min 2, max 10, CPU 70%), sales-order-service (min 2, max 8, CPU 70%), api-gateway (min 2, max 12, CPU 60%). Verify HPA metrics-server is running. | S (2d) | High | M8-E1-T2 | DoD-Performance |

**M8-E1 Subtotal: 20 developer-days**

---

### M8-E2 — Helm Charts

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M8-E2-T1 | Helm chart structure | Create `helm/smartstock/Chart.yaml`, `values.yaml` (defaults), `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`. Sub-charts (one per service) with `templates/deployment.yaml`, `templates/service.yaml`, `templates/hpa.yaml`, `templates/configmap.yaml`. | L (7d) | Critical | M8-E1 gate | DoD-Global |
| M8-E2-T2 | Helm chart install and upgrade test | `helm install smartstock-dev ./helm/smartstock -f values-dev.yaml --dry-run` → no errors. `helm install` on local K8s (minikube). `helm upgrade` with new image tag → rolling update, zero downtime. `helm rollback` reverts to previous version. | M (3d) | Critical | M8-E2-T1 | DoD-MilestoneGate |

**M8-E2 Subtotal: 10 developer-days**

---

### M8-E3 — Performance and Security Hardening

| Task ID | Title | Description | Effort | Priority | Dependencies | DoD |
|---|---|---|---|---|---|---|
| M8-E3-T1 | Performance test (Gatling) | Write `InventoryLoadTest.scala` (Gatling simulation): 50 concurrent users, 5-minute ramp, GET /api/v1/inventory/{productId} with random product IDs. Assert p99 ≤200ms, error rate <1%, throughput ≥100 RPS. Run against K8s cluster (staging). Tune Hikari pool sizes and JVM heap if p99 fails. | L (7d) | Critical | M8-E1 gate | DoD-Performance |
| M8-E3-T2 | Trivy vulnerability scan | Configure Trivy in GitHub Actions `security.yml`: scan each Docker image as part of CI. Fix any critical CVEs found in base image or dependencies. Use `eclipse-temurin:25-jdk-alpine` latest patch. Update all `spring-boot-starter-*` to patch-level with no critical CVEs. | M (4d) | Critical | None | DoD-Security |
| M8-E3-T3 | OWASP dependency-check | Configure OWASP Dependency-Check Maven plugin. Run `mvn dependency-check:check`. Fix or suppress (with justification) any critical findings. Integrate in CI: fail build on CVSS ≥7. | M (3d) | High | None | DoD-Security |
| M8-E3-T4 | Token blacklist (Redis) | Add `token_blacklist:{jti}` Redis key set on logout/revocation (TTL = remaining token lifetime). Modify `JwtAuthenticationGatewayFilter` to check blacklist on each request. Performance impact: one Redis GET per request — benchmark to verify p99 remains ≤200ms. | M (4d) | High | M1-E6-T3 | DoD-Security |
| M8-E3-T5 | Production configuration review | Review all `application-docker.yml` and K8s ConfigMap values: disable spring.jpa.show-sql in prod; set log level to INFO; set Hikari max-pool-size=50 for production; verify no sensitive values in ConfigMaps (only Secrets). | S (2d) | High | M8-E1 gate | DoD-Security |
| M8-E3-T6 | CI/CD pipeline finalization | Verify `.github/workflows/build-test.yml` covers all modules. Add Helm chart lint job (`helm lint`). Add Trivy scan job. Add OWASP scan job. Add performance test job (optional, with manual trigger). Document pipeline stages in `docs/deployment/`. | M (5d) | High | M8-E2 gate | DoD-MilestoneGate |

**M8-E3 Subtotal: 25 developer-days**

**M8 Grand Total: 55 developer-days**

---

## Backlog Totals

| Milestone | Epics | Tasks | Developer-Days |
|---|---|---|---|
| M0 | Complete | Complete | 0 |
| M1 — Identity & Security | 6 | 31 | ~53 |
| M2 — Core Domain Services | 5 | 38 | ~98 (parallel execution) |
| M3 — Transactional Services | 3 | 21 | ~65 |
| M4 — Operational Intelligence | 2 | 12 | ~56 |
| M5 — Observability & Audit | 2 | 9 | ~30 |
| M6 — Reporting / Export / Analytics | 3 | 14 | ~45 |
| M7 — Desktop UI | 3 | 19 | ~60 |
| M8 — Production Hardening | 3 | 14 | ~55 |
| **Total** | **27 epics** | **158 tasks** | **~462 developer-days** |

**Note:** The per-task estimate totals exceed the milestone calendar estimates in the Roadmap because multiple services in M2, M3 are worked on sequentially within each milestone period, and some tasks within an epic can overlap when dependencies allow. The Roadmap calendar estimates reflect elapsed calendar time assuming focused sequential work; the backlog estimates reflect cumulative effort.
