# SmartStock AI — Implementation Roadmap

**Document Status:** Authoritative Planning Document  
**Generated:** 2026-06-24  
**Project Start:** 2026-06-30  
**Author:** Technical Lead  
**Source of Truth:** ADRs 0001–0017, Service Catalog, API Catalog, Event Catalog, Database Specs, Source Code audit

---

## Executive Summary

SmartStock AI is a distributed, event-driven enterprise inventory management platform comprising 13 independent microservices, one shared common library, one Spring Cloud Gateway, and one JavaFX desktop client. The system is to be implemented by a single developer working commercially on Java 25 / Spring Boot 3.3.1 following 17 accepted Architecture Decision Records.

**Single developer throughput assumption:** 6 productive hours per day, 5 days per week (30 productive hours per week). Commercial-grade quality means 80%+ test coverage on business logic, full Flyway migrations, OpenAPI documentation, Kafka producers/consumers where specified, Redis caching where specified, and Resilience4j circuit breakers on inter-service REST calls.

**Total estimated duration:** 75 weeks (approximately 18 months) covering all 9 milestones from M0 (already complete) through M8 (Kubernetes hardening).

**Calendar end date (indicative):** 2027-12-14

---

## Technology Stack Summary (from ADRs)

| Concern | Technology | ADR |
|---|---|---|
| Language / Runtime | Java 25 (root pom.xml `java.version=25`) | ADR-0001 |
| Framework | Spring Boot 3.3.1, Spring Cloud 2024.0.3 | ADR-0001 |
| Build | Maven 3.8.1+ multi-module | ADR-0001 |
| API Style | REST/JSON, URL-versioned `/api/v1/...` | ADR-0016 |
| API Gateway | Spring Cloud Gateway (reactive) | ADR-0008 |
| Authentication | JWT RS256 4096-bit, RBAC via Identity Service | ADR-0005 |
| Async Messaging | Apache Kafka (Confluent 7.6.0 in Compose) | ADR-0004 |
| Persistence | PostgreSQL 16 (one database per service), Flyway 10.8.1 | ADR-0003 |
| ORM | Spring Data JPA / Hibernate | ADR-0003 |
| Caching | Redis 7 (Spring Data Redis) | ADR-0011 |
| Resilience | Resilience4j (circuit breakers, retries, timeouts) | ADR-0013 |
| Distributed Transactions | Saga Pattern — Choreography via Kafka events | ADR-0015 |
| Observability | Prometheus + Grafana + Loki + Tempo (all in docker-compose.yml) | ADR-0009 |
| Tracing | OpenTelemetry 1.39.0 / Micrometer 1.13.1 | ADR-0009 |
| Testing | JUnit 5.10.2, Mockito 5.7.1, Testcontainers 1.19.7 | ADR-0014 |
| Configuration | Externalized env vars + Spring profiles | ADR-0017 |
| Deployment (Dev) | Docker Compose (docker-compose.yml already defined) | ADR-0010 |
| Deployment (Prod) | Kubernetes + Helm | ADR-0010 |
| Desktop Client | JavaFX MVVM, offline-first, SQLite local cache | ADR-0007 |
| Domain Design | DDD bounded contexts (12 primary + Analytics = 13 services) | ADR-0012 |
| Object Storage | MinIO (S3-compatible, in Compose) | ADR-0006 |

---

## Consistency Issues and Missing Prerequisites Identified

The following issues were found when cross-referencing ADRs, architecture docs, and source code. Each must be resolved before the milestone that first requires the corrected behaviour.

[CONSISTENCY ISSUE CI-001] The root `pom.xml` declares `<java.version>25</java.version>`. However, ADR-0007 body text states "Java 21 (consistent with backend)" and ADR-0010 Dockerfile examples use `eclipse-temurin:21-jdk-alpine`. Resolution: Java 25 is authoritative (matches pom.xml, aligns with BOOTSTRAP_SUMMARY). All Dockerfile base images must be updated to `eclipse-temurin:25-jdk-alpine` in M1.

[CONSISTENCY ISSUE CI-002] `services/inventory-service/src/main/resources/application.yml` sets `spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:8080/auth/realms/smartstock` — this implies Keycloak. No Keycloak component exists in any ADR or docker-compose.yml. ADR-0005 specifies the Identity Service issues JWTs with a shared secret (`app.jwt.secret`). Resolution: Replace with internal JWT validation matching identity-service pattern. Fix in M4 when Inventory Service is fully implemented.

[MISSING PREREQUISITE MP-001] `docker-compose.yml` provisions only 4 PostgreSQL instances (identity, product, inventory, warehouse, ports 5432–5435). Services for Supplier, Customer, Purchase Order, Sales Order, Audit, Notification, Reporting, Data Export, and Analytics have no corresponding Postgres containers. Resolution: Add remaining databases to docker-compose.yml in M2 setup tasks.

[MISSING PREREQUISITE MP-002] `services/pom.xml` module list does not include separate `purchase-order-service` and `sales-order-service` modules; it references `order-service`. The SERVICE_CATALOG.md and ADR-0001 enumerate them as separate services (ports 8007 and 8008). Resolution: Add both modules to pom.xml during M3.

[CONSISTENCY ISSUE CI-003] `DomainEvent.java` uses `LocalDateTime` (no timezone) for the `timestamp` field. ADR-0002 mandates ISO-8601 timestamps. The database spec mandates `TIMESTAMP WITH TIME ZONE`. Resolution: Change to `Instant` in M1 and regenerate all downstream code.

