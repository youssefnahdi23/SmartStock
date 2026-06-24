# SmartStock AI — Risk Register

**Document Status:** Authoritative Planning Document  
**Generated:** 2026-06-24  
**Owner:** Technical Lead (Youssef Nahdi)  
**Review Cadence:** Reviewed and updated at the end of every milestone gate  
**Scale:** Probability and Impact rated H (High), M (Medium), L (Low). Severity = P × I on a 3×3 scale (HH=9, HM=6, MH=6, MM=4, HL=3, LH=3, ML=2, LM=2, LL=1).

---

## Risk Register Table

| ID | Category | Risk | Probability | Impact | Severity | Status |
|---|---|---|---|---|---|---|
| R-01 | Schedule | Single developer: illness, personal emergency, or burnout delays timeline by 4+ weeks | H | H | 9 | Open |
| R-02 | Technical | Saga choreography (ADR-0015) complexity significantly exceeds estimate; idempotency bugs discovered late | H | H | 9 | Open |
| R-03 | Architectural | Java 25 / Spring Boot 3.3.1 compatibility issues with Kafka, Testcontainers, or other dependencies | M | H | 6 | Open |
| R-04 | Technical | Inventory Service integration complexity (consumes 5+ event types, publishes 5+ events, Redis lock) causes M4 overrun | H | M | 6 | Open |
| R-05 | Technical | JavaFX offline sync conflict resolution (M7) proves more complex than designed in ADR-0007 | H | M | 6 | Open |
| R-06 | Architectural | Consistency Issue CI-002: Keycloak-style JWT issuer-uri in inventory-service persists into production undetected | M | H | 6 | Open |
| R-07 | Schedule | M2 (4 services × 3 weeks each) underestimates Kafka+Redis integration per service; M2 extends by 4+ weeks | M | H | 6 | Open |
| R-08 | Quality | Test coverage falls below 80% on business logic due to time pressure; technical debt compounds across milestones | M | H | 6 | Open |
| R-09 | Infrastructure | docker-compose.yml only has 4 PostgreSQL instances; missing 9+ databases causes local dev environment failures until fixed | H | M | 6 | Identified (CI-005) |
| R-10 | Technical | DomainEvent.timestamp (LocalDateTime) propagates into all 12 skeleton services before CI-003 fix; fix becomes high-cost | M | M | 4 | Identified (CI-003) |
| R-11 | External | Apache Kafka 7.6.0 (Confluent CP) used in docker-compose; Spring Kafka 3.x compatibility edge cases with older Confluent image | L | H | 3 | Open |
| R-12 | Schedule | Kubernetes Helm chart complexity (13 services, multiple environments) in M8 exceeds 11-week estimate | M | M | 4 | Open |
| R-13 | Quality | OpenAPI documentation incomplete or inaccurate; JavaFX client calls endpoints not matching documented contract | M | M | 4 | Open |
| R-14 | Technical | Redis single instance in docker-compose becomes a SPOF; services fail in degraded modes not tested until late | L | M | 2 | Open |
| R-15 | Architectural | Missing prerequisite MP-002: order-service module naming conflict between pom.xml and service catalog; causes build failure when creating purchase-order-service and sales-order-service modules | H | L | 3 | Identified (MP-002) |
| R-16 | Technical | Kafka consumer group rebalancing during saga event processing causes duplicate event processing before idempotency keys are in place | M | H | 6 | Open |
| R-17 | Quality | Testcontainers PostgreSQL 16 image pull failures in CI (network timeout, Docker Hub rate limit) cause flaky CI pipeline | M | M | 4 | Open |
| R-18 | Schedule | JavaFX tooling (jlink, jpackage, platform-specific packaging) on Windows requires more effort than expected; installer build fails on CI (Linux runner) | H | M | 6 | Open |
| R-19 | External | MinIO API compatibility: Apache Arrow Parquet writer requires specific MinIO SDK version; version conflicts with other dependencies | M | M | 4 | Open |
| R-20 | Architectural | ADR-0007 specifies Java 21 in technology section while pom.xml declares Java 25; if a library dependency targets Java 21 module-info and breaks on Java 25 runtime, significant rework needed | M | H | 6 | Identified (CI-001) |
| R-21 | Quality | Audit Service immutability: PostgreSQL trigger preventing UPDATE/DELETE on audit_events not present in V1 migration; discovered only in security review | M | M | 4 | Open |
| R-22 | Technical | Analytics Service daily scheduler conflicts with Reporting Service event consumers when both try to compute metrics from same event stream simultaneously; race condition in reporting_db | L | M | 2 | Open |
| R-23 | Schedule | Resilience4j circuit breaker misconfiguration in API Gateway → Identity Service path causes 100% of requests to fail with cryptic errors; hard to diagnose | M | H | 6 | Open |
| R-24 | Infrastructure | Kafka topic auto-creation (enabled in docker-compose) creates topics with wrong partition count (1 instead of 10); partitioning rework required before performance testing | H | M | 6 | Open |
| R-25 | External | Spring Cloud 2024.0.3 + Spring Boot 3.3.1 BOM incompatibility: `spring-cloud-starter-gateway` reactive base conflicts with spring-boot-starter-web non-reactive in API Gateway | M | H | 6 | Open |

