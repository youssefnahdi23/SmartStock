# SmartStock AI — Technical Debt Register

**Date:** 2026-06-27
**Scope:** Full repository — 16 Maven modules (`services/`), infrastructure, CI, docs.
**Method:** Static code analysis of the implemented services (no code modified). Grounded in the actual source, not the design docs.
**Sources of truth:** ADRs (`docs/decisions`), implementation under `services/`, prior reviews (`architecture-audit.md`, `repository-review.md`, `KNOWN_LIMITATIONS.md`).

> **How this differs from the existing reviews.** `architecture-audit.md` reviews the *design docs* (scores 89/100) and `repository-review.md` covers *build/config hygiene*. This register looks at the **runtime and code-level engineering debt** that those two did not: event delivery guarantees, concurrency correctness, cross-cutting duplication, the (non-functional) event integration wiring, and the unimplemented AI pipeline. Several items here are **more severe** than anything in the prior reports.

---

## Executive Summary

The platform has clean layering and good documentation, but the *running system* carries meaningful debt concentrated in four areas, each of which maps to one of the prioritization lenses requested:

| Lens | Headline debt |
|------|---------------|
| **Correctness / production-readiness** | A duplicate Flyway version stops a service booting; fire-and-forget events silently lose data; no optimistic locking on stock. |
| **Scalability** | No caching layer (ADR-0011 unimplemented), no concurrency control on hot aggregates, no consumer back-pressure/DLQ. |
| **Maintainability** | Security + Kafka + config code is copy-pasted across ~13 services while the `common` module holds only 2 classes. |
| **Future AI integration** | The entire analytics + data-export (data-lake/feature) pipeline is empty skeletons, and published events are not durably persisted anywhere — there is no data to train on. |

### Debt count by severity

| Severity | Count | Blocks production? |
|----------|-------|--------------------|
| 🔴 Critical | 5 | Yes — all 5 |
| 🟠 High | 7 | Mostly (see each) |
| 🟡 Medium | 6 | No (risk amplifiers) |
| 🟢 Low | 6 | No |

---

## 🔴 Critical

### C-1 — Duplicate Flyway version `V2` in inventory-service (service will not start)

- **Description:** `inventory-service` ships two migrations with the same version:
  [V2__add_stockin_inspection_columns.sql](services/inventory-service/src/main/resources/db/migration/V2__add_stockin_inspection_columns.sql) and [V2__seed_data.sql](services/inventory-service/src/main/resources/db/migration/V2__seed_data.sql). Flyway aborts on startup with *"Found more than one migration with version 2"*.
- **Risk:** Inventory Service — the correctness-critical core of a stock platform — **cannot boot against a clean database**. This is masked today only because live runtime was never exercised (`KNOWN_LIMITATIONS` K-5). It is a guaranteed cold-start failure.
- **Estimated effort:** 30 min (renumber one migration to `V3`/`V4`, verify ordering against existing `V3` if any; run `flyway validate`).
- **Recommended fix:** Renumber `V2__seed_data.sql` → next free version; add a `flyway validate` gate to CI so duplicate/identical versions fail the build, not the deploy.
- **Blocks production:** **Yes.**

### C-2 — Events are fire-and-forget `@Async` with swallowed exceptions (silent data loss, dual-write)

- **Description:** Every event publisher (e.g. [InventoryEventPublisher.java](services/inventory-service/src/main/java/com/smartstock/inventory/service/InventoryEventPublisher.java), and the equivalents in product/warehouse/supplier/customer/purchase-order/sales-order) publishes via `@Async` `KafkaTemplate.send(...)` wrapped in a `try/catch` that only logs on failure. There is **no transactional outbox** (no `outbox` table anywhere in the repo).
- **Risk:** Classic **dual-write problem**. The DB transaction commits, then an asynchronous Kafka send happens *outside* that transaction. If the broker is down, the partition is unavailable, or the process dies between commit and send, the event is **lost forever** (only a log line remains). For an event-driven system whose downstream state (audit, notifications, reporting) *and future ML training data* derive from these events, this means **permanent, undetectable divergence** between services and a corrupted analytical record. `@Async` also breaks ordering guarantees.
- **Estimated effort:** 3–5 days (introduce a shared transactional-outbox: write event rows in the same DB tx as the state change, relay via a poller or Debezium CDC; idempotent producer config).
- **Recommended fix:** Implement the outbox pattern in `common` and adopt it in all 7 producers; enable `acks=all` + idempotent producer; remove `@Async` fire-and-forget for domain events. (Directly enables ADR-0006 data-lake fidelity.)
- **Blocks production:** **Yes** — for any deployment that relies on event-driven consistency or AI data capture.

