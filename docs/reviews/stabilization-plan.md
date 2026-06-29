# SmartStock AI — Stabilization Plan

**Date:** 2026-06-27
**Owner:** Chief Software Architect / Principal Reliability Engineer
**Source of truth:** [technical-debt.md](technical-debt.md)
**Objective:** Stabilize the running system so it can **boot, run, publish events reliably, protect inventory correctness, and be ready for automated testing.** No new business features. No architecture rewrite. Preserve Clean Architecture and ADR compliance.

---

## Guiding constraints

- Do **not** add business features, rewrite the architecture, or change public APIs unless a correctness fix requires it (additive, backward-compatible changes to event payloads are permitted — they are internal contracts, not public REST APIs).
- Do **not** skip or ignore failing tests; do **not** leave a service partially migrated. When a cross-cutting pattern is introduced (outbox, locking, idempotency), it is rolled out to **all** affected services within its milestone, or explicitly scoped with a follow-up tracked in progress notes.
- All secrets are placeholders / env-driven. No GitGuardian-detectable secrets.

---

## Milestone breakdown

Ordered per the requested priority and the dependency graph below. Each milestone is an atomic, revertible commit (or a small ordered set of commits).

### M1 — C-1: Duplicate Flyway version (boot blocker)
- **Change:** Renumber `V2__seed_data.sql` → `V3__seed_data.sql` in `inventory-service` (the `V2__add_stockin_inspection_columns.sql` keeps `V2`; ordering preserved — seed runs after the column add). Audit every other service for duplicate versions.
- **Guard:** Add a `flyway validate`-style duplicate-version check to CI so this class of defect fails the build, not the deploy. (Implemented as a lightweight portable check that does not require a live DB.)
- **Verify:**
  - `ls services/inventory-service/src/main/resources/db/migration` → no two files share a version.
  - `scripts/check-flyway-versions.sh` (new) exits 0.
  - `mvn -f services/pom.xml -pl inventory-service -am test` green.
- **Dependencies:** none. Do first.

### M2 — C-5 + M-5: Full-stack Docker Compose + runtime verification
- **Change:** Extend `docker-compose.yml` (already infra + 8 DBs + gateway) with the 8 application services wired to their DBs, Kafka, and env-driven config. Provide a single `docker compose up` path. Remove/retire the deprecated `docker-compose.services.yml`. Add a smoke-flow verification script (create product → stock-in → sales order → deliver → assert stock + customer stats) and document the runtime verification procedure.
- **Verify:**
  - `docker compose config` validates (syntactic + reference check; does not require the daemon).
  - `scripts/smoke-test.sh` documented; executed when a Docker host is available (daemon currently unavailable per K-5 — procedure authored and statically validated).
- **Dependencies:** Independent of M1, but provides the harness that gives confidence in M3–M6. Built early.

### M3 — C-4: Event topic/event-name contract (dead wiring)
- **Change:** Establish a single source of truth for topic names + event types as constants in `common` (`Topics`, event-type registry). Fix `OrderEventListener` (customer-service) to consume the **real** topic (`sales-order.events`) and the **real** event type (`DeliveryCompletedEvent` = order completed). Make the event self-sufficient for the consumer (add `totalAmount` to `DeliveryCompletedEvent` — additive, backward-compatible) so customer spend statistics update on a true terminal state (avoids the cancel/double-count hazard of recording on create). Add a contract test asserting producer topic == consumer topic.
- **Verify:**
  - Unit test: listener records spend exactly once on `DeliveryCompletedEvent`, ignores others.
  - Contract test: `Topics.SALES_ORDER_EVENTS` is the topic the publisher sends to and the listener subscribes to.
  - `mvn -pl customer-service,sales-order-service,common -am test` green.
- **Dependencies:** Pairs with M6 (idempotency makes redelivery safe). Topic constants from this milestone feed M4/M6/M8.

### M4 — C-2: Transactional outbox (no fire-and-forget)
- **Change:** Implement the outbox pattern as shared infrastructure in `common`: `OutboxEvent` JPA entity, `OutboxRepository`, an `OutboxService` that writes an event row in the **same DB transaction** as the state change, and an `OutboxRelay` (`@Scheduled` poller) that publishes unsent rows to Kafka with `acks=all` + idempotent producer, marking them sent. A reusable Flyway migration creates the `outbox` table per service DB. Replace `@Async` fire-and-forget `KafkaTemplate.send` in all 7 producers with outbox writes. Remove the `@Async` executors for domain events.
- **Verify:**
  - Unit test: outbox row written within the service transaction; relay marks sent; failure leaves the row for retry (no loss).
  - `grep -rn "@Async" services/**/EventPublisher.java` → none for domain events.
  - `grep -rln "outbox" services/*/src/main/resources/db/migration` → present for every producer service.
  - Reactor build + unit tests green.
- **Dependencies:** Depends on M3 topic constants. Foundation for H-2/H-4/M-3.