---

## Risk Heat Map (5×5 Scale — Impact vs. Probability)

The following map plots all 25 risks. Rows = Probability (top = High), Columns = Impact (right = High). Each cell contains risk IDs.

```
                     IMPACT
              L       M       H       VH      Critical
         +-------+-------+-------+-------+-------+
    H    |       | R-09  | R-01  |       |       |
         | R-15  | R-04  | R-02  |       |       |
         |       | R-05  |       |       |       |
         |       | R-18  |       |       |       |
         |       | R-24  |       |       |       |
P    +-------+-------+-------+-------+-------+
R    M    |       | R-10  | R-03  |       |       |
O         |       | R-12  | R-06  |       |       |
B         |       | R-13  | R-07  |       |       |
         |       | R-17  | R-08  |       |       |
         |       | R-19  | R-16  |       |       |
         |       | R-21  | R-20  |       |       |
         |       |       | R-23  |       |       |
         |       |       | R-25  |       |       |
    +-------+-------+-------+-------+-------+
    L    |       | R-14  | R-11  |       |       |
         |       | R-22  |       |       |       |
         |       |       |       |       |       |
         +-------+-------+-------+-------+-------+
```

**Red Zone (Severity 9 — HH):** R-01, R-02  
**Orange Zone (Severity 6 — HM or MH):** R-03, R-04, R-05, R-06, R-07, R-08, R-09, R-16, R-18, R-20, R-23, R-24, R-25  
**Yellow Zone (Severity 4 — MM):** R-10, R-12, R-13, R-17, R-19, R-21  
**Green Zone (Severity ≤3):** R-11, R-14, R-15, R-22

---

## Top 5 Risks — Detailed Treatment

### R-01 — Single Developer: Availability Risk

**Category:** Schedule  
**Risk:** A single developer project has zero redundancy. Illness (especially multi-week), personal emergency, or sustained overwork leading to burnout could delay the 75-week timeline by 4+ weeks per occurrence. Two such events would push the project end date past February 2028 even with the 15% contingency.

**Probability:** High — over an 18-month project, the probability of at least one multi-week disruption is greater than 50% for any developer.

**Impact:** High — each week lost is a week of non-compressible delay. Unlike team projects, no colleague can absorb the work.

**Severity:** 9 (HH) — highest possible.

**Mitigation Strategies:**
1. Maintain detailed progress notes and task state at the end of every working day (5-minute commit message + dev diary). If work must stop abruptly, a replacement developer can restart within 2 days.
2. Merge feature branches to `feature/project-bootstrap` at minimum every 2 weeks. Never hold large changes locally for more than 5 working days.
3. Work-in-progress (WIP) cap: no more than 3 tasks in progress simultaneously. Prevents incomplete, non-compilable code accumulating if a pause is forced.
4. Schedule explicit rest periods (at least 1 week off per quarter) to prevent burnout-driven quality collapse.
5. During M8 planning, assess whether an additional developer can be onboarded for the Kubernetes hardening work.

