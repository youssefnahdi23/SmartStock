# SmartStock AI — Definition of Done (DoD)

**Document Status:** Authoritative Planning Document  
**Generated:** 2026-06-24  
**Source:** ADRs 0001–0017, Engineering Standards (docs/standards/), Testing Guidelines, Java Style Guide, REST API Guidelines, Database Guidelines, Spring Boot Guidelines  
**Applies to:** Every task in the implementation backlog, without exception.  
**Review Cycle:** Reviewed at start of each milestone; updated only with architectural justification.

---

## How to Use This Document

Every task in `implementation-backlog.md` references one or more DoD sections from this document (e.g., `DoD-REST`, `DoD-DomainModel`). Before marking a task as "Done," all criteria in the referenced section(s) AND the Global DoD must pass. No exceptions.

The backlog also references `DoD-MilestoneGate` — that section defines criteria that must ALL pass before the milestone is considered complete and work on the next milestone begins.

---

## DoD-Global — Applies to Every Task

These criteria apply to every single task regardless of type.

### Code Quality
- [ ] Code compiles with zero warnings (Maven `<compilerArg>-Xlint:all</compilerArg>` enabled, with documented suppressions where unavoidable)
- [ ] Code follows `docs/standards/java-style-guide.md`: naming conventions (camelCase methods/fields, PascalCase classes, UPPER_SNAKE_CASE constants), max method length 50 lines, max class length 300 lines, no commented-out code
- [ ] Lombok is used for boilerplate (@Data, @Builder, @Getter, @Setter, @AllArgsConstructor, @NoArgsConstructor) — no hand-written getters/setters
- [ ] No `System.out.println` anywhere; all logging uses SLF4J `private static final Logger log = LoggerFactory.getLogger(...)` or `@Slf4j` (Lombok)
- [ ] No hardcoded strings for business values — use constants, enums, or `@Value` properties
- [ ] No `TODO` or `FIXME` comments in committed code without an accompanying GitHub Issue reference

### Version Control
- [ ] Changes committed to a feature branch (never directly to `main` or `feature/project-bootstrap`)
- [ ] Commit message follows `docs/standards/commit-guidelines.md`: imperative mood, ≤72 chars subject, body explains WHY not WHAT
- [ ] Pull request opened with description, linked to backlog task ID
- [ ] All CI checks pass on the PR: `build-test.yml`, `code-quality.yml`, `security.yml`

### Configuration and Secrets
- [ ] No secrets, passwords, or private keys in any `.java`, `.yml`, `.xml`, or `.properties` file committed to Git
- [ ] All configuration values read from environment variables via `${ENV_VAR:default}`
- [ ] No `spring.jpa.hibernate.ddl-auto=create-drop` in any profile other than `test`
- [ ] Feature-flagged code uses env var `${features.X.enabled:true}` pattern per ADR-0017

### Error Handling
- [ ] All public methods that can fail have documented failure modes (either throws declaration or Javadoc)
- [ ] No bare `catch (Exception e) { }` — exceptions either logged with context and rethrown, or handled explicitly with business meaning
- [ ] HTTP 5xx responses never expose stack traces or internal error details (only logged server-side with correlationId)

### Dependency Management
- [ ] No new dependency added without corresponding entry in `services/pom.xml` dependency management section (BOM-managed where possible)
- [ ] No two different versions of the same library across modules (Maven enforcer verifies this)
- [ ] New dependency justification documented in PR description

---

## DoD-DomainModel — JPA Entity Implementation

Referenced for tasks that implement JPA entity classes.