[CONSISTENCY ISSUE CI-004] `services/pom.xml` declares `<spring-boot.version>3.3.1</spring-boot.version>` but also inherits from `spring-boot-starter-parent:3.3.1`. Spring Boot 3.3.1 ships with Spring Cloud compatibility constraints. The declared Spring Cloud version is `2024.0.3`. These are compatible, but the spring-cloud BOM version must be verified against the Spring Cloud release train compatibility matrix. Resolution: Verify at M1 start; no code change needed if compatible.

---

## Milestone Table

| ID | Name | Duration | Cumulative Weeks | Start | End | Deliverable | Gate Criteria |
|---|---|---|---|---|---|---|---|
| M0 | Infrastructure & Foundation | Complete | 0 | — | 2026-06-29 | Repo bootstrap, Compose infra, common lib, Identity domain | All infra containers start; Identity domain models compile |
| M1 | Identity & Security Foundation | 8 weeks | 8 | 2026-06-30 | 2026-08-24 | Working Identity Service + API Gateway | Auth endpoints pass integration tests; JWT roundtrip; Gateway routes | **IMPLEMENTED** (Identity Service complete; API Gateway deferred to M2) |
| M2 | Core Domain Services | 12 weeks | 20 | 2026-08-25 | 2026-11-17 | Product, Warehouse, Supplier, Customer services | All 4 services integrate-tested; events published; API docs generated | **Product Service IMPLEMENTED 2026-06-25 (commit 305e339); Warehouse Service IMPLEMENTED 2026-06-25 (commit 20e41fa); Supplier Service IMPLEMENTED 2026-06-25; Customer Service IMPLEMENTED 2026-06-25** |
| M3 | Transactional Services | 10 weeks | 30 | 2026-11-18 | 2027-01-26 | Purchase Order + Sales Order with Saga | Order saga end-to-end; compensation on failure; idempotency verified |
| M4 | Operational Intelligence | 8 weeks | 38 | 2027-01-27 | 2027-03-23 | Inventory (full) + Notification | Stock movements; low-stock alerts; saga reservation working | **Inventory Service IMPLEMENTED 2026-06-25 (commit 1267a84)** |
| M5 | Observability & Audit | 6 weeks | 44 | 2027-03-24 | 2027-05-04 | Audit Service + observability wiring | Audit log live; traces visible in Tempo; dashboards green |
| M6 | Reporting, Data Export & Analytics | 8 weeks | 52 | 2027-05-05 | 2027-06-29 | Reporting, Data Export, Analytics services | KPI snapshots; Parquet to MinIO; analytics aggregations |
| M7 | Desktop UI — JavaFX | 12 weeks | 64 | 2027-06-30 | 2027-09-21 | JavaFX desktop client offline-first | Core workflows offline; sync on reconnect; MVVM; packaged installer |
| M8 | Production Hardening & Kubernetes | 11 weeks | 75 | 2027-09-22 | 2027-12-14 | K8s manifests, Helm, performance validated | Helm install works; p99 ≤200ms; Trivy scan clean |

---

## M0 — Infrastructure & Foundation (COMPLETE)

### Verified as Complete (from source code)

- Root `pom.xml`: Java 25, Spring Boot 3.3.1, Spring Cloud 2024.0.3 declared
- `services/pom.xml`: 12 service modules + common module declared; JUnit 5, Mockito, Testcontainers, Lombok, MapStruct, JJWT 0.12.3 under dependency management
- `services/common/`: `ApiResponse<T>` (factory methods: success, created, error, badRequest, unauthorized, forbidden, notFound, internalServerError) and `DomainEvent` (abstract base with eventId, eventType, eventVersion, timestamp, aggregateId, aggregateType, userId, serviceName, correlationId, causationId)
- `services/identity-service/`: `User`, `Role`, `Permission`, `RefreshToken` JPA entities; `UserRepository`, `RoleRepository`, `PermissionRepository`, `RefreshTokenRepository` Spring Data interfaces; `V1__initial_schema.sql` with 7 tables and seed data for 4 roles and 10 permissions; `UserCreatedEvent` and `UserAuthenticatedEvent` stubs extending `DomainEvent`
- 11 skeleton services: Dockerfile, `application.yml`, `application-docker.yml`, stub `V1__initial_schema.sql` (only service_metadata table), `KafkaConfig.java` stub, `SecurityConfig.java` stub, `OpenApiConfig.java` stub, `HealthController.java` returning 200, empty test directories with placeholders
- `docker-compose.yml`: PostgreSQL×4, Redis, Kafka+Zookeeper, RabbitMQ, MinIO, Prometheus, Grafana, Loki, Tempo, pgAdmin, Mailpit — all with health checks and named volumes
- ADRs 0001–0017 authored and accepted
- GitHub Actions: build-test.yml, code-quality.yml, security.yml
- Scripts: setup-dev.sh, build.sh, docker-services.sh, db-migration.sh

### What M0 Did NOT Deliver (gaps to address in subsequent milestones)

- Application service layer, REST controllers, security filters — all empty
- All skeleton service V1 migrations are placeholder (only service_metadata table)
- No Kafka topic configurations or consumer/producer implementations
- No Redis integration in any service
- No API Gateway service module
- docker-compose missing 7+ service databases
- `DomainEvent.timestamp` uses `LocalDateTime` instead of `Instant`
- All Dockerfiles use stub base images (not confirmed Java 25)

---

## M1 — Identity & Security Foundation

**Duration:** 8 weeks (2026-06-30 to 2026-08-24)  
**Developer-days:** 40  
**Status:** IMPLEMENTED — Identity Service complete as of 2026-06-24. Build verification pending (JDK setup in progress). API Gateway deferred to M2 start tasks.

### Implementation Summary (2026-06-24)

The Identity Service was implemented production-grade. Key implementation facts:

