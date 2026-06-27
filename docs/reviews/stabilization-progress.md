# SmartStock AI — Stabilization Progress

Living log of the stabilization effort. Plan: [stabilization-plan.md](stabilization-plan.md). Source of truth: [technical-debt.md](technical-debt.md).

| Milestone | Debt | Status | Commit |
|-----------|------|--------|--------|
| M1 | C-1 | ✅ Done | 84fcba9 |
| M2 | C-5, M-5 | ✅ Done | 6e95aea |
| M3 | C-4 | ✅ Done | c73fb23 |
| M4 | C-2 | ✅ Done | f942fbd |
| M5 | C-3 | ✅ Done | aa90953 |
| M6 | H-3 | ✅ Done | 9852c4f |
| M9 | H-7 | ✅ Done | f2fbba9 |
| M8 | H-4 | ✅ Done | _pending_ |
| M7 | H-1 | ⏳ In progress | — |

> Execution note: M9 (H-7) was brought forward ahead of M7/M8 — it is an explicit final
> acceptance criterion ("integration tests run in CI"), low-risk, and it validates every prior
> milestone. M7 (H-1) is maintainability-only (not an acceptance criterion) and, per the line-count
> audit, the duplicated security code has **genuinely diverged** (JwtService 3→110 lines: a stub, a
> token *issuer*, and several *validators*), making a blanket extraction the highest-risk change —
> so it is sequenced last and scoped conservatively.

---

## M1 — C-1: Duplicate Flyway version in inventory-service ✅

**Problem:** `inventory-service` shipped two migrations at version `V2`
(`V2__add_stockin_inspection_columns.sql` and `V2__seed_data.sql`). Flyway aborts on
startup with *"Found more than one migration with version 2"* — a guaranteed cold-start
failure on a fresh DB.

**Fix:**
- Renamed `V2__seed_data.sql` → `V3__seed_data.sql` (`git mv`, history preserved). The
  column-add keeps `V2`; the seed now runs after it — ordering preserved and correct.
- Audited all 14 services with migrations: inventory was the **only** service with a
  collision (warehouse already uses V1/V2/V3 cleanly; others are V1 or V1/V2).
- Added [scripts/check-flyway-versions.sh](../../scripts/check-flyway-versions.sh): a
  portable, DB-free guard that fails on any duplicate version within a service. Excludes
  `target/` build output.
- Wired the guard into [build-test.yml](../../.github/workflows/build-test.yml) as the
  first step, so this class of defect fails the build, not the deploy.

**Verification (run 2026-06-27):**
- `bash scripts/check-flyway-versions.sh` → `OK: no duplicate Flyway migration versions found.`
- `ls services/inventory-service/.../db/migration` → `V1`, `V2`, `V3` (no collision).
- `mvn -f services/pom.xml -pl common,inventory-service -am test` → **BUILD SUCCESS**, 18 tests, 0 failures.

**Notes:** Full live Flyway `migrate`/`validate` against a Postgres instance is exercised
by the integration suite (M9) once a Docker host is available (K-5). The static guard
covers the specific C-1 failure mode deterministically in CI.

---

## M2 — C-5 + M-5: Full-stack compose + runtime verification ✅

**Problem:** `docker-compose.yml` ran infra + 8 DBs + gateway only — the 8 application
services were never wired in, so there was no one-command full-stack run and therefore no
way to exercise the wired system (the reason C-1…C-4 went undetected). A deprecated
`docker-compose.services.yml` duplicated config with the wrong build context and colliding
ports. **Worse:** every business service's `docker` Spring profile hardcoded a broken
datasource — host `postgres` (no such container; only `postgres-<svc>` exist) and stale DB
names (`customer_db`, `purchase_db`, `sales_db`, shared `smartstock`). Because the
Dockerfiles activate the `docker` profile, these overrode any compose env var → guaranteed
connection failure on boot.