- [ ] Entity class is in `domain/model/` package within the service
- [ ] Entity implements `@Entity`, `@Table(name = "...")` with explicit table name (no implicit naming)
- [ ] Primary key is `@Id String id` (VARCHAR(36) UUID string, consistent with identity-service pattern) with UUID generated in `@PrePersist` if not set
- [ ] All standard audit fields present: `createdAt (LocalDateTime/Instant)`, `updatedAt`, `createdBy (String userId)`, `updatedBy (String userId)`
- [ ] Soft-delete field present: `deletedAt (LocalDateTime/Instant)` — `null` means active; set to `now()` on logical delete
- [ ] `@PrePersist` sets `id`, `createdAt`, `updatedAt`; `@PreUpdate` sets `updatedAt` only
- [ ] `@Column` explicit on all fields with `nullable`, `length` (VARCHAR max), `unique` (where applicable) — no silent implicit defaults
- [ ] `@Table` declares `indexes` array covering all frequently-queried columns per database spec
- [ ] No database-level foreign key constraints to tables in other services (IDs stored as plain UUID strings)
- [ ] Enum types stored as `@Enumerated(EnumType.STRING)` — never ORDINAL
- [ ] JSON/flexible attributes stored as `@Column(columnDefinition = "JSONB")` with `@Convert` or Jackson annotation
- [ ] Timestamps use `LocalDateTime` with `@Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")` for local business times; `Instant` with `TIMESTAMP WITH TIME ZONE` for audit/event timestamps
- [ ] Equals/hashCode based on `id` field only (not Lombok @Data default which uses all fields — use @EqualsAndHashCode(onlyExplicitlyIncluded = true))
- [ ] No `FetchType.EAGER` on collection associations (all collections LAZY to avoid N+1 queries)
- [ ] Bidirectional associations avoided where possible; when used, `mappedBy` set on the non-owning side
- [ ] Unit test: instantiate entity, set fields, verify `@PrePersist` sets `id` and `createdAt` correctly

---

## DoD-Repository — Spring Data JPA Repository Layer

- [ ] Repository interface extends `JpaRepository<Entity, String>` or `PagingAndSortingRepository` where pagination needed
- [ ] Repository interface is in `domain/repository/` package
- [ ] All custom query methods use JPQL (not native SQL) unless a performance-justified native query with comment explaining the reason
- [ ] All custom query methods that filter soft-deleted records include `AND e.deletedAt IS NULL` in JPQL
- [ ] Pageable-accepting methods return `Page<T>` not `List<T>` to support pagination
- [ ] Named parameter binding used (`@Param`) — no positional parameters (`?1`)
- [ ] No repository method performs cross-entity joins spanning more than 2 tables (complex joins extracted to service layer using separate queries)
- [ ] Repository interface has no business logic — pure data access only
- [ ] Each custom query method has a corresponding unit test verifying the JPQL expression is valid (using @DataJpaTest with in-memory H2 or Testcontainers)

---

## DoD-AppService — Application Service Layer

- [ ] Service class is in `application/service/` package
- [ ] Service class annotated with `@Service`, `@Transactional` at class level (read-only `@Transactional(readOnly = true)` on query methods, default transactional on write methods)
- [ ] Service class depends only on: repository interfaces, event publisher, cache service, other service's client interfaces — never on controllers or infrastructure configs
- [ ] Input validation performed at service layer (not only at controller layer): check business invariants (e.g., quantity > 0, referenced entity exists)
- [ ] Throws typed exceptions from common exception hierarchy (ResourceNotFoundException, ConflictException, BusinessException) — never throws RuntimeException directly
- [ ] Every state-changing method publishes the corresponding domain event (via Kafka event publisher) within the same transaction where possible, or reliably after commit (transactional outbox pattern if needed)
- [ ] Cache interactions: read via cache-aside before DB; write-through or explicit cache invalidation on update/delete
- [ ] All public methods are documented with Javadoc explaining business purpose, preconditions, and postconditions
- [ ] Service does NOT contain SQL or JPA queries inline — delegates to repository
- [ ] Pagination: service accepts `PageRequest` from common module, maps to `Pageable`, returns `PageResponse<T>` from common module
- [ ] Unit tests mock all dependencies; test happy path AND all exception paths; test publishes correct event; test cache is invalidated on update

---

## DoD-REST — REST Controller Layer