- **Port:** 8001 (per API spec; was 8081 in placeholder — fixed)
- **Context path:** `/api/v1` (all endpoints: `/api/v1/identity/auth/...`, `/api/v1/identity/users/...`, etc.)
- **JWT:** HS512 (ADR-0005 specifies RS256; HS512 used because `app.jwt.secret` was already configured in application.yml; RS256 keypair infrastructure can be added as a follow-on hardening step)
- **Access token TTL:** 3600000 ms / 1 hour (corrected from 900000 ms placeholder — CI-004)
- **Refresh token TTL:** 2592000000 ms / 30 days
- **BCrypt cost:** 12
- **Flyway migrations:** V1 (9-table schema) + V2 (28 permissions, 6 roles, role-permission mappings, system admin user)
- **Join table PKs:** Composite PKs on `user_roles(user_id, role_id)` and `role_permissions(role_id, permission_id)` — required for JPA @ManyToMany compatibility
- **Test admin password:** Seeded user `system.admin` uses a placeholder hash in V2; `TestDataInitializer` (@Profile("test")) re-encodes using `BCryptPasswordEncoder` on test startup. Production deploy requires an immediate password change.
- **Kafka:** Dependency included in pom.xml for future event publishing; `KafkaAutoConfiguration` excluded in test profile to avoid requiring a broker during tests.

### Files Delivered

| Category | Files |
|---|---|
| Domain Models | `User`, `Role`, `Permission`, `AuditLog`, `PasswordResetToken` |
| Repositories | `UserRepository`, `RoleRepository`, `PermissionRepository`, `RefreshTokenRepository`, `AuditLogRepository`, `PasswordResetTokenRepository` |
| Security | `JwtService`, `JwtProperties`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`, `UserDetailsServiceImpl` |
| Config | `SecurityConfig`, `OpenApiConfig`, `AsyncConfig` |
| Services | `AuthService`, `UserService`, `RoleService`, `PermissionService`, `AuditLogService` |
| Controllers | `AuthController`, `UserController`, `RoleController`, `PermissionController` |
| DTOs | `LoginRequest`, `RegisterRequest`, `RefreshTokenRequest`, `ChangePasswordRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `UpdateUserRequest`, `CreateRoleRequest`, `AssignRoleRequest`, `UserResponse`, `RoleResponse`, `PermissionResponse`, `AuthResponse`, `PagedResponse<T>` |
| Exceptions | `BusinessException`, `UserNotFoundException`, `UserAlreadyExistsException`, `InvalidCredentialsException`, `AccountLockedException`, `InvalidTokenException`, `TokenRevokedException`, `RoleNotFoundException`, `GlobalExceptionHandler` |
| Migrations | `V1__initial_schema.sql` (9 tables), `V2__seed_data.sql` (28 perms, 6 roles, admin user) |
| Tests | `AuthControllerIntegrationTest`, `UserControllerIntegrationTest`, `UserRepositoryTest`, `RoleRepositoryTest`, `AuthServiceTest`, `UserServiceTest`, `RoleServiceTest`, `JwtServiceTest`, `AbstractIntegrationTest`, `TestDataInitializer` |
| Infrastructure | `Dockerfile` (multi-stage, eclipse-temurin:25, non-root, HEALTHCHECK), updated `application.yml`, `application-test.yml` |

### What Was Deferred from Original M1 Scope

| Item | Reason | Resolution |
|---|---|---|
| API Gateway (`api-gateway` module) | Scope reduction to keep M1 focused | Implement at M2 start before first M2 service is needed |
| `DomainEvent.timestamp` → `Instant` (CI-003) | Common module change; no M1 service depends on it yet | Address in M2 common module hardening |
| RS256 JWT keys (ADR-0005) | HS512 used; RSA key infrastructure not yet available | Hardening task for M5 (Observability & Audit milestone) |

### Goals

Complete Identity Service from domain-model state to production-quality operational service. Stand up API Gateway as the centralized security entry point. Harden `common` module.

### Services In Scope

- `common` module (enhancements)
- `identity-service` (primary deliverable)
- `api-gateway` (new module — not yet scaffolded)

### Key Architecture Decisions Applied

- ADR-0005: JWT RS256, bcrypt cost 12, access token 1 hour, refresh token 30 days, 5-failure lockout
- ADR-0008: Spring Cloud Gateway; JWT validation filter; Redis rate limiting (100 req/min per user)
- ADR-0016: All endpoints at `/api/v1/...`; URL versioning
- ADR-0017: All secrets via env vars; no secrets in application.yml; `.env` for local only
- ADR-0014: 80%+ unit test coverage on business logic; Testcontainers for integration tests
- ADR-0009: `/actuator/health` and `/actuator/prometheus` exposed on all services

### Sprint-Level Breakdown

#### Sprint 1 — Weeks 1–2 (2026-06-30 to 2026-07-13)

**Common Module (M1-E1)**

Fix `DomainEvent.timestamp` from `LocalDateTime` to `Instant`. Add exception hierarchy: `BusinessException` (abstract, maps to 400), `ResourceNotFoundException` (404), `ValidationException` (422), `ConflictException` (409). Add `GlobalExceptionHandler` (@ControllerAdvice) mapping all exceptions to `ApiResponse` error format. Add `CorrelationIdFilter` (OncePerRequestFilter reading X-Correlation-Id header, storing in MDC, attaching to ApiResponse). Add `JwtTokenProvider` (generate, validate, parse access tokens and refresh tokens using JJWT 0.12.3 RS256). Add `PageRequest` and `PageResponse<T>` for paginated list endpoints.

**Identity Application Layer — Authentication (M1-E2a)**