**Contingency:** Build 11-week (15%) contingency into all external commitments. If delay exceeds 4 weeks in any milestone, reduce scope of that milestone to minimum viable deliverable (drop observability wiring, reduce test coverage target to 70% minimum) and flag for next milestone catch-up.

**Owner:** Project Lead  
**Review:** Monthly

---

### R-02 — Saga Pattern Complexity and Idempotency Bugs

**Category:** Technical  
**Risk:** The Saga choreography pattern (ADR-0015, M3) involves 5 services, 12+ event types, and complex compensation logic. Idempotency bugs (processing the same event twice causing double-reservation or double-release) are notoriously difficult to detect and reproduce. Integration bugs may not surface until M4 when the full Inventory Service replaces the stub.

**Probability:** High — distributed transaction patterns reliably surface unexpected edge cases, particularly around consumer rebalancing (R-16), network partitions, and event ordering.

**Impact:** High — inventory data corruption (double-reservation, phantom stock, negative quantities) in the Inventory Service is a core business-data integrity failure. Recovery requires manual reconciliation.

**Severity:** 9 (HH)

**Mitigation Strategies:**
1. Implement idempotency table (`saga_states` in sales_db, `processed_event_ids` in inventory_db) FIRST — before any saga business logic. Idempotency is not an afterthought; it is the foundation.
2. Write the saga failure path integration tests in M3 BEFORE the happy path. Verify compensation triggers correctly before verifying the normal flow.
3. Use deterministic Kafka partition assignment in tests (single-partition topics in Testcontainers) to eliminate rebalancing race conditions during testing.
4. Code review the saga handlers against the ADR-0015 example implementation step-by-step.
5. Add saga state visualization (simple query on `saga_states` table) exposed as `GET /api/v1/sagas/{orderId}/status` for debugging.

**Contingency:** If saga bugs are found in M4 after Inventory Service replaces the stub, allocate up to 2 additional weeks (10 developer-days) for diagnosis and fix. Do not proceed to M5 until all saga integration tests pass without flakiness (5 consecutive runs clean).

**Owner:** Technical Lead  
**Review:** Weekly during M3 and M4

---

### R-03 — Java 25 Dependency Compatibility

**Category:** Architectural  
**Risk:** Java 25 is a newer release. `services/pom.xml` targets `java.version=25` but Spring Boot 3.3.1 officially supports Java 21 and 23 and may have compatibility notes for 25. Key dependencies — Testcontainers 1.19.7, MapStruct 1.6.0.Beta1, JJWT 0.12.3, OpenTelemetry 1.39.0 — may have module-path issues or reflective access restrictions introduced in Java 25.

**Probability:** Medium — Java 25 is a non-LTS release; most enterprise libraries officially support LTS releases (Java 21, Java 23 EA). Java 25 may have preview features that affect compatibility.

**Impact:** High — if a core dependency (Spring Boot, Hibernate, Kafka) does not work on Java 25, downgrading to Java 21 or 23 would require changes to the root pom.xml and all service poms, plus re-verification.

**Severity:** 6 (MH)

**Mitigation:**
1. At M1 start (2026-06-30), run the full Maven build on Java 25 immediately: `mvn clean verify -pl services/identity-service`. If any compilation or test failure related to Java version is found, resolve before writing application logic.
2. Pin the `eclipse-temurin:25-jdk-alpine` Docker base image to a specific patch version (e.g., `:25.0.1_12-jdk-alpine`) to prevent unexpected JDK updates breaking CI.
3. If Java 25 proves incompatible with more than 2 dependencies during M1, create an ADR to downgrade to Java 21 LTS and update all poms. The cost of this change is minimal in M1 but grows linearly with code written.
4. Check Spring Boot 3.3.x release notes for Java 25 support statement before M1 kickoff.

**Contingency:** Downgrade to Java 21 (LTS). Root pom.xml change: `<java.version>21</java.version>`. All Dockerfile base images: `eclipse-temurin:21-jdk-alpine`. Estimated cost: 1 developer-day. Write ADR-0018 documenting the decision.

**Owner:** Technical Lead  
**Review:** M1 kickoff (2026-06-30) and weekly during M1

---

### R-04 — Inventory Service Integration Complexity Overrun