- [ ] Controller class is in `presentation/controller/` (or `interfaces/api/v1/`) package
- [ ] Controller annotated with `@RestController`, `@RequestMapping("/api/v1/{resource-path}")`, `@Tag(name="...", description="...")` (OpenAPI)
- [ ] Every handler method annotated with `@Operation(summary = "...")` and `@ApiResponse(responseCode = "...", description = "...")` for all possible HTTP statuses
- [ ] All request bodies annotated with `@Valid` — no manual validation in controller
- [ ] All path variables of type UUID annotated with custom `@ValidUUID` constraint (or use UUID type directly with @PathVariable)
- [ ] All list endpoints accept `page`, `size`, `sortBy`, `sortDir` query parameters; return paginated `ApiResponse<PageResponse<T>>`
- [ ] All responses wrapped in `ApiResponse<T>`: successful creates return HTTP 201 with `ApiResponse.created(data)`; successful reads return HTTP 200; successful deletes return HTTP 204 (no body)
- [ ] Error responses: 400 for validation failure, 401 for unauthenticated, 403 for insufficient permissions, 404 for not found, 409 for conflict, 422 for business rule violation, 500 for unexpected errors
- [ ] Controller does NOT contain business logic — delegates entirely to application service layer
- [ ] CORS: not configured at controller level (handled by API Gateway)
- [ ] Required permission documented in `@Operation(description = "Requires role: X or permission: Y")`
- [ ] Content negotiation: produces `application/json` only (no XML)
- [ ] Integration test verifies each endpoint: happy path, auth failure (401), permission failure (403), not found (404), validation failure (400)

---

## DoD-KafkaProducer — Kafka Event Publishing

- [ ] Domain event class extends `DomainEvent` from `services/common`
- [ ] Event class has `@JsonTypeName("EventTypeName")` annotation for polymorphic deserialization
- [ ] Event class is immutable (all fields final, set via constructor or @Builder)
- [ ] Event `payload` contains ALL data needed by consumers — consumers must NOT need to call back to the producer service to get additional data (per ADR-0002)
- [ ] `correlationId` is propagated from the calling request context (via MDC) into the event
- [ ] `userId` is populated from the security context (from `X-User-Id` header set by gateway)
- [ ] `KafkaProducerConfig` configured with: `acks=all` (strongest durability), `retries=3`, `enable.idempotence=true`, `key-serializer=StringSerializer`, `value-serializer=JsonSerializer`
- [ ] Partition key = `aggregateId` (ensures events for the same entity are ordered per ADR-0004)
- [ ] Kafka send uses `kafkaTemplate.send(topic, partitionKey, event)` — key is always provided (never null)
- [ ] Kafka send result is logged at DEBUG level with topic, partition, offset on success; ERROR level on failure
- [ ] Topic name is a Spring `@Value("${kafka.topics.product}")` property — never hardcoded in producer code
- [ ] Dead letter topic fallback: if send fails after retries, event is published to `{topic}.dlq` with original event payload + failure reason
- [ ] Unit test: verify event published with correct type, aggregateId, correlationId (mock KafkaTemplate, capture ArgumentCaptor)
- [ ] Integration test: publish event, consume with embedded Kafka, verify event fields

---

## DoD-KafkaConsumer — Kafka Event Consumption

- [ ] Consumer class is in `infrastructure/messaging/` package
- [ ] Consumer class annotated with `@Component`, `@KafkaListener(topics = "${kafka.topics.X}", groupId = "${spring.kafka.consumer.group-id}")`
- [ ] Consumer implements idempotency: before processing, check if eventId has already been processed (via a `processed_events` table with unique constraint on eventId, or Redis key `processed:{eventId}` with TTL equal to max expected replay window)
- [ ] If eventId already processed: log at DEBUG level and return immediately (no re-processing)
- [ ] Consumer uses manual offset commit (`AckMode.MANUAL`) to ensure at-least-once delivery — offset committed only after successful processing
- [ ] Consumer is annotated `@Transactional` to ensure DB write and offset commit are atomic where possible
- [ ] Consumer handles `deserialization errors` by routing to dead letter topic — not blocking the consumer group
- [ ] Consumer handles `business logic errors` (e.g., inventory level not found): log at ERROR level, send to dead letter topic, commit offset (do not block)
- [ ] Consumer handles `transient errors` (DB timeout, network issue): do NOT commit offset, let Kafka retry with exponential backoff (Resilience4j retry on consumer)
- [ ] Each consumer method is documented with Javadoc: which event triggers it, what business action is taken, what events are published as a result
- [ ] Unit test: verify happy path processing produces correct state changes (mock repository); verify idempotency (second call with same eventId is no-op); verify failure sends to DLQ
- [ ] Integration test: publish test event to Testcontainers Kafka; verify consumer processes and DB state updated within 5 seconds; verify second identical event is no-op