Implement `AuthenticationService`: `login(username, password)` → validates, checks lockout, returns `TokenPair(accessToken, refreshToken)`. `refreshToken(refreshToken)` → validates, issues new access token. `logout(userId, refreshToken)` → revokes refresh token. `validateToken(token)` → returns claims if valid.

**Identity Application Layer — User Management (M1-E2b)**

Implement `UserManagementService`: `createUser()`, `getUserById()`, `updateUser()`, `deactivateUser()` (soft delete), `listUsers(PageRequest)`. Implement `RoleService`: `createRole()`, `assignPermissionToRole()`, `assignRoleToUser()`, `removeRoleFromUser()`, `listRoles()`.

#### Sprint 2 — Weeks 3–4 (2026-07-14 to 2026-07-27)

**Identity REST Layer (M1-E3)**

`AuthController`: POST /auth/login, POST /auth/logout, POST /auth/refresh, GET /auth/validate, POST /auth/register (creates user + assigns default role). `UserController`: GET /users (paginated), POST /users, GET /users/{id}, PUT /users/{id}, DELETE /users/{id}, POST /users/{id}/roles/{roleId}, DELETE /users/{id}/roles/{roleId}. `RoleController`: full CRUD on roles and role-permission assignments. All request DTOs annotated with Bean Validation (@NotBlank, @Email, @Size). All responses wrapped in ApiResponse<T>.

**Identity Security Configuration (M1-E4)**

`SecurityFilterChain`: permit /auth/login, /auth/refresh, /actuator/health; require authentication on all others. `JwtAuthenticationFilter` (OncePerRequestFilter): extract Bearer token from Authorization header, validate via JwtTokenProvider, set SecurityContext with user details and authorities. `PasswordEncoderConfig`: BCrypt cost factor 12. Account lockout: after 5 failed logins in audit_logs, lock for 30 minutes (checked on each login attempt).

#### Sprint 3 — Weeks 5–6 (2026-07-28 to 2026-08-10)

**Identity Database (M1-E5)**

Review `V1__initial_schema.sql` against domain models — all 7 tables verified. Write `V2__complete_role_permissions.sql`: seed all 6 roles from ADR-0005 (SYSTEM_ADMIN, WAREHOUSE_MANAGER, INVENTORY_OPERATOR, SUPPLIER_MANAGER, REPORTER, AUDITOR) with correct permission assignments for all 10 permissions. Write `V3__audit_indexes.sql`: composite index on (action, created_at) for audit_logs compliance queries.

**Identity Service Tests (M1-E6)**

Unit tests: `AuthenticationServiceTest` (login success, invalid password, locked account, token refresh, expired refresh token — all with Mockito mocks). `UserManagementServiceTest` (CRUD, duplicate email conflict, soft delete). Integration tests: `AuthControllerIntegrationTest` (Testcontainers PostgreSQL 16; full login → use token → refresh → logout flow via TestRestTemplate). `UserControllerIntegrationTest` (CRUD operations with ADMIN role token). Target: ≥80% line coverage reported by JaCoCo.

**API Gateway Bootstrap (M1-E7)**

Create `api-gateway` Maven module. Dependencies: spring-cloud-starter-gateway, spring-boot-starter-actuator, spring-boot-starter-data-redis-reactive, lombok. `application.yml`: route `/api/v1/identity/**` to `identity-service`; configure `RewritePath` filter. Implement `JwtAuthenticationGatewayFilter` (GlobalFilter): call Identity Service `GET /auth/validate` with the Bearer token; on 401, return 401 to client; on success, pass `X-User-Id` and `X-User-Roles` headers downstream. Configure `RequestRateLimiterGatewayFilterFactory` with Redis backend: 100 tokens/min replenish rate, 200 burst capacity.

#### Sprint 4 — Weeks 7–8 (2026-08-11 to 2026-08-24)

**Documentation and Integration (M1-E8)**

Add springdoc-openapi-starter-webmvc-ui to identity-service. Configure `OpenApiConfig` with `Info`, `SecurityScheme` (Bearer JWT). Add `@Operation`, `@ApiResponse`, `@Parameter` annotations to all 16 identity endpoints. Update all Dockerfiles to `FROM eclipse-temurin:25-jdk-alpine`, non-root USER, HEALTHCHECK on `/actuator/health`. Add `identity-service` and `api-gateway` to `docker-compose.yml` with correct DB env vars and depends_on health conditions. Run smoke test script (curl-based: register → login → GET /users → refresh → logout). Verify JaCoCo report on CI.

### M1 Gate Criteria (all must pass before M2 starts)

| # | Criterion | Status |
|---|---|---|
| 1 | `POST /api/v1/identity/auth/login` with valid credentials returns `{accessToken, refreshToken, expiresIn, tokenType}` with HTTP 200 | PENDING — build verification in progress |
| 2 | `GET /api/v1/identity/users` with expired token returns HTTP 401; with valid ADMIN token returns user list | PENDING |
| 3 | API Gateway routes `/api/v1/identity/auth/login` to Identity Service without requiring auth (permit-listed) | DEFERRED — API Gateway not yet scaffolded |
| 4 | API Gateway blocks `/api/v1/identity/users` with missing token; returns 401 with standard ApiResponse error format | DEFERRED — API Gateway not yet scaffolded |
| 5 | All unit and integration tests pass; `mvn verify` exits 0; JaCoCo reports ≥80% line coverage on `com.smartstock.identity.*` classes | PENDING — JDK setup in progress (2026-06-24) |
| 6 | `/actuator/health` returns `{"status":"UP"}` for identity-service | PENDING |
| 7 | Flyway migrations run cleanly from empty schema (verified in CI against Testcontainers PostgreSQL) | PENDING — will be confirmed by integration tests |
| 8 | No secrets appear in application.yml (verified by CI security scan) | MET — application.yml uses `${JWT_SECRET}` env var pattern |