### C-3 — No optimistic/pessimistic locking on mutable aggregates (lost updates / overselling)

- **Description:** No `@Version` field and no `@Lock`/`PESSIMISTIC_*` usage exists in any entity (grep across `services/**`). Inventory aggregates such as `InventoryLevel`, `StockOut`, `InventoryHold` ([domain/model](services/inventory-service/src/main/java/com/smartstock/inventory/domain/model/)) are read-modify-write under concurrency with no guard.
- **Risk:** Two concurrent stock-out / reservation operations can **lost-update** each other, driving stock negative or **overselling** — the single most damaging class of bug for an inventory platform. The defect is invisible at low load and surfaces exactly when the system scales (a scalability *and* correctness issue).
- **Estimated effort:** 1–2 days (add `@Version` to mutable aggregates; choose optimistic with retry for hot rows, pessimistic for reservation paths; add concurrent integration tests).
- **Recommended fix:** Add JPA optimistic locking (`@Version`) to all mutable aggregates; wrap stock reservation/decrement in a retry-on-`OptimisticLockException` policy; add a Testcontainers concurrency test proving no oversell.
- **Blocks production:** **Yes** for inventory/order paths.

### C-4 — The event integration is wired to topics/events nobody publishes (non-functional end-to-end)

- **Description:** The **only** consumer in the entire codebase is [OrderEventListener.java](services/customer-service/src/main/java/com/smartstock/customer/service/OrderEventListener.java). It listens on topic `events.order` for an `ORDER_COMPLETED` event. But Sales Order Service publishes to `sales-order.events` and emits `SalesOrderCreated/Confirmed/.../DeliveryCompleted` (per `project_m3_progress` and its publisher) — **never** `events.order` / `ORDER_COMPLETED`. The topic name and event type do not exist on the producer side.
- **Risk:** The advertised event-driven flows **do not actually run**. No stock is deducted when a sale ships; customer statistics never update; audit/reporting receive nothing. The system is effectively a set of isolated CRUD services that *write* events into a void — there are 7 producers and **1 consumer, and that consumer is mis-wired**. This is a correctness gap hidden by the fact that runtime was never exercised (K-5).
- **Estimated effort:** 2–4 days (define a canonical topic/event-name contract; correct the listener; implement the genuinely-needed consumers: inventory-deduction on sale, audit sink).
- **Recommended fix:** Establish a single source of truth for topic names + event types (constants in `common`, validated by contract tests); fix `OrderEventListener` to the real topic/type; add the missing consumers required for the documented sagas.
- **Blocks production:** **Yes** — core business flows are inoperative.

### C-5 — No live-runtime verification of any of the above

- **Description:** Per `KNOWN_LIMITATIONS` K-5/K-6, no service has ever been started against live infra; integration tests (`*IntegrationTest`, Testcontainers) are excluded from the default build, and there is **no single full-stack compose** (K-1) to run the system end-to-end.
- **Risk:** C-1 through C-4 are exactly the class of defect that only a running system exposes — and none of them would survive first boot. The green "169 tests" are predominantly unit tests (34 unit vs 23 integration files, integration excluded by default), so they validate components in isolation, not the wired system. Confidence in production-readiness is currently **unfounded**.
- **Estimated effort:** 2–3 days (full-stack compose for all 8 services + infra; a Docker-enabled CI job that runs the integration suite and a smoke flow).
- **Recommended fix:** Author the full-stack `docker-compose`; add a CI stage with Docker that runs integration tests + a happy-path smoke test (create product → stock-in → sales order → ship → verify stock + customer stats).
- **Blocks production:** **Yes.**

---

## 🟠 High

### H-1 — Cross-cutting code is copy-pasted across ~13 services; `common` holds only 2 classes

- **Description:** `JwtService`, `JwtAuthenticationFilter`, `SecurityUserDetails`, `SecurityConfig`, and `KafkaConfig` are duplicated in nearly every service ([12+ `SecurityConfig.java`](services/), 13 `KafkaConfig.java`, 8 `JwtService.java`), each a near-identical copy. Meanwhile the shared [common module](services/common/src/main/java/com/smartstock/common/) — declared as a dependency by all services — contains only `ApiResponse` and `DomainEvent`.
- **Risk:** **Maintainability and security.** A JWT validation fix or a Kafka serialization change must be made in 8–13 places; copies drift (the `JwtService` files already range 58–110 lines). A security patch applied to 12 of 13 copies is a silent vulnerability. This is the single largest drag on future change velocity.
- **Estimated effort:** 3–5 days (extract a `smartstock-security-starter` and `smartstock-messaging-starter` auto-configuration into `common`/shared starters; delete per-service copies).
- **Recommended fix:** Move JWT/security/Kafka/exception-handling cross-cutting code into versioned shared Spring Boot starters; services consume via dependency + `application.yml`, not copied classes.
- **Blocks production:** No, but blocks safe maintenance and is a security-fan-out risk.