---

## DoD-DBMigration — Flyway Database Migration

- [ ] Migration file is in `src/main/resources/db/migration/` following Flyway naming: `V{version}__{description}.sql` (two underscores)
- [ ] Migration version numbers are sequential with no gaps; never reuse or modify an existing version
- [ ] Migration is idempotent where possible (`CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`)
- [ ] Migration uses explicitly typed columns: no `TEXT` where `VARCHAR(n)` is appropriate; `DECIMAL(15,2)` not `FLOAT` for monetary values; `TIMESTAMP WITH TIME ZONE` for event/audit timestamps; `UUID` or `VARCHAR(36)` for IDs
- [ ] All tables include standard audit columns: `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, `updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
- [ ] Primary key uses `VARCHAR(36) PRIMARY KEY` (UUID stored as string, consistent with identity-service pattern)
- [ ] Foreign keys within the same service database are defined with `ON DELETE CASCADE` for child tables or `ON DELETE SET NULL` for nullable references
- [ ] No foreign key constraints between tables in different databases
- [ ] All indexes named explicitly: `idx_{table}_{column(s)}` (e.g., `idx_users_email`)
- [ ] Partial indexes used where appropriate: `WHERE deleted_at IS NULL` on soft-delete filtered queries
- [ ] Composite indexes defined for all multi-column filter patterns (e.g., `(product_id, warehouse_id)` for inventory lookups)
- [ ] Seed data (reference data / default records) in separate `V{version}__seed_data.sql` file, not in the schema migration
- [ ] Migration tested locally: `flyway:clean` + `flyway:migrate` runs without error from empty schema
- [ ] Migration tested in CI: Testcontainers PostgreSQL 16 starts cleanly with migration applied
- [ ] Zero-downtime migration strategy used for schema changes to existing tables (add nullable column first, backfill, then add constraint — never `ALTER TABLE ... ADD COLUMN NOT NULL` without default on live table)

---

## DoD-UnitTest — Unit Test Class

- [ ] Test class in `src/test/java/com/smartstock/{service}/unit/` package
- [ ] Test class name follows pattern: `{ClassUnderTest}Test.java`
- [ ] Uses `@ExtendWith(MockitoExtension.class)` — no Spring context loaded
- [ ] All dependencies of the class under test are injected as `@Mock` fields; class under test as `@InjectMocks`
- [ ] Test method names follow pattern: `should{ExpectedBehavior}_when{Condition}()` (per testing-guidelines.md)
- [ ] Each test method has exactly one logical assertion group (arrange, act, assert pattern clearly visible)
- [ ] Both happy path AND exception paths are tested for every public method
- [ ] Event publishing is verified via `verify(kafkaTemplate).send(eq(topic), eq(key), argThat(event -> ...))`
- [ ] Cache interactions are verified via `verify(cacheService).get(...)` and `verify(cacheService).set(...)`
- [ ] No `Thread.sleep` — async behavior tested via `CompletableFuture.get()` or Awaitility
- [ ] No test accesses a real database, network, or file system (all via mocks)
- [ ] JaCoCo line coverage on the class under test: ≥80%; branch coverage: ≥70%
- [ ] Test execution time: each test completes in < 100ms; test class completes in < 2 seconds

---

## DoD-IntTest — Integration Test

- [ ] Integration test class in `src/test/java/com/smartstock/{service}/integration/` package
- [ ] Test class name follows pattern: `{FeatureName}IntegrationTest.java`
- [ ] Uses `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` or `@DataJpaTest`
- [ ] Infrastructure provided via `@Testcontainers` and `@Container` annotations:
  - PostgreSQL: `PostgreSQLContainer<>("postgres:16-alpine")`
  - Kafka: `KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))`
  - Redis: `GenericContainer<>("redis:7-alpine").withExposedPorts(6379)`