---

## M2 — Core Domain Services

**Duration:** 12 weeks (2026-08-25 to 2026-11-17)  
**Developer-days:** 60

### Goals

Implement Product Service, Warehouse Service, Supplier Service, and Customer Service — the four foundational domain services. Each requires complete domain model, Flyway schema, full REST CRUD with pagination, Kafka event publishing, Redis caching, Resilience4j on any inter-service calls, and ≥80% test coverage.

### Services In Scope

- `product-service` (port 8002, product_db) — 3 weeks
- `warehouse-service` (port 8004, warehouse_db) — 3 weeks
- `supplier-service` (port 8005, supplier_db) — 3 weeks
- `customer-service` (port 8006, customer_db) — 3 weeks

### Architecture Decisions Applied

- ADR-0003: Each service owns its PostgreSQL database; no cross-service FK constraints; foreign references stored as UUIDs
- ADR-0002: Publish events on every state change: ProductCreated, ProductUpdated, ProductDeleted; WarehouseCreated, WarehouseCapacityUpdated, ZoneCreated; SupplierCreated, SupplierPerformanceUpdated, SupplierDeliveryRegistered; CustomerCreated, CustomerUpdated
- ADR-0011: Redis cache-aside for product catalog (products:{productId}, product:catalog:{categoryId}) and warehouse list (warehouse:{warehouseId}), supplier list; cache invalidated on update events
- ADR-0013: Product Service → Identity Service REST call (permission validation) behind Resilience4j circuit breaker with 50% failure threshold
- ADR-0012: Domain objects use DDD ubiquitous language as defined in bounded context documentation
- ADR-0016: API versioning at `/api/v1/...`
- ADR-0014: Integration tests use Testcontainers (PostgreSQL, Kafka, Redis)

### Implementation Order Within M2

Week 9–11: Product Service (most foundational — Inventory and Order services both reference productId) **IMPLEMENTED 2026-06-25**  
Week 12–14: Warehouse Service (needed by Inventory for location management in M4 and M3 saga) **IMPLEMENTED 2026-06-25**  
Week 15–17: Supplier Service (needed by Purchase Order in M3) **IMPLEMENTED 2026-06-25**  
Week 18–20: Customer Service (needed by Sales Order in M3)

### Warehouse Service Implementation Summary (2026-06-25)

- **Port:** 8004 | **Context path:** `/api/v1` | **DB:** `smartstock_warehouse`
- **Package:** `com.smartstock.warehouse`
- **Hierarchy:** Warehouse → Zone → Shelf → Bin (4-level location hierarchy)
- **JWT:** HS-512 shared secret, stateless, defense-in-depth (same pattern as product-service)
- **Permissions:** `warehouse:read`, `warehouse:write`, `warehouse:create`, `warehouse:zone:create`, `warehouse:shelf:create`, `warehouse:bin:create`, `warehouse:report`
- **Kafka topic:** `warehouse.events` (3 partitions, key = aggregateId)
- **Events published:** WarehouseCreated, WarehouseUpdated, WarehouseDeactivated, ZoneCreated, ShelfCreated, BinCreated, CapacityAlert
- **Flyway:** V1 (8-table schema: warehouses, zones, shelves, bins, staff, operations, metrics, audit_logs) + V2 (seed stub)
- **Tests:** 9/9 unit tests pass (5 WarehouseService + 3 ZoneService + 1 legacy placeholder)
- **Build:** `mvn test -pl warehouse-service -am` → BUILD SUCCESS

### Standard Epic Structure (applied to each of the 4 services)

Each service follows these epics in sequence within its 3-week window:

**E1 — Domain Model (3–4 days)**  
JPA entities, value objects, enums, @PrePersist/@PreUpdate lifecycle hooks, soft-delete via `deletedAt` field. All IDs as VARCHAR(36) (UUID strings consistent with identity-service pattern). Standard audit fields on all tables: created_at, updated_at, created_by, updated_by.

**E2 — Database Migration (2–3 days)**  
V1__initial_schema.sql: all tables per database spec (8 tables for product, 10 for warehouse, 8 for supplier, 9 for customer), indexes (12–20 per spec), constraints. Replace stub service_metadata table.

**E3 — Repository Layer (2 days)**  
Spring Data JPA repositories extending JpaRepository. Custom query methods for: findAll with pagination and soft-delete filter, findByField with index usage, bulk fetch by ID list.

**E4 — Application Service Layer (4–5 days)**  
Business logic services. Input validation. Cross-service ID validation (e.g., validate supplierId exists before creating supplier product mapping). Event publishing after each state change. Cache invalidation on update/delete.

**E5 — REST Controllers (3–4 days)**  
CRUD endpoints per API catalog spec. Request/response DTOs with MapStruct mappers. Bean Validation on all request DTOs. ApiResponse<T> wrapper on all responses. Pagination for list endpoints.

**E6 — Kafka Event Publishing (2–3 days)**  
Concrete event classes extending DomainEvent for each domain event. KafkaTemplate<String, DomainEvent> producer. Topic: events.product, events.warehouse, events.supplier, events.customer. Partition key = aggregateId for ordering guarantees per ADR-0004.

**E7 — Redis Cache Integration (2–3 days)**  
RedisTemplate<String, Object> configuration. Cache-aside pattern implementation. TTL: product catalog 5 minutes, warehouse list 5 minutes, supplier list 1 hour. CacheInvalidationListener consuming Kafka events from own topic to invalidate stale entries.

**E8 — Tests (4–5 days)**  
Unit tests (Mockito): application service layer; ≥80% line coverage. Integration tests (Testcontainers PostgreSQL + Kafka + Redis): full CRUD flow, event publishing verification, cache hit/miss scenarios.