**Fix:**
- Rewrote all 7 business-service `application-docker.yml` profiles (product, inventory,
  warehouse, supplier, customer, purchase-order, sales-order) to clean, **env-driven**
  config with correct per-service defaults (`postgres-<svc>` / `smartstock_<svc>`),
  `kafka:29092` internal listener, and consistent actuator/prometheus exposure. Fixed
  supplier's wrong `server.port: 8080`. identity-service has no docker profile and uses its
  env-driven base config (correct).
- Added the 8 application services to [docker-compose.yml](../../docker-compose.yml) on the
  canonical 800x ports, each with `depends_on` its `postgres-<svc>` + `kafka` at
  `condition: service_healthy`, env-driven config, and the JWT secret as a `${JWT_SECRET:-<JWT_SECRET>}`
  placeholder (no hardcoded secret).
- Removed the deprecated `docker-compose.services.yml`; repointed `Makefile.services`
  targets to the unified file and corrected the printed URLs to 800x + gateway.
- Added [scripts/smoke-test.sh](../../scripts/smoke-test.sh) (readiness gate across all 9
  endpoints + happy-path scaffold) and
  [runtime-verification.md](runtime-verification.md) documenting the one-command bring-up.

**Verification (run 2026-06-27):**
- `docker compose config -q` → **OK** (parses + resolves env, no daemon needed).
- `docker compose config --services` → all 8 app services + gateway + infra present;
  app-service count = **8**.
- `bash -n scripts/smoke-test.sh` → syntax OK.

**Notes (honest status):** the Docker **daemon is unavailable here** (K-5), so live
`docker compose up --build` and `smoke-test.sh` execution remain to be run on a Docker
host — commands are in runtime-verification.md. Everything that can be validated without a
daemon (compose parse, service graph, env resolution, script syntax) passes. Live runtime
proof is folded into M9's Docker-enabled CI job.

---

## M3 — C-4: Event topic/event-name contract ✅

**Problem:** The only consumer, `OrderEventListener` (customer-service), subscribed to topic
`events.order` for an `ORDER_COMPLETED` event type that **no producer emits**.
Sales-order-service publishes `DeliveryCompletedEvent` (and 6 siblings) to
`sales-order.events`. The advertised customer-statistics flow therefore never ran.

**Fix:**
- Added [Topics.java](../../services/common/src/main/java/com/smartstock/common/event/Topics.java)
  in `common` — one canonical constant per producing context, the single source of truth.
- Repointed the sales-order producer constant
  (`KafkaConfig.SALES_ORDER_EVENTS_TOPIC = Topics.SALES_ORDER_EVENTS`) and the customer
  listener (`@KafkaListener(topics = "#{T(...Topics).SALES_ORDER_EVENTS}")`) at the same
  constant, so producer and consumer cannot drift.
- Rewrote `OrderEventListener` to record customer spend on **`DeliveryCompletedEvent`** —
  the order's terminal state — rather than a non-existent type. Recording on completion (not
  creation) avoids double-counting a cancelled order.
- Made the event self-sufficient: added `totalAmount` to `DeliveryCompletedEvent`
  (additive, backward-compatible), populated from `SalesOrder.getTotalAmount()` at publish
  time, so the consumer needs no callback into sales-order-service.
- Made deserialization deterministic: producers send `add.type.headers=false`, so the
  customer consumer pins `spring.json.value.default.type` to a tolerant top-level
  `SalesOrderEventPayload` (ignores unknown fields → one default type serves every event on
  the topic).
- Tests: `OrderEventListenerTest` (records once on completion; ignores other types; ignores
  incomplete payloads; no-op on unknown customer; **contract** assertion the listener binds
  the canonical topic) + `KafkaTopicContractTest` (producer side).

**Verification (run 2026-06-27):**
- `mvn -pl common,sales-order-service,customer-service -am test` → **BUILD SUCCESS**;
  customer 17 tests, sales-order 17 tests, 0 failures.