- [ ] Test uses `@DynamicPropertySource` to override datasource URL, Kafka bootstrap servers, Redis host with container ports
- [ ] Tests run against real SQL (not H2) — PostgreSQL container ensures production-like behavior
- [ ] Async behavior (Kafka consumption, notifications) awaited with Awaitility: `await().atMost(10, SECONDS).untilAsserted(() -> ...)`
- [ ] Tests are independent: each test method starts with a clean state (use `@Sql(scripts = "classpath:test-data.sql")` or `@Transactional` rollback)
- [ ] Tests do NOT share state via static fields or class-level setup that modifies the database
- [ ] Test verifies both the response (HTTP status, body fields) AND the side effects (DB state, events published)
- [ ] Test execution time: each integration test class completes in < 60 seconds
- [ ] Tests are not flaky: 5 consecutive runs pass without test-order dependency

---

## DoD-OpenAPI — API Documentation

- [ ] `springdoc-openapi-starter-webmvc-ui` added to service pom.xml
- [ ] `OpenApiConfig` class configures `Info` (title, version, description, contact) and `SecurityScheme` (Bearer JWT, scheme "bearer", bearerFormat "JWT")
- [ ] Every `@RestController` has `@Tag(name = "...", description = "...")` at class level
- [ ] Every `@GetMapping`/`@PostMapping`/etc. method has `@Operation(summary = "...", description = "...")` with:
  - `summary`: one-line description (≤60 chars)
  - `description`: 2–3 sentences explaining purpose, required permissions
  - `@Parameter` annotations on all path variables and query parameters
  - `@ApiResponse` for each possible HTTP status code (200, 201, 204, 400, 401, 403, 404, 409, 422, 500)
- [ ] All DTO classes have `@Schema(description = "...")` on the class and `@Schema(description = "...", example = "...")` on each field
- [ ] OpenAPI spec is accessible at `http://localhost:{port}/api/v1/swagger-ui/index.html` without authentication (permit-listed)
- [ ] `openapi.json` spec file can be generated via `mvn springdoc:generate` (or equivalent) and is valid OpenAPI 3.1
- [ ] No sensitive data (passwords, tokens) in example values

---

## DoD-Security — Security Implementation

- [ ] Every REST endpoint specifies required role/permission in `@Operation(description = "Requires role: X")`
- [ ] Every endpoint is either explicitly permit-listed (login, health) or requires valid JWT
- [ ] Endpoints that modify data require write permissions; read-only endpoints require read permissions
- [ ] Resource-level authorization: if a user has access to only warehouse W01, they cannot read/write data for W02 — this check is implemented in the service layer, not only the JWT (warehouse IDs validated against `X-User-Warehouses` header)
- [ ] No secrets appear in logs (passwords, tokens, credit card numbers, PII) — verified by log output review in integration tests
- [ ] Password storage: BCrypt with cost ≥12 (identity-service only)
- [ ] JWT generation: RS256 with ≥4096-bit key pair; keys loaded from env vars (never hardcoded)
- [ ] SQL injection: all database queries use parameterized JPQL or named parameters; no string concatenation in queries
- [ ] Input sanitization: `@Size`, `@Pattern`, `@NotBlank` annotations on all user-provided string inputs
- [ ] File uploads (bulk import): validated for content type, size limit configured; content scanned for embedded scripts
- [ ] The service fails to start if `JWT_SECRET` / `JWT_PRIVATE_KEY_PEM` env var is missing (validated via `@Validated @ConfigurationProperties`)
- [ ] All HTTP connections to external services use HTTPS in staging/production (`application-prod.yml` enforces TLS endpoints)
- [ ] Security test: attempt to access endpoint without token → 401; attempt with valid token but wrong role → 403; attempt with valid token for another warehouse → 403

---

## DoD-Observability — Metrics, Traces, and Logs