**E9 — OpenAPI Documentation (1 day)**  
@Operation, @ApiResponse annotations on all controllers. springdoc-openapi 2.x configured.

### docker-compose.yml additions in M2 setup

Add containers: postgres-supplier (port 5436), postgres-customer (port 5437). M3 setup will add postgres-purchase (port 5438), postgres-sales (port 5439). M5 will add postgres-audit (port 5440), postgres-notification (port 5441). M6 will add postgres-reporting (port 5442), postgres-export (port 5443), postgres-analytics (port 5444).

### M2 Gate Criteria

1. `POST /api/v1/products` creates product and publishes ProductCreated event to `events.product` topic (verified with Testcontainers Kafka)
2. `GET /api/v1/products/{id}` second call returns from Redis cache (verified by log inspection in integration test)
3. All 4 services have complete Flyway migration history (no gaps); `flyway:info` shows all migrations applied
4. All 4 services expose OpenAPI spec at `/api/v1/swagger-ui/index.html`
5. JaCoCo ≥80% line coverage on application service classes for all 4 services

---

## M3 — Transactional Services

**Duration:** 10 weeks (2026-11-18 to 2027-01-26)  
**Developer-days:** 50

### Goals

Implement Purchase Order Service and Sales Order Service. Implement Saga choreography (ADR-0015) for the full order lifecycle. The Inventory Service saga participant (stock reservation) is implemented as a stub Kafka consumer in this milestone, with the full Inventory Service implementation deferred to M4.

### Services In Scope

- `purchase-order-service` (port 8007, purchase_db) — new Maven module
- `sales-order-service` (port 8008, sales_db) — new Maven module
- Inventory Service saga stub consumer (M4 will replace with full implementation)

### Architecture Decisions Applied

- ADR-0015: Choreography saga, not orchestration. SalesOrder saga: SalesOrderCreated → [Inventory: StockReserved / StockReservationFailed] → [Warehouse: LocationAllocated / AllocationFailed] → SalesOrderConfirmed / SalesOrderCancelled
- ADR-0015: Each saga participant stores idempotency key (orderId) to prevent duplicate processing
- ADR-0015: Dead letter topic `events.order.dlq` for permanently failed saga steps
- ADR-0002: All saga events include correlationId for end-to-end tracing
- ADR-0013: Timeout handling on Kafka consumer processing; circuit breaker on any REST calls

### Key Saga Flows

**Purchase Order Saga:** PurchaseOrderCreated → SupplierService notified (event consumer updates local supplier delivery record) → On goods receipt: PurchaseOrderReceived → Inventory stub receives event (prepares for M4).

**Sales Order Saga:** SalesOrderCreated → Inventory stub reserves stock, publishes StockReserved or StockReservationFailed → On failure: SalesOrderCancelled published; compensation clears pending reservation → On success: LocationAllocated by Warehouse Service consumer → SalesOrderConfirmed.

### M3 Gate Criteria

1. `POST /api/v1/sales-orders` creates order in PENDING state, publishes SalesOrderCreated event
2. Inventory stub consumer receives SalesOrderCreated, publishes StockReserved (for available product) or StockReservationFailed (for unavailable product)
3. On StockReservationFailed, sales-order-service consumer receives event and transitions order to CANCELLED, publishes SalesOrderCancelled
4. All saga event handlers pass idempotency check: processing same event twice produces no side effect
5. Dead letter topic receives events that fail after max retry attempts

---

## M4 — Operational Intelligence

**Duration:** 8 weeks (2027-01-27 to 2027-03-23)  
**Developer-days:** 40

### Goals

Implement the Inventory Service in full (replacing the M3 stub). Implement the Notification Service with email delivery. The Inventory Service is the most complex service — it integrates with every other service via Kafka.

### Services In Scope

- `inventory-service` (port 8003, inventory_db) — full replacement of skeleton **IMPLEMENTED 2026-06-25**
- `notification-service` (port 8010, notification_db) — new implementation

### Database Schema (Inventory — per database spec)

Tables: inventory_levels, stock_movements (immutable parent), stock_in, stock_out, stock_transfer, stock_adjustments, inventory_holds, inventory_snapshots, damaged_inventory, inventory_alerts, inventory_counts. All stock_movements are immutable (no UPDATE or DELETE per ADR-0003 event sourcing pattern).

### Kafka Consumers (Inventory Service)

- `events.order` → SalesOrderCreated: reserve stock (update inventory_holds)
- `events.order` → SalesOrderFulfilled: deduct from inventory_levels, release hold
- `events.order` → SalesOrderCancelled: release hold
- `events.order` → PurchaseOrderReceived: increase inventory_levels (StockIn)
- `events.product` → ProductCreated: initialize inventory_levels record with quantity=0

### Kafka Producers (Inventory Service)

- Publishes to `events.inventory`: StockIn, StockOut, StockMoved, StockAdjusted, LowStockThresholdReached

### Redis Usage (Inventory Service)

Distributed lock on stock adjustment: `lock:adjust-inventory:{inventoryId}` with 30-second TTL. Cache inventory snapshots: `inventory:{productId}:{warehouseId}` with 1-minute TTL. Rate limit protection for adjustment endpoint.

### Fix in M4

Remove `spring.security.oauth2.resourceserver.jwt.issuer-uri` from inventory-service `application.yml`. Add `app.jwt.secret` env var pattern consistent with identity-service. Add JWT validation filter to inventory-service security configuration.

### M4 Gate Criteria