### H-2 — No saga / compensation despite ADR-0015 (distributed transactions unmanaged)

- **Description:** No `saga`/`compensat*` code exists anywhere. Order services publish lifecycle events but there is no orchestrator and no compensating action when a downstream step fails (e.g. payment captured but stock unavailable).
- **Risk:** Multi-service operations have **no atomicity or rollback**. A partially-completed order leaves permanently inconsistent state across inventory/order/customer with no recovery path — compounded by C-2 (lost events) and C-4 (dead wiring).
- **Estimated effort:** 1–2 weeks (choreography or orchestration saga for the order fulfilment flow; idempotent steps; compensations).
- **Recommended fix:** Implement the order-fulfilment saga (ADR-0015) with explicit compensations; piggyback on the outbox from C-2 for reliable step events.
- **Blocks production:** Yes for any flow spanning >1 service with side effects.

### H-3 — No Kafka consumer error handling, DLQ, retry, or idempotency

- **Description:** [OrderEventListener.java](services/customer-service/src/main/java/com/smartstock/customer/service/OrderEventListener.java) processes and `save`s with no dedup key, no `DefaultErrorHandler`/`DeadLetterPublishingRecoverer`, no retry policy. Kafka is at-least-once.
- **Risk:** On redelivery, `customer.recordOrder(amount)` **double-counts** spend/order totals (non-idempotent). A poison message blocks the partition indefinitely. As real consumers are added (audit, inventory deduction), this pattern multiplies the corruption surface.
- **Estimated effort:** 1–2 days (shared consumer config: error handler + DLT + a `processed_events` idempotency table).
- **Recommended fix:** Add a shared `DefaultErrorHandler` with exponential backoff + dead-letter topics; enforce idempotent consumption via a processed-event-id table; make all handlers idempotent.
- **Blocks production:** Yes once consumers carry business meaning.

### H-4 — AI/analytics pipeline is entirely unimplemented (the product's stated differentiator)