**Category:** Technical  
**Risk:** The Inventory Service (M4) is the most complex service in the platform. It consumes from 3+ Kafka topics (events.product, events.order with 5+ event subtypes), publishes to 1 topic (5 event types), uses Redis distributed locking for concurrent access protection, makes synchronous REST calls to Product and Warehouse services, manages 11+ database tables, and handles the Saga participation (stock reservation, release, deduction). The M4 estimate (8 weeks / 40 developer-days for Inventory + Notification) may be optimistic.

**Probability:** High — integration services are consistently underestimated. The Inventory Service's centrality means any cross-service issue manifests here.

**Impact:** Medium — M4 overrun delays M5 and everything downstream but does not cause data corruption (unlike R-02).

**Severity:** 6 (HM)

**Mitigation:**
1. Spike (2-day prototype) at M4 start: implement StockIn endpoint end-to-end with Kafka event and Redis lock before writing any other Inventory functionality. Identify integration issues early.
2. Replace M3 Inventory stub with a more detailed stub (handles 4 event types, not just 1) to de-risk the Kafka consumer integration.
3. Use in-memory Redis (`testcontainers-redis`) for all integration tests to make distributed lock testing deterministic.
4. Decouple Notification Service from M4 if needed: implement Notification as a simple stub in M4 (just logs the event) and complete email delivery in M5 alongside observability work.

**Contingency:** If Inventory Service implementation exceeds 7 weeks, defer Notification Service full email delivery to M5. Extend M4 by up to 2 weeks and compress M5 from 6 to 4 weeks (auditing is simpler). Total project delay: 0 weeks (absorbed by scope shift).

**Owner:** Technical Lead  
**Review:** Weekly during M4

---

### R-05 — JavaFX Offline Sync Complexity

**Category:** Technical  
**Risk:** ADR-0007 defines a detailed offline-first architecture with SQLite local cache, pending_operations queue, background sync, conflict resolution ("server wins"), and connectivity detection. JavaFX + background threading + SQLite is a non-trivial combination. Testing offline scenarios is difficult (must mock network failures). The sync conflict resolution for inventory quantities (optimistic vs. server-authoritative) may reveal race conditions not apparent during design.

**Probability:** High — offline-first sync is one of the most underestimated UX engineering challenges. The M7 estimate (12 weeks) is the largest single milestone.

**Impact:** Medium — M7 overrun delays M8 but does not block backend services. The desktop client is additional value, not the core backend platform.

**Severity:** 6 (HM)

**Mitigation:**
1. Implement the SyncService and pending_operations queue as the FIRST epic in M7 (before any UI screens). Validate sync mechanics with unit tests before building UI on top.
2. Use TestFX for headless JavaFX testing; establish headless test infra at M7 start before any screen implementation.
3. Reduce scope if needed: defer Warehouse browser screen and Settings screen to post-M7 backlog. Focus M7 on Login, Dashboard, Stock In, Stock Out — the minimum viable desktop client.
4. Mock the network in integration tests via a custom `APIClient` test double that can be switched to OFFLINE mode programmatically.

**Contingency:** If M7 exceeds 12 weeks by more than 3 weeks, reduce scope: Ship with Login + Stock In + Stock Out only (no Product browse, no Warehouse tree). Full feature set delivered in a post-M8 patch. M8 can begin even without a complete JavaFX client (Kubernetes does not depend on the desktop client).

**Owner:** Technical Lead  
**Review:** Weekly during M7

---

## Additional Risk Details

### R-09 — Missing PostgreSQL Instances in docker-compose (IDENTIFIED)

**Status:** Identified. Tracked as MP-001 in implementation roadmap.  
**Mitigation:** Add all 9 missing database containers (supplier, customer, purchase, sales, audit, notification, reporting, export, analytics) to docker-compose.yml during M2 setup task M2-E5-T1 and subsequent setup tasks. Ports 5436–5444 are pre-allocated above.  
**Owner:** Technical Lead  
**Target Resolution:** M2 Week 1 (2026-08-25)

### R-10 — DomainEvent.timestamp Type Mismatch (IDENTIFIED)

