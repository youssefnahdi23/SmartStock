# SmartStock AI — Release 1.0 Checklist (GA)

**Release:** 1.0.0
**Date:** 2026-07-02
**Owner / Role:** Release Manager (Phase 10 — Release Engineering)
**Build target:** Java 21 (Temurin) · Spring Boot 3.3.1 · Spring Cloud 2024.0.3 · Maven ≥ 3.8.1
**Scope:** 16 Maven modules — **8 implemented business services**, API Gateway, shared `common`
library, and **6 skeleton services** (health-only, out of functional scope).

> **Verification basis for this release.** This checklist was produced by **static + structural
> verification** of the repository and by **review of the CI/CD pipeline definitions**. The Release
> Manager environment for Phase 10 has **no local Maven and no Docker daemon**, so build/test/image
> steps were **not re-executed here**. Where a row cites an *executed* result, the evidence is the
> recorded output of the prior phases (Phase 7 QA + Phase 9 DevOps), run on Docker-capable hosts and
> captured in `docs/reviews/` and the git history. Rows that could only be checked by inspection are
> marked accordingly.

> Status legend: ✅ verified · ⚠️ verified-with-caveat (see KNOWN_LIMITATIONS.md) · ⏭️ out of 1.0 scope · ⛔ blocked

---

## 1. Architecture

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 1.1 | Microservices layout (ADR-0001, ADR-0012) | ✅ | 17 module dirs under `services/`: 8 business services + `api-gateway` + `common` (lib) + `load-tests` + 6 skeletons. |
| 1.2 | Root reactor integrity | ✅ (static) | Root `pom.xml` v`1.0.0`, packaging `pom`, `desktop-client` module correctly commented out until present. |
| 1.3 | Clean layering (api → service → domain → infrastructure) | ✅ (recorded) | Phase-7 review: no controller→repository or domain→web/infra leaks across all 8 services. |
| 1.4 | Database-per-service (ADR-0003) | ✅ | Each service owns its `db/migration/*.sql`; 8 dedicated Postgres instances in `docker-compose.yml`. |
| 1.5 | Event-driven backbone (ADR-0002, ADR-0004) | ✅ | 8 canonical Kafka topics in `common.event.Topics`; transactional outbox + idempotency + DLQ in `common`. |
| 1.6 | Cross-cutting via opt-in auto-config | ✅ | `common` ships Spring auto-configurations (security, outbox, idempotency) toggled by `smartstock.*.enabled`. |
| 1.7 | ADRs recorded | ⚠️ | 18 ADR files present; duplicate `ADR-0005` number (jwt-rbac vs event-driven-strategy) — doc-only nit (KL C-3). |

## 2. Tests

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 2.1 | Unit/slice suite green | ✅ (recorded) | Phase-7 review: `mvn -f services/pom.xml clean test` → 209 tests, 0 failures/errors/skips (18 modules). |
| 2.2 | Test inventory present | ✅ (static) | 101 test source files; **454** `@Test` methods; 37 `*IntegrationTest`, 9 `*RepositoryTest`, 3 `*SmokeTest`. |
| 2.3 | Testcontainers integration tier | ✅ (recorded) | Runs under `-Pintegration-test` (failsafe) on a Docker host; stabilized per-service in Phase 7 (§7.3–7.4). |
| 2.4 | Kafka contract tests | ✅ | `KafkaTopicContractTest` per producing service; blocking in both pipelines. |
| 2.5 | Coverage gate wired | ✅ | Per-module JaCoCo `0.20` floor (parent) + aggregate `40%` floor in `ci-cd.yml` (`scripts/check-coverage.sh`). |
| 2.6 | Local re-execution in Phase 10 env | ⚠️ | Not executed — no `mvn` in the release environment (see TEST_REPORT.md §1). |

See **TEST_REPORT.md** for the full breakdown.

## 3. Docker

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 3.1 | Per-service Dockerfiles | ✅ | 15 hardened Dockerfiles (8 services + gateway + 6 skeletons); `common`/`load-tests` need none. |
| 3.2 | Hardened image pattern | ✅ | `eclipse-temurin:21-jre-alpine`, **non-root** user, container-aware JVM, `HEALTHCHECK`, repo-root context. |
| 3.3 | Full-stack Compose | ✅ | `docker-compose.yml` now runs the 8 services **and** the gateway alongside infra (resolves RC1 blocker K-1). |
| 3.4 | Image build/publish | ⚠️ | Dockerfiles validated statically + against JAR globs; live image build not executed here — CI `docker-build` job builds them. |

## 4. Kafka

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 4.1 | Canonical topic registry | ✅ | 8 topics in `Topics.java` (inventory/product/warehouse/supplier/customer/identity/purchase-order/sales-order `.events`). |
| 4.2 | No topic-name string drift | ✅ (recorded) | Every producer/consumer references the constant; contract tests enforce it. |
| 4.3 | Producers & consumers | ✅ | 8 domain event publishers; 7 `@KafkaListener` consumers; analytics `EventCaptureSink` consumes all 8. |
| 4.4 | Reliability primitives | ✅ | Transactional outbox (`OutboxService`/`OutboxRelay`, acks=all, `SKIP LOCKED`), idempotency (`processed_events`), DLQ. |
| 4.5 | Event catalog | ⚠️ | 29 events documented; catalog still references obsolete "Order Service" events (KL C-4). |