- **Description:** `analytics-service` and `data-export-service` are health-endpoint-only skeletons (no application/domain code beyond config). No service persists the event stream to a lake/warehouse; there is no feature store, no `@KafkaListener` sink for analytics, and no Analytics DB spec (`architecture-audit` Critical #3). ADR-0006 ("data-lake AI readiness") has zero implementation.
- **Risk:** **Future AI integration is not merely incomplete — it has no foundation.** Because events are also lossy (C-2) and largely unconsumed (C-4), even after services run there is **no durable, complete historical event record to train models on**. Retrofitting event capture later means lost history that can never be recovered.
- **Estimated effort:** 2–4 weeks for a minimal viable pipeline (durable event-lake sink consuming all topics → object store/warehouse; schema-stable event envelope; Analytics DB spec + ingestion).
- **Recommended fix:** Prioritize a "capture-everything" durable event sink *now* (even before analytics logic) so historical data accrues; specify the Analytics DB; design the feature store against the standardized event envelope. Depends on C-2.
- **Blocks production:** Not for v1 operations; **blocks the AI roadmap** and is time-sensitive (lost history is unrecoverable).

### H-5 — No caching layer despite ADR-0011 (Redis)

- **Description:** No `@Cacheable`, `RedisTemplate`, `@EnableCaching`, or `spring-boot-starter-data-redis` anywhere in services (Redis is provisioned in compose but unused by code).
- **Risk:** **Scalability.** Every read hits Postgres. Hot read paths (product catalog, inventory levels, auth/permission lookups) will not scale horizontally; per-service DB-per-service isolation makes cross-service reads expensive without a cache. Idle infrastructure cost too.
- **Estimated effort:** 2–4 days for the high-value read paths.
- **Recommended fix:** Add Spring Cache + Redis to product catalog and identity permission lookups first; define TTL/eviction; invalidate on the corresponding domain events.
- **Blocks production:** No (functional), but a near-term scalability ceiling.

### H-6 — Resilience patterns (ADR-0013) absent from services

- **Description:** Resilience4j appears only in [api-gateway/pom.xml](services/api-gateway/pom.xml); no circuit breakers, retries, bulkheads, or timeouts in any business service. (Today services don't call each other synchronously — there are no Feign/RestTemplate/WebClient clients — so the gap is currently latent, but Kafka consumer resilience, DB retry, and any future sync call are unguarded.)
- **Risk:** When inter-service calls or external integrations are added, failures will cascade. Even now, transient DB/broker blips have no retry policy.
- **Estimated effort:** 2–3 days when sync calls are introduced; 1 day for consumer/DB retry now.
- **Recommended fix:** Centralize Resilience4j config in a shared starter; apply to consumers and any outbound call as they appear.
- **Blocks production:** No today; yes once synchronous integrations exist.

### H-7 — Integration tests excluded by default; wired-system behavior untested

- **Description:** `*IntegrationTest`/`*RepositoryTest` are excluded from `mvn test` (`KNOWN_LIMITATIONS` K-6); CI's `build-test.yml` provisions Postgres but the default test goal skips the tests that would use it. 23 integration test files never run in the standard pipeline.
- **Risk:** Coverage metrics overstate confidence; the integration bugs in C-1…C-4 sailed through precisely because these tests don't run. Coverage is also uneven and not aggregated (Codecov path points at a non-existent root report — `KNOWN_LIMITATIONS` L-2).
- **Estimated effort:** 1 day (dedicated Docker CI job; fix JaCoCo aggregation/Codecov path).
- **Recommended fix:** Add a required `integration-test` CI job with Docker; add a JaCoCo aggregate module; gate merges on it.
- **Blocks production:** No directly, but it is why the Critical items went undetected.

---

## 🟡 Medium

### M-1 — MapStruct Beta on the production path, with version drift
- **Description:** Parent pins `mapstruct.version=1.6.0.Beta1` ([services/pom.xml](services/pom.xml#L79)); warehouse-service previously overrode to `1.5.5.Final`. A pre-release annotation processor generates production mappers.
- **Risk:** Generated-mapper behavior can change between Beta and GA; cross-service drift yields subtly different mapping semantics.
- **Estimated effort:** 0.5 day (bump to stable `1.6.x` final, add `lombok-mapstruct-binding`, re-verify mappers).
- **Recommended fix:** Pin one stable release in the parent; add `org.projectlombok:lombok-mapstruct-binding` to processor paths.
- **Blocks production:** No (recommended before GA).

### M-2 — Spring Cloud / Spring Boot version skew
- **Description:** Parent imports `spring-cloud-dependencies:2024.0.3` with `spring-boot-starter-parent:3.3.1` ([services/pom.xml](services/pom.xml#L48-L49)). Spring Cloud 2024.0.x targets Boot 3.4.x.
- **Risk:** Unsupported combination; subtle autoconfig/dependency-convergence issues, especially in the gateway (the only heavy Spring Cloud consumer).
- **Estimated effort:** 0.5–1 day (align to a supported Boot+Cloud pair; run the reactor).
- **Recommended fix:** Move to a documented compatible matrix (Boot 3.4.x + Cloud 2024.0.x, or Cloud 2023.0.x + Boot 3.3.x).
- **Blocks production:** No (latent risk).

### M-3 — No event schema versioning / registry / contract tests
- **Description:** Events are POJOs serialized as JSON; some publish via the `DomainEvent` base, but the publisher falls back to `aggregateId = "unknown"` for events that don't extend it ([InventoryEventPublisher.java:45](services/inventory-service/src/main/java/com/smartstock/inventory/service/InventoryEventPublisher.java#L45)). No schema registry, no versioning field discipline, no producer/consumer contract tests.
- **Risk:** **Maintainability + AI.** A producer field rename silently breaks every consumer and the analytical record; with 7–10 year retention (ADR), old and new event shapes must coexist and there is no mechanism for that.
- **Estimated effort:** 3–5 days (standardize the event envelope in `common`, add a schema registry or JSON-schema contract tests in CI).
- **Recommended fix:** One canonical event envelope (type, version, aggregateId, occurredAt, payload); enforce all events extend it; add consumer-driven contract tests.
- **Blocks production:** No, but compounds C-2/H-4 for AI fidelity.

### M-4 — Obsolete `order-service` skeleton still in the reactor
- **Description:** `order-service` is superseded by purchase-order + sales-order but still occupies a module, port (8014), and DB allocation (`KNOWN_LIMITATIONS` K-3).
- **Risk:** Confusion, wasted port/DB allocation, accidental wiring (note C-4's `events.order` topic name echoes this dead service).
- **Estimated effort:** 0.5 day.
- **Recommended fix:** Remove the module and its compose/port/DB allocation once confirmed unused.
- **Blocks production:** No.

### M-5 — No full-stack orchestration; deprecated compose with plaintext creds
- **Description:** `docker-compose.yml` runs infra + gateway only; the 8 services aren't wired in (K-1). `docker-compose.services.yml` is deprecated, duplicates config, and hardcodes plaintext dev passwords (`repository-review` M-4).
- **Risk:** No one-command full-stack run (directly enables the C-5 verification gap); deprecated file normalizes insecure defaults and risks container-name collisions.
- **Estimated effort:** 1–2 days (single parametrized full-stack compose; delete the deprecated file after migrating `Makefile.services`).
- **Recommended fix:** One env-driven full-stack compose; remove `docker-compose.services.yml`.
- **Blocks production:** No (but blocks verification → see C-5).

### M-6 — Committed build/log artifacts in the repo
- **Description:** `services/api-gateway/logs/api-gateway.log` and a rotated `.gz` are committed; compiled `target/` output appears in the tree (`common/target/...`).
- **Risk:** Repo bloat, noisy diffs, potential leakage of runtime data in logs; signals gaps in `.gitignore` hygiene.
- **Estimated effort:** 1 hour (git-rm cached artifacts; tighten `.gitignore`).
- **Recommended fix:** Remove tracked `logs/` and `target/`; ensure both are git-ignored.
- **Blocks production:** No.

---

## 🟢 Low

| ID | Description | Risk | Effort | Fix | Blocks prod |
|----|-------------|------|--------|-----|-------------|
| L-1 | Wildcard imports in several controllers/entities (`customer`, `identity`) | Readability; style-guide violation | 1 h | Configure Spotless to forbid `*` imports; reformat | No |
| L-2 | Low-entropy dev default secrets (`smartstock123`, `admin123`, `minioadmin`) in compose/`.env.example` | Acceptable for dev; dangerous if shipped | 0.5 d | Document mandatory override; fail-fast if defaults detected in prod profile | No |
| L-3 | JaCoCo not aggregated; Codecov path targets a non-existent root report | Misleading coverage signal | 0.5 d | Add JaCoCo aggregate module; fix Codecov path | No |
| L-4 | CI quality gate masked: `spotless:check \|\| spotless:apply` always passes; dated action versions (`upload-artifact@v3`, `codeql-action@v2`) | Style/security regressions slip through | 0.5 d | Make `spotless:check` a hard gate; bump action majors | No |
| L-5 | Skeleton package naming (`com.smartstock.<name>service`) diverges from implemented bounded-context names (`com.smartstock.customer`) | Inconsistency; harmless now | per-service | Align when each skeleton is implemented | No |
| L-6 | Mockito dynamic-agent / CDS warnings in test logs | Cosmetic noise | 15 min | Add `-XX:+EnableDynamicAgentLoading` to Surefire | No |

---

## Prioritized Remediation Path

Ordered to unblock production and protect the AI roadmap, respecting dependencies:

1. **C-1** (30 min) — unblock inventory boot. *Trivial, do immediately.*
2. **C-5 + M-5** (3–4 d) — full-stack compose + Docker integration CI. *Prerequisite to trust every other fix.*
3. **C-4** (2–4 d) — fix the dead event wiring so flows actually run.
4. **C-2** (3–5 d) — transactional outbox. *Foundation for H-2, H-4, M-3.*
5. **C-3** (1–2 d) — optimistic locking on stock. *Correctness under load.*
6. **H-3** (1–2 d) — consumer idempotency + DLQ. *Pairs with C-2/C-4.*
7. **H-1** (3–5 d) — extract shared starters. *Unlocks safe maintenance velocity.*
8. **H-4 capture-sink** (start now, 1 wk MVP) — durable event lake so AI history accrues. *Time-sensitive: lost history is unrecoverable.*
9. **H-2** (1–2 wk) — order-fulfilment saga.
10. **H-5 / H-6 / Medium / Low** — schedule into normal sprints.

### Theme rollup against the requested lenses

- **Scalability:** C-3 (locking), H-5 (cache), H-3/H-6 (consumer resilience), C-2 (`@Async` ordering).
- **Maintainability:** H-1 (duplication — the biggest one), M-3 (event contracts), M-2/M-1 (dependency hygiene), M-4/M-6 (cruft).
- **Future AI integration:** H-4 (pipeline + capture sink), C-2 (event fidelity), M-3 (schema stability), C-4 (events must actually flow to be captured).

---

*Prepared by automated code review. No source was modified. Each item is traceable to a file path above; estimates are engineering-days for a single developer familiar with the stack.*