**Status:** Identified. Tracked as CI-003 in implementation roadmap.  
**Mitigation:** Fix in M1-E1-T1 (first task). Change `LocalDateTime timestamp` to `Instant timestamp`. Since no other service has implemented DomainEvent usage yet (all stubs), cost is 1 developer-day.  
**Owner:** Technical Lead  
**Target Resolution:** M1 Sprint 1, Week 1 (2026-06-30)

### R-15 — Order Service Module Naming Conflict (IDENTIFIED)

**Status:** Identified. Tracked as MP-002 in implementation roadmap.  
**Mitigation:** During M3-E1-T1 and M3-E1-T2, add `purchase-order-service` and `sales-order-service` to `services/pom.xml`. The existing `order-service` module (if present in the filesystem) must be removed or renamed. Verify no `order-service` directory exists in `services/` before M3.  
**Owner:** Technical Lead  
**Target Resolution:** M3 Week 1 (2026-11-18)

### R-16 — Kafka Consumer Group Rebalancing During Saga

**Category:** Technical  
**Risk:** During a rolling restart or new consumer instance joining the consumer group, Kafka triggers a rebalance that can pause message delivery for 10–30 seconds. If a saga is in-flight during rebalance (e.g., StockReserved was published but Kafka reassigns the partition before the SalesOrderService consumer processes it), the saga step may be delayed indefinitely.

**Mitigation:** Set `session.timeout.ms=45000` and `heartbeat.interval.ms=3000` to reduce false rebalance triggers. Use `enable.auto.commit=false` with manual acknowledgment so in-flight messages are re-delivered after rebalance (at-least-once). Idempotency keys ensure duplicate delivery is safe.

**Owner:** Technical Lead  
**Target Resolution:** M3 (implement as part of KafkaConsumerConfig)

### R-24 — Kafka Topic Partition Count Misconfiguration

**Category:** Infrastructure  
**Risk:** `docker-compose.yml` sets `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"` with no explicit partition count. Topics auto-created with 1 partition in dev. When performance testing in M8 on K8s with 10 partitions (per ADR-0004), consumer parallelism is different. If partition keys are assigned expecting 10 partitions, hash distribution in dev (1 partition) versus prod (10 partitions) may reveal ordering bugs not caught in dev.

**Mitigation:** Add a `KafkaTopicConfig` bean to each service that creates topics explicitly at startup with correct partition counts (using `KafkaAdmin` bean). Topic creation is idempotent (succeeds if topic exists, no-op). This ensures dev and prod have identical partition topology. Set `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"` in docker-compose once all services have topic creation code.

**Owner:** Technical Lead  
**Target Resolution:** M1-E6-T2 / M2-E1-T6 (add topic creation config to each service as Kafka integration is implemented)

### R-25 — Spring Cloud Gateway Reactive vs. Non-Reactive Conflict

**Category:** External  
**Risk:** `spring-cloud-starter-gateway` is built on Spring WebFlux (reactive). The API Gateway module must NOT include `spring-boot-starter-web` (non-reactive Tomcat), which would conflict. All gateway filter implementations must use reactive types (`Mono`, `Flux`, `ServerWebExchange`). A developer unfamiliar with WebFlux may import the wrong starter or write blocking code in a reactive context, causing ThreadDepletion errors under load.

**Mitigation:** Explicitly exclude `spring-boot-starter-web` from api-gateway pom.xml: `<exclusion>spring-boot-starter-web</exclusion>`. Add `spring-boot-starter-webflux` explicitly. Add a CI check: `mvn dependency:tree` output for api-gateway must NOT contain `tomcat-embed-core`. Document in api-gateway README.md.

**Owner:** Technical Lead  
**Target Resolution:** M1-E6-T1 (Maven module setup)

---

## Risk Status Summary

| Status | Count | Risk IDs |
|---|---|---|
| Open | 20 | R-01 through R-25 minus identified |
| Identified (fix planned) | 5 | R-09, R-10, R-15, R-16, R-24 |
| Mitigated | 0 | — |
| Closed | 0 | — |

**Review Schedule:** End of each milestone gate. Status updated at each review. All Open risks reassessed; newly identified risks added with next available ID.