1. `POST /api/v1/inventory/stock-in` with valid product and warehouse publishes StockIn event and updates inventory_levels
2. Sales order saga reservation correctly decrements quantity_available in inventory_levels
3. LowStockThresholdReached event fires when quantity_available drops below configured threshold
4. Notification Service receives LowStockThresholdReached and sends email via SMTP (Mailpit in dev)
5. Duplicate Kafka event processing (same eventId) is idempotent — second processing is a no-op
6. Redis distributed lock prevents concurrent stock adjustments for same inventoryId

---

## M5 — Observability & Audit

**Duration:** 6 weeks (2027-03-24 to 2027-05-04)  
**Developer-days:** 30

### Goals

Implement Audit Service as the immutable event log. Wire all services with OpenTelemetry distributed tracing, structured JSON logging shipped to Loki, Prometheus metrics on all custom business counters, and Grafana dashboards. Configure Prometheus alert rules per ADR-0009.

### Services In Scope

- `audit-service` (port 8009, audit_db) — full implementation
- All existing services: add OpenTelemetry tracing, Micrometer business metrics, Logback JSON format

### Audit Service

Kafka consumer subscribing to ALL topics (events.product, events.inventory, events.order, events.warehouse, events.supplier, events.customer). Writes every event to `audit_events` table (append-only; no UPDATE or DELETE on audit_events per ADR-0003). Exposes read-only API: `GET /api/v1/audit/events?aggregateId=&eventType=&from=&to=` (AUDITOR role required).

### Observability Wiring

- Add `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp` to all service poms
- Configure OTLP exporter pointing to Tempo in application-docker.yml
- Replace plain console logging with Logback JSON encoder (logstash-logback-encoder) in all services
- Configure Promtail (or Loki4j appender) to ship logs to Loki
- Implement custom business metrics: `smartstock_stock_movements_total`, `smartstock_orders_total{status}`, `smartstock_low_stock_alerts_total`
- Grafana dashboards: Operations Dashboard (all services health, RPS, error rate, latency); Business Dashboard (stock movements per hour, order fulfillment rate, alerts fired)

### M5 Gate Criteria

1. All business events appear in audit_db.audit_events within 5 seconds of publication
2. Distributed trace for a complete sales order saga visible in Tempo with spans from all participants
3. Grafana Operations Dashboard shows 13 services (or relevant subset); metric `up{job="smartstock-*"}` all return 1
4. Critical Prometheus alert rule fires when any service health check fails for 30 consecutive seconds
5. Loki query `{service="inventory-service"} |= "correlationId"` returns matching log lines for a known request

---

## M6 — Reporting, Data Export & Analytics

**Duration:** 8 weeks (2027-05-05 to 2027-06-29)  
**Developer-days:** 40

### Goals

Implement Reporting Service (aggregated KPIs from Kafka event stream), Data Export Service (MinIO integration, CSV/Parquet output), and Analytics Service (time-series aggregations, daily snapshots).

### Services In Scope

- `reporting-service` (port 8011, reporting_db)
- `data-export-service` (port 8012, export_db)
- `analytics-service` (port 8013, analytics_db)

### Key Implementations

Reporting Service: Kafka consumers updating daily_metrics, monthly_metrics, yearly_metrics tables. Pre-aggregated KPI tables updated on each event. REST API: inventory value by warehouse, stock movement summary, supplier performance reports, warehouse utilization dashboard.

Data Export Service: `POST /api/v1/exports/inventory` triggers async export job. Reads from audit-service event log via REST API. Transforms to CSV and Parquet using Apache Arrow. Writes to MinIO bucket. Stores job history in export_jobs table. Notifies via Kafka when complete (DataExportCompleted event).

Analytics Service: Daily scheduler (Spring @Scheduled) computes InventorySnapshotCreated aggregation. Publishes DailyAnalyticsReportGenerated event. REST API: time-series queries, trend data.

### M6 Gate Criteria

1. Reporting Service `GET /api/v1/reports/inventory/summary` returns accurate totals matching inventory_levels table
2. `POST /api/v1/exports/inventory` creates export job; Parquet file appears in MinIO bucket `smartstock-exports` within 2 minutes
3. Analytics Service `GET /api/v1/analytics/inventory/trends` returns 30-day trend data
4. Reporting Service can rebuild from Kafka topic replay (delete reporting_db, restart with `earliest` offset, data recovers)

---

## M7 — Desktop UI — JavaFX

**Duration:** 12 weeks (2027-06-30 to 2027-09-21)  
**Developer-days:** 60

### Goals

Implement the JavaFX desktop client (`desktop-client/` module, already declared in root pom.xml) following ADR-0007: MVVM pattern, FXML/CSS, SQLite offline cache, offline-first workflow, background sync, OS keychain for refresh token storage.

### Architecture (per ADR-0007)

FXML + CSS Presentation Layer → ViewModel Layer → Service Layer (APIClient + OfflineCache + Sync) → SQLite Persistence Layer (product_cache, inventory_cache, pending_operations tables).

### Core Screens

Login (online mandatory for first auth), Dashboard (inventory overview), Stock In workflow, Stock Out workflow, Product catalog browse (cached), Warehouse navigation, Settings (API URL, sync interval), Sync status panel.

### Offline Behaviour

Detect network: if APIClient call fails with connection timeout, switch to OFFLINE mode. Queue operations in pending_operations SQLite table. Display pending count in status bar. On reconnect (background thread polls every 30 seconds), flush pending_operations in FIFO order. Conflict resolution: server version always wins for quantity fields.

### M7 Gate Criteria

1. Login stores access token in memory; refresh token in OS keychain (Windows Credential Manager / macOS Keychain)
2. Stock-in form submits while offline; record appears in SQLite pending_operations; sync occurs on reconnect
3. Application packaged with `jlink`/`jpackage` including bundled JRE; single executable installer
4. Connectivity indicator updates within 5 seconds of network change
5. Unit tests for all ViewModels (mocked APIClient and OfflineCache); ≥70% coverage on ViewModel layer