## 5. Databases

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 5.1 | Flyway migrations per service | ✅ | **76** `V*__*.sql` migrations across 14 service schemas; forward-only. |
| 5.2 | One Postgres per business service | ✅ | 8 `postgres-*` services + named volumes in `docker-compose.yml` (ports 5432–5439). |
| 5.3 | Migration version hygiene | ✅ | `scripts/check-flyway-versions.sh` + CI `flyway-validation` job (no duplicate/gap versions). |
| 5.4 | Schema ↔ entity alignment | ✅ (recorded) | Phase-7 §7.4 fixed all schema-validation defects (inventory variance, supplier UUID/VARCHAR, warehouse default). |
| 5.5 | Live migration execution | ⚠️ | Executed against Testcontainers Postgres in CI; not re-run in the Phase 10 env (no Docker). |

## 6. OpenAPI

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 6.1 | springdoc on all HTTP services | ✅ | 9 poms depend on `springdoc` (8 services + gateway). |
| 6.2 | Swagger UI / api-docs | ✅ (static) | Served at `/api/v1/swagger-ui.html` (services) per DEPLOYMENT_REPORT.md smoke steps. |
| 6.3 | API catalog documented | ✅ | 15 API docs under `docs/api/` (catalog + 13 service specs + README). |

## 7. Security

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 7.1 | No committed secrets | ✅ (recorded) | Source/YAML high-entropy sweep clean; only labeled test secrets under `src/test`. |
| 7.2 | JWT secret externalized | ✅ | `${JWT_SECRET:...}` env placeholder across all services + gateway; must be supplied at deploy. |
| 7.3 | AuthN/Z: JWT + BCrypt + RBAC | ✅ | Shared JWT validation in `common`; 12 `SecurityConfig` chains; `@PreAuthorize` in 26 files. |
| 7.4 | Edge enforcement | ✅ | API Gateway enforces JWT + rate limiting at `:8080`. |
| 7.5 | CI security scanning | ✅ | `security.yml` (OWASP Dependency-Check, Snyk, GitGuardian) + `ci-cd.yml` (CodeQL, dependency-scan). |
| 7.6 | Dependency-Check severity | ⚠️ | OWASP runs advisory (`|| true`) in `security.yml`; `ci-cd.yml` `dependency-scan` is the enforced path. See SECURITY_REVIEW.md. |

## 8. Coverage

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 8.1 | JaCoCo bound to build | ✅ | `jacoco-maven-plugin` `0.8.12`, agent + report + `check` in `services/pom.xml`. |
| 8.2 | Per-module floor | ✅ | `jacoco.minimum.coverage = 0.20` (line-only, ratcheting); skeletons set to `0.0` (out of scope). |
| 8.3 | Aggregate CI floor | ✅ | `ci-cd.yml` `COVERAGE_MIN=40` enforced by `scripts/check-coverage.sh` over merged unit reports. |
| 8.4 | Honest baseline, ratchet plan | ✅ | Documented ratchet 0.20 → 0.90 target (phase-7 review §6). Coverage is uneven — see KNOWN_LIMITATIONS L-2. |

## 9. CI/CD & Deployment

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 9.1 | Production pipeline | ✅ | `ci-cd.yml`: 13 gated stages incl. flyway-validation, helm-validate, unit/integration/contract tests, coverage-gate, CodeQL, dependency-scan, docker-build, deployment-validation → `pipeline-gate`. |
| 9.2 | QA automation pipeline | ✅ | `qa-automation.yml`: blocking gate = unit + coverage + kafka-contract; Testcontainers suites advisory. |
| 9.3 | Helm chart + K8s | ✅ (recorded) | `helm/smartstock` umbrella chart; lint + kubeconform clean; installed to kind (Phase 9, DEPLOYMENT_REPORT.md §8). |
| 9.4 | Backup / DR | ✅ | Daily `pg_dump` CronJob + `scripts/db-restore.sh` + `docs/deployment/DISASTER_RECOVERY.md`. |
| 9.5 | Release automation | ✅ | Release Please (`.release-please-manifest.json` → `1.0.0`), `release.yml`. |

---

## Go / No-Go

**Recommendation: GO for 1.0.0 GA** of the 8 implemented business services + API Gateway, deployed
via `docker-compose.yml` (single-host) or the `helm/smartstock` chart (Kubernetes), with a strong
`JWT_SECRET` supplied.

**Conditions carried into GA (non-blocking, tracked in KNOWN_LIMITATIONS.md):**
- Publish service images to a registry (GHCR) so the Helm app pods reach `Ready` (K-3) and the CI
  `docker-build`/`deployment-validation` chain runs end-to-end.
- Continue the coverage ratchet from the honest `0.20`/`40%` floors toward the `0.90` target.
- 6 skeleton services remain **out of 1.0 functional scope** (health-only).

**Sign-off:** Release Manager — 2026-07-02. See TEST_REPORT.md, DEPLOYMENT_REPORT.md,
KNOWN_LIMITATIONS.md, RELEASE_NOTES.md, and SECURITY_REVIEW.md for detail.