Per ADR-0009, three-pillar observability is required.

### Metrics (Prometheus / Micrometer)
- [ ] `/actuator/health` returns `{"status":"UP"}` when service is healthy
- [ ] `/actuator/health/liveness` and `/actuator/health/readiness` return correct states (for K8s probes)
- [ ] `/actuator/prometheus` is exposed and returns valid Prometheus text format
- [ ] Custom business counters registered: at minimum one counter per major operation (stock-in count, order count)
- [ ] All custom metrics prefixed with `smartstock_` and include service label tag

### Distributed Tracing (OpenTelemetry)
- [ ] `micrometer-tracing-bridge-otel` on classpath
- [ ] `opentelemetry-exporter-otlp` configured in `application-docker.yml` pointing to Tempo (port 4317)
- [ ] `X-B3-TraceId` / `traceparent` headers propagated in all `RestTemplate`/`WebClient` / `OkHttpClient` outgoing calls
- [ ] `traceId` and `spanId` appear in log output via MDC bridge (configured in logback-spring.xml)
- [ ] Integration test: perform a request, verify correlationId appears in log output; trace appears in Tempo (verified via Tempo HTTP API)

### Structured Logging (Loki)
- [ ] `logstash-logback-encoder` on classpath
- [ ] `logback-spring.xml` (present as stub in all skeletons) configured with `LogstashEncoder` for `application-docker` profile
- [ ] Every log line includes at minimum: `timestamp`, `level`, `service` (from `spring.application.name`), `correlationId` (from MDC), `traceId` (from MDC via OTel)
- [ ] No PII (personal email, phone, address, credit card) in log output at INFO or above
- [ ] Log levels: ERROR for actionable failures; WARN for degraded performance; INFO for business events (stock in, order created, user login); DEBUG for diagnostic details (only enabled in dev profile)

---

## DoD-Performance — Latency and Throughput

Per ADR-0002, the target response time is <200ms per request.

- [ ] Each REST endpoint has been profiled in integration test and confirmed: p50 ≤50ms, p95 ≤150ms, p99 ≤200ms under single-user sequential load
- [ ] N+1 query problem eliminated: list endpoints do not execute one DB query per row (verified via Hibernate statistics in `@SpringBootTest` with `hibernate.generate_statistics=true`)
- [ ] Cache hit ratio for cacheable resources: first request hits DB; second request hits cache (verified in integration test via mock or metrics)
- [ ] Bulk import endpoints (`POST /products/import`): 100-record import completes in < 5 seconds (verified in integration test)
- [ ] Kafka producer send completes in < 100ms in test environment (non-blocking send, verified via send future timing)
- [ ] Database query explain plans reviewed for all custom JPQL queries: no sequential scans on large tables (index exists)

---

## DoD-MilestoneGate — Gate Criteria Before Next Milestone Starts

These criteria must ALL be true before work on the next milestone begins.

### Gate for M1 (Identity + Gateway complete → begin M2)
- [ ] All M1 tasks marked Done
- [ ] `mvn clean verify` on `services/identity-service` and `services/api-gateway` exits 0 with all tests passing
- [ ] JaCoCo reports ≥80% line coverage on `com.smartstock.identity.*` classes
- [ ] `POST /api/v1/auth/login` returns valid JWT with correct claims (verified by manual curl smoke test)
- [ ] `GET /api/v1/users` returns 401 without token; 200 with ADMIN token (verified)
- [ ] API Gateway rejects request with missing/expired JWT with HTTP 401 in standard ApiResponse format
- [ ] `/actuator/health` returns UP on both services
- [ ] All GitHub CI checks pass on the merge commit to `feature/project-bootstrap`
- [ ] No critical security findings in `security.yml` scan output

### Gate for M2 (Core Domains complete → begin M3)
- [ ] All M2 tasks marked Done
- [ ] All 4 services (product, warehouse, supplier, customer) pass `mvn clean verify` with ≥80% coverage
- [ ] Each service publishes at least one Kafka event verified by integration test
- [ ] Each service's Redis caching demonstrates cache-hit on second read in integration test log
- [ ] OpenAPI spec accessible at swagger-ui for all 4 services
- [ ] docker-compose includes all 4 service containers and databases; `docker compose up` starts all containers healthy