- Literal `events.order` / type `ORDER_COMPLETED` no longer referenced in code.

**Notes:** redelivery safety (idempotency) + DLQ come in **M6**; this milestone makes the
flow *correct and live*. Rolling `Topics` constants into the remaining services'
`KafkaConfig` is folded into the messaging starter in **M7**.

---

## M4 — C-2: Transactional outbox (no fire-and-forget) ✅

**Problem:** All 7 domain-event publishers sent via `@Async KafkaTemplate.send(...)` wrapped
in a `try/catch` that only logged on failure — the classic dual-write: the DB transaction
commits, then an asynchronous Kafka send happens *outside* it. Broker down / process dies →
the event is **lost forever**, leaving downstream state (and the future ML training record)
permanently divergent with only a log line. (A latent serialization bug was also masked by
the swallow: a bare `ObjectMapper` can't serialize the events' `LocalDateTime` — the old path
would have thrown and been silently swallowed.)

**Fix — shared infrastructure in `common`:**
- `OutboxRecord`, `OutboxRepository` (JDBC, no JPA entity → no `@EntityScan` wiring, and
  Hibernate `ddl-auto: validate` stays satisfied), `OutboxService` (serialize + insert in the
  caller's transaction), `OutboxRelay` (`@Scheduled` drain).
- `OutboxAutoConfiguration` (registered via `AutoConfiguration.imports`), gated by
  `smartstock.outbox.enabled=true` and `@ConditionalOnClass(KafkaTemplate)`, so only producer
  services run the relay. The relay uses its **own `acks=all` + idempotent** String producer
  (max-in-flight=1), so the stored JSON goes out verbatim and reliability config lives in one
  place. Claims rows with `FOR UPDATE SKIP LOCKED` (multi-instance safe), blocks on the broker
  ack, marks `PUBLISHED` only on success, bumps `attempts` and stays `PENDING` on failure →
  at-least-once, no silent loss.
- Added `spring-kafka` (optional) to the `common` POM.

**Fix — adoption (all 7 producers, no partial migration):**
- Rewrote inventory, product, warehouse, supplier, customer, purchase-order, sales-order
  publishers to call `outbox.append(topic, event)` synchronously — **public method signatures
  unchanged**, so the domain services calling them are untouched. Removed `@Async` and the
  swallow.
- Added an `outbox` table Flyway migration to each producer DB (versions: inventory V4,
  product V3, warehouse V4, supplier V3, customer V3, purchase-order V2, sales-order V2).
- Enabled `smartstock.outbox.enabled` in each producer's `application.yml`.

**Verification (run 2026-06-27):**
- `mvn -f services/pom.xml clean test` → **BUILD SUCCESS** (all 16 modules); new
  `OutboxServiceTest` (serialization + keying) and `OutboxRelayTest` (publish/mark, failure
  records + stays pending, empty no-op) green.
- `grep @Async services/**/*EventPublisher.java` → only javadoc mentions; no annotations.
  `KafkaTemplate.send` in publishers → none.
- `check-flyway-versions.sh` → OK.

**Notes:** the relay's at-least-once delivery makes **idempotent consumers (M6)** a hard
requirement, which is the next-priority milestone. Live broker round-trip is covered by the
Testcontainers integration suite in **M9**. Per-service `AsyncConfig` executor beans are now
unused but left in place (harmless) — removal is deferred to the M7 shared-starter cleanup.

---

## M5 — C-3: Optimistic locking on mutable inventory aggregates ✅

**Problem:** No `@Version` / `@Lock` anywhere. `InventoryLevel`, `StockOut`, `InventoryHold`
were read-modify-write under concurrency with no guard, so two concurrent stock-outs /
reservations could lost-update each other — driving stock negative / overselling, the single
most damaging bug class for an inventory platform, invisible at low load.

**Fix:**
- Added a JPA `@Version` column to `InventoryLevel`, `StockOut`, `InventoryHold`, with Flyway
  migration `V5__add_optimistic_lock_version.sql` (`version BIGINT NOT NULL DEFAULT 0`,
  backfilling existing rows). A stale writer now fails with `OptimisticLockException` instead
  of clobbering a concurrent update.
- Added `ConcurrencyRetry` and wrapped the two contention paths named in the register —
  `InventoryService.dispatchStock` (decrement) and `ReservationService.reserve` — so a losing
  writer **re-reads fresh stock and retries** (succeeding if stock remains, or being correctly
  rejected), instead of failing the user request. Each retry runs a fresh transaction via the
  bean's `ObjectProvider` self-proxy (the public method is non-transactional; the renamed
  `*Transactional` method carries `@Transactional`). The other `InventoryLevel` writers
  (receive/adjust/transfer) remain lost-update-safe via `@Version` (a conflict surfaces as an
  error) — retry can be extended to them later; no path is left unprotected.