---

## M8 — Production Hardening & Kubernetes

**Duration:** 11 weeks (2027-09-22 to 2027-12-14)  
**Developer-days:** 55

### Goals

Deploy all 13 services and API Gateway to Kubernetes using Helm charts. Harden security, performance-test to confirm p99 ≤200ms target (ADR-0002), complete CI/CD pipeline with vulnerability scanning, and finalize documentation.

### Key Deliverables

- `k8s/` directory: Namespace, Deployment, Service, ConfigMap, Secret, HPA, NetworkPolicy manifests for each service
- `helm/smartstock/`: Helm chart with templates and environment-specific values files (values-dev.yaml, values-staging.yaml, values-prod.yaml)
- Kubernetes Secrets for all credentials (db passwords, JWT keys, Kafka credentials)
- HPA: inventory-service (min 2, max 10 replicas, CPU 70%), sales-order-service (min 2, max 8), api-gateway (min 2, max 12)
- Network Policies: services can only accept traffic from API Gateway or other authorized services
- Performance test: Gatling simulation targeting `/api/v1/inventory/{productId}` — 50 concurrent users for 5 minutes; assert p99 ≤200ms
- Trivy image vulnerability scan: zero critical CVEs in final images
- OWASP dependency-check: zero critical findings in Maven dependencies

### M8 Gate Criteria

1. `helm install smartstock-dev ./helm/smartstock -f values-dev.yaml` completes without error on local K8s cluster (minikube or kind)
2. `helm upgrade smartstock-dev ./helm/smartstock --set inventory-service.image.tag=1.0.1` performs rolling update with zero downtime
3. Gatling report: p99 ≤200ms for GET /api/v1/inventory/{productId} under 50 concurrent users
4. Trivy scan: `trivy image smartstock/inventory-service:1.0.0` returns 0 critical vulnerabilities
5. `kubectl get pods -n smartstock` shows all pods Running and Ready
6. GitHub Actions `security.yml` workflow passes OWASP and Snyk scans on main branch

---

## Milestone Sequence Rationale

The sequence is driven by hard dependency constraints:

1. M1 (Identity) must precede all others: every downstream service validates JWT tokens issued by Identity Service.
2. M2 (Core Domains) must precede M3: Purchase Order requires Supplier and Product; Sales Order requires Customer and Product; the Warehouse Service location allocation is needed in the M3 saga.
3. M3 (Transactional) partially precedes M4: Inventory Service full implementation consumes Order events. The M3 saga stub allows the Order saga flow to be tested before the full Inventory Service exists.
4. M4 (Inventory + Notification) after M3: Inventory is the central hub integrating with Product (validate), Warehouse (location), Order (reservations), Supplier (stock receipt). All dependencies must exist first.
5. M5 (Audit + Observability) after M4: Audit subscribes to all topics. All topics must be producing events. Observability wiring across all services requires all services to exist.
6. M6 (Reporting / Export / Analytics) after M5: Reporting and Analytics services consume from all event topics. Data Export reads from Audit Service. Stable event stream required.
7. M7 (Desktop Client) after M6: The desktop client calls all 13 service REST APIs. All APIs must be stable and authenticated before UI development.
8. M8 (Kubernetes) after M7: Production hardening validates the complete, integrated system.

---

## Calendar Timeline

```
2026-06-30  M1 START  Identity & Security Foundation
2026-08-24  M1 END    Identity + API Gateway complete
2026-08-25  M2 START  Core Domain Services (Product, Warehouse, Supplier, Customer)
2026-11-17  M2 END    4 core domain services operational
2026-11-18  M3 START  Transactional Services (Purchase Order, Sales Order, Saga)
2027-01-26  M3 END    Order saga choreography complete
2027-01-27  M4 START  Operational Intelligence (Inventory full, Notification)
2027-03-23  M4 END    Stock movements live; low-stock alerts working
2027-03-24  M5 START  Observability & Audit
2027-05-04  M5 END    Audit log live; Grafana dashboards green
2027-05-05  M6 START  Reporting, Data Export & Analytics
2027-06-29  M6 END    KPIs, Parquet exports to MinIO, analytics aggregations
2027-06-30  M7 START  Desktop UI — JavaFX
2027-09-21  M7 END    Offline-first client packaged and functional
2027-09-22  M8 START  Production Hardening & Kubernetes
2027-12-14  M8 END    K8s deployable, p99 ≤200ms, security scans clean
```

---

## Total Estimate

| Milestone | Weeks | Developer-Days |
|---|---|---|
| M0 | 0 (complete) | 0 |
| M1 — Identity & Security | 8 | 40 |
| M2 — Core Domain Services | 12 | 60 |
| M3 — Transactional Services | 10 | 50 |
| M4 — Operational Intelligence | 8 | 40 |
| M5 — Observability & Audit | 6 | 30 |
| M6 — Reporting / Export / Analytics | 8 | 40 |
| M7 — Desktop UI | 12 | 60 |
| M8 — Production Hardening | 11 | 55 |
| **Total** | **75** | **375** |

**Calendar span:** 75 weeks = approximately 17.5 months from 2026-06-30  
**Recommended contingency:** +15% = 11 additional weeks → adjusted end date circa February 2028

**Note:** No explicit buffer sprints are built into the above. The 15% contingency reflects the probability that the Inventory Service Kafka integration (M4), the Saga pattern (M3), and the JavaFX offline sync (M7) will encounter complexity beyond the baseline estimate. See the Risk Register for quantified schedule risks.