### Gate for M3 (Transactional Services complete → begin M4)
- [ ] All M3 tasks marked Done
- [ ] Sales order saga happy path: SalesOrderCreated → StockReserved → OrderConfirmed (verified end-to-end integration test)
- [ ] Sales order saga failure path: SalesOrderCreated with unavailable product → StockReservationFailed → OrderCancelled (verified)
- [ ] Idempotency verified: replay StockReserved event does not double-confirm the order
- [ ] Dead letter topic receives event after max retries (verified in integration test)
- [ ] All M3 tests pass with ≥80% coverage

### Gate for M4 (Inventory + Notification complete → begin M5)
- [ ] All M4 tasks marked Done
- [ ] Stock-in operation persists movement and publishes StockIn event (verified)
- [ ] Sales order reservation decrements quantityAvailable in inventory_levels (verified)
- [ ] LowStockThresholdReached fires when threshold crossed (verified)
- [ ] Notification Service delivers email to Mailpit within 30 seconds of LowStockThresholdReached (verified)
- [ ] Redis distributed lock prevents concurrent adjustments (verified with concurrent test)
- [ ] Keycloak issuer-uri removed from inventory-service application.yml (CI-002 resolved)
- [ ] All M4 tests pass with ≥80% coverage

### Gate for M5 (Observability + Audit complete → begin M6)
- [ ] All M5 tasks marked Done
- [ ] All business events appear in audit_db.audit_events within 5 seconds (verified)
- [ ] PostgreSQL trigger on audit_events rejects UPDATE/DELETE (verified by test)
- [ ] Distributed trace for a complete saga visible in Tempo (verified via Tempo API query)
- [ ] Grafana Operations Dashboard loads and shows all running services as UP
- [ ] Prometheus alert rule fires in test (verified by silencing a service and checking alertmanager)
- [ ] JSON structured logging with correlationId confirmed in Loki (verified via Loki HTTP API query)

### Gate for M6 (Reporting + Export + Analytics complete → begin M7)
- [ ] All M6 tasks marked Done
- [ ] `GET /api/v1/reports/inventory/summary` returns accurate values matching inventory_levels (verified)
- [ ] Export job creates Parquet file in MinIO within 2 minutes (verified)
- [ ] Reporting Service recovers data correctly after DB wipe and Kafka topic replay (verified)
- [ ] Analytics 30-day trend data available (verified)
- [ ] All M6 tests pass

### Gate for M7 (JavaFX Desktop Client complete → begin M8)
- [ ] All M7 tasks marked Done
- [ ] Login → stock-in → offline stock-in → sync → verify shows correct sequence (manual walkthrough documented)
- [ ] Connectivity indicator updates within 5 seconds of network change (verified in offline integration test)
- [ ] pending_operations SQLite table correctly populated in offline mode; cleared after sync (verified)
- [ ] Application packages as installer and runs on clean machine (Windows MSI or macOS DMG — one platform verified)
- [ ] ViewModel unit tests pass with ≥70% coverage

### Gate for M8 (Production Hardening complete — project fully done)
- [ ] All M8 tasks marked Done
- [ ] `helm install smartstock-dev ./helm/smartstock -f values-dev.yaml` completes without error on local K8s
- [ ] `kubectl get pods -n smartstock` shows all pods Running and Ready after helm install
- [ ] Gatling report: p99 ≤200ms for GET /api/v1/inventory/{productId} under 50 concurrent users
- [ ] Trivy scan: zero critical (CVSS ≥9.0) vulnerabilities in all service images
- [ ] OWASP dependency-check: zero critical findings (CVSS ≥7.0) in Maven dependencies
- [ ] All GitHub Actions CI workflows pass on `main` branch (including security, code quality, build-test)
- [ ] All 17 ADRs remain in Accepted status (no unresolved conflicts discovered during M8)
- [ ] All consistency issues (CI-001 through CI-005) resolved and verified