**Verification (run 2026-06-27):**
- `mvn -pl common,inventory-service -am test` → **BUILD SUCCESS**; 21 inventory unit tests
  incl. new `ConcurrencyRetryTest` (3); existing service tests updated to exercise the
  retry/self-proxy wrapper (real `@Spy ConcurrencyRetry` + mocked self-provider) and pass.
- New `InventoryConcurrencyIntegrationTest` (Testcontainers): 20 parallel dispatches of 10
  against 100 stock — asserts `on_hand == 100 - 10*successes` and never negative. Compiles;
  runs under the `integration-test` profile / M9 Docker CI (excluded from default `mvn test`).
- `check-flyway-versions.sh` → OK.

---

## M6 — H-3: Consumer retry, DLQ, idempotency ✅

**Problem:** `OrderEventListener` processed and `save`d with no dedup key, no error handler,
no DLQ, no retry. Kafka is at-least-once (and the M4 relay can redeliver), so a redelivered
event would **double-count** `customer.recordOrder(amount)`, and a poison message would block
the partition forever.

**Fix — shared infrastructure in `common` (`consumer` package):**
- `IdempotencyService.claim(consumer, eventId)` — `INSERT ... ON CONFLICT DO NOTHING` against a
  `processed_events` ledger; returns true only the first time a (consumer, eventId) pair is
  seen. JDBC, runs in the handler's transaction (rolls back with it → reprocess-safe).
- `ConsumerAutoConfiguration` (registered in `AutoConfiguration.imports`, opt-in via
  `smartstock.consumer.enabled=true`): a `DefaultErrorHandler` with `ExponentialBackOff`
  (1s→×2→10s cap, 30s max) + `DeadLetterPublishingRecoverer` routing exhausted records to
  `<topic>.DLT` instead of wedging the partition. Spring Boot applies this `CommonErrorHandler`
  bean to the listener container factory automatically. Plus the `IdempotencyService` bean.

**Fix — adoption (customer-service, the only consumer today):**
- `processed_events` Flyway migration (`V4`).
- `smartstock.consumer.enabled=true` in `application.yml`.
- `OrderEventListener` now calls `idempotencyService.claim("customer-order-listener", eventId)`
  inside its transaction and skips already-processed events — so a redelivered
  `DeliveryCompletedEvent` records spend exactly once.

**Verification (run 2026-06-27):**
- `mvn -pl common,customer-service -am test` → **BUILD SUCCESS**; common 8 (incl.
  `IdempotencyServiceTest` 3), customer 18 (incl. `OrderEventListenerTest` redelivery-is-skipped).
- `check-flyway-versions.sh` → OK.

**Notes:** the DLT round-trip and live redelivery are exercised by the Testcontainers
integration suite (M9). The M8 analytics capture sink reuses this same idempotency + error
handling.

---

## M9 — H-7: Integration tests in CI ✅