### M5 — C-3: Optimistic locking on mutable aggregates
- **Change:** Add JPA `@Version` to mutable aggregates (`InventoryLevel`, `StockOut`, `InventoryHold`, and order aggregates with mutable lifecycle). Wrap stock reservation/decrement service paths in a retry-on-`OptimisticLockException` policy. Add a Testcontainers concurrency test proving no oversell/lost update. Outbox writes (M4) keep the event emission inside the retried transaction.
- **Verify:**
  - Concurrency integration test (Testcontainers): N concurrent stock-outs never drive stock negative.
  - Unit tests for retry policy.
  - `mvn -pl inventory-service -am test` green; integration profile green where Docker available.
- **Dependencies:** Benefits from M4 (outbox inside the retried tx). Independent of M3.

### M6 — H-3: Consumer retry, DLQ, idempotency
- **Change:** Shared consumer config in `common`: `DefaultErrorHandler` with exponential backoff + `DeadLetterPublishingRecoverer` (DLT). A `processed_events` idempotency table + helper so handlers dedupe by `eventId`. Make `OrderEventListener` (and any new consumer) idempotent.
- **Verify:**
  - Unit/integration test: redelivered `DeliveryCompletedEvent` records spend **once**.
  - Poison message routes to `<topic>.DLT` instead of blocking the partition.
  - Tests green.
- **Dependencies:** Depends on M3 (real consumer) and M4 (event ids). Pairs with M5.

### M7 — H-1: Extract shared security/Kafka/config starters
- **Change:** Move duplicated `JwtService`, `JwtAuthenticationFilter`, `SecurityUserDetails`, `SecurityConfig`, `KafkaConfig` into auto-configured shared modules in `common` (`smartstock-security` + `smartstock-messaging` auto-configuration). Delete per-service copies; services consume via dependency + `application.yml`. Done in waves (reference service first, then the rest) but completed within the milestone — no service left half-migrated.
- **Verify:**
  - `find services -name SecurityConfig.java | wc -l` and `KafkaConfig.java` count drop to the shared module only (plus justified per-service overrides).
  - Full reactor build + all unit tests green; security tests pass per service.
- **Dependencies:** Lower risk after M3/M4/M6 have stabilized the messaging contracts they share.

### M8 — H-4: Durable analytics event-capture sink
- **Change:** Minimal "capture-everything" durable sink: a consumer (in `analytics-service` or `data-export-service`) subscribing to all domain topics, persisting the raw event envelope to a durable store (`captured_events` table / object store) with the standardized envelope. No analytics logic — capture only, so historical data accrues now.
- **Verify:**
  - Integration test: an emitted event lands in `captured_events`.
  - Idempotent capture (reuses M6 dedupe).
- **Dependencies:** Depends on M4 (reliable emission) + M6 (idempotency) + M3 (topic registry).

### M9 — H-7: Integration tests in CI
- **Change:** Add a required `integration-test` CI job (Docker-enabled) that runs the `integration-test` Maven profile across services; fix JaCoCo aggregation/Codecov path. Wire the M1 flyway check and M2 `docker compose config` into CI.
- **Verify:**
  - CI workflow lints/parses; integration job runs the excluded suites.
  - Merge gate documented.
- **Dependencies:** Closes the loop opened by M2; validates M3–M8.

---

## Dependency graph

```
M1 (Flyway)            ── independent, first
M2 (Compose/verify)    ── independent, early (harness for trust)
M3 (Event contract)    ── feeds ▸ M4, M6, M8
M4 (Outbox)            ── needs M3 ▸ feeds M5(tx), M8
M5 (Locking)           ── benefits from M4
M6 (Consumer DLQ/idemp)── needs M3, M4 ▸ feeds M8
M7 (Shared starters)   ── after M3/M4/M6 stable
M8 (Capture sink)      ── needs M3, M4, M6
M9 (CI integration)    ── after M2; validates M3–M8
```

## Per-milestone acceptance ritual

After **every** milestone:
1. `mvn -f services/pom.xml clean test` (unit) — green.
2. Integration tests (`-Pintegration-test`) where the milestone touches wired behavior and Docker is available.
3. Flyway duplicate-version check — green.
4. `docker compose config` (and `docker build` where the daemon is available).
5. Update [stabilization-progress.md](stabilization-progress.md).
6. Commit with a conventional-commit message scoped to the milestone.

## Final acceptance criteria (tracked to closure in the final report)

Boot on fresh DBs · no duplicate Flyway versions · producers/consumers on documented topics · no fire-and-forget domain events · transactional outbox for critical events · inventory protected against lost updates · idempotent consumers · DLQ exists · full-stack compose starts · integration tests in CI · no GitGuardian-detectable secrets · `stabilization-final-report.md` generated.

## Environment note

Maven: `C:\tools\apache-maven-3.9.6` (not on PATH — prepend in PowerShell). JDK 21 (Temurin). Docker daemon currently unavailable (K-5) — container builds and compose runs are authored and statically validated (`docker compose config`); live-runtime steps are scripted and marked for execution on a Docker host.