**Problem:** `*IntegrationTest`/`*RepositoryTest` were excluded from `mvn test`, and only 4 of
15 services even had a profile to run them — 24 integration test files that **never ran in any
pipeline**. This is precisely why C-1…C-4 went undetected. Codecov also pointed at a
non-existent root JaCoCo report (L-3).

**Fix:**
- Added a uniform `integration-test` profile to the **parent** pom using `maven-failsafe-plugin`
  (verify phase). Failsafe is independent of each service's surefire excludes — no
  include/exclude precedence ambiguity, no double-run with the unit suite — and the profile is
  inherited by all 15 modules. `mvn verify -Pintegration-test` runs the whole Testcontainers
  suite.
- Reworked [build-test.yml](../../.github/workflows/build-test.yml): the `build` job now also
  runs the **Flyway version guard** (M1) and **`docker compose config`** validation (M2); a new
  **`integration-test` job** runs `mvn -Pintegration-test verify` on a Docker-enabled runner
  (Testcontainers) — the check that would have caught the Critical items. Bumped
  `codecov-action@v3→v4` and let it auto-discover per-module JaCoCo reports (fixes the dead
  root-report path, L-3); dropped the unused service-container Postgres (unit tests are mocked,
  IT use Testcontainers).

**Verification (run 2026-06-27):**
- `mvn -Pintegration-test help:active-profiles` → profile active, inherited from the parent by
  every module.
- `mvn -f services/pom.xml validate` → clean (parent pom change parses across the reactor).
- `docker compose config -q` → OK (the new CI step, validated locally).

**Notes:** the integration suites execute on the Docker-enabled CI runner (the local Docker
daemon is unavailable, K-5). The 4 legacy per-service surefire `integration-test` profiles are
superseded by the parent failsafe profile and are functionally harmless (base surefire still
excludes IT, so no double execution); they can be removed in a later cleanup.

---

## M8 — H-4: Durable analytics event-capture sink ✅

**Problem:** `analytics-service`/`data-export-service` were health-only skeletons; **no service
persisted the event stream** anywhere. Combined with the (now-fixed) lossy/unconsumed events,
there was no durable historical record to ever train models on — and lost history is
unrecoverable, so capture must start *now*, before any analytics logic.

**Fix (analytics-service):**
- `captured_events` + `processed_events` Flyway migration (`V2`).
- `EventCaptureSink` — a `@KafkaListener` subscribing to **all 8 domain topics** (via the
  `Topics` registry) that persists the **raw JSON envelope verbatim** (consumer value
  deserializer switched to `StringDeserializer`), parsing `eventId`/`eventType` best-effort for
  indexing. Never drops an event on a parse error. Idempotent via the shared H-3 ledger and
  hardened by the shared error-handler/DLT (`smartstock.consumer.enabled=true`).
- `EventCaptureRepository` (JDBC, no managed entity → `ddl-auto: validate` safe).
- Added the missing `testcontainers:junit-jupiter` test dep; added a Testcontainers
  `AbstractIntegrationTest` base (real Postgres, Kafka listener stopped) and aligned the
  pre-existing skeleton integration test to it so the context boots end to end.

**Verification (run 2026-06-27):**
- `mvn -pl common,analytics-service -am test` → **BUILD SUCCESS**; analytics unit test green;
  integration tests compile.
- New `EventCaptureSinkIntegrationTest` (Testcontainers): asserts an emitted envelope lands in
  `captured_events` with parsed id/type and verbatim payload, and that a redelivery is captured
  once — runs under M9's Docker CI.
- `check-flyway-versions.sh` → OK.

**Notes:** this is a deliberately minimal "capture everything" sink (no analytics logic), per the
register's guidance to accrue history immediately. It depends on M4 (reliable emission) + M6
(idempotency) + M3 (topic registry), all in place. `data-export-service` can adopt the same sink
pattern later.
