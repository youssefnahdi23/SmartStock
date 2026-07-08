# SmartStock AI — Known Limitations (Release 1.0)

**Release:** 1.0.0 · **Date:** 2026-07-02 · **Owner:** Release Manager (Phase 10)

The honest list of what is **not** done, **not** re-verified in Phase 10, or intentionally
out of scope for 1.0, with severity for post-GA planning. Two RC1 GA-blockers (full-stack
Compose and Helm/K8s deployment) are now **resolved** — recorded at the bottom.

> Severity: **GA-caveat** = accept for 1.0 with a documented workaround · **Medium/Low** = tracked
> debt · **Info** = intentional scope decision.

---

## Functional scope

| ID | Severity | Limitation |
|----|----------|-----------|
| K-1 | Info | **6 skeleton services are not feature-complete:** `audit` (8009), `notification` (8010), `reporting` (8011), `analytics` (8012), `data-export` (8013), `order` (8014). They expose a health endpoint + placeholder tests only. Standardized (ports, env config, JSON logging, hardened Dockerfile) but **out of 1.0 functional scope**. |
| K-2 | Low | **`order-service` is superseded** by `purchase-order-service` + `sales-order-service` (M-3). It still occupies a module, port, and DB allocation. Remove once confirmed unused. |
| K-3 | GA-caveat | **Service images are not published to a registry** in this environment. On Kubernetes the app Deployments schedule and pass admission but hit `ImagePullBackOff` until images are pushed (CI → GHCR). Not a chart defect; blocks the *live app-pod `Ready`* step only. Postgres StatefulSets + PVs verified `Bound`/`Running` on kind (DEPLOYMENT_REPORT.md §8.1). |
| K-4 | Medium | **Testcontainers suites are advisory in `qa-automation.yml`.** They are **blocking in `ci-cd.yml`**, but per-service test-JWT config is inconsistent (`identity` matches `TestJwtTokenFactory.TEST_SECRET`; several services differ or lack `application-test.yml`), and event-assertion tests need a CI Kafka broker. Controller ITs that never attach a JWT correctly 403. Re-promote per service once auth wiring + a CI Kafka broker land. |
| K-5 | Info | **`desktop-client` (JavaFX, ADR-0007) is not present.** Its reactor module is intentionally disabled in the root `pom.xml`; re-enable when the directory and POM exist. |

## Runtime verification (Phase 10 environment)

| ID | Severity | Limitation |
|----|----------|-----------|
| K-6 | GA-caveat | **The Phase 10 release environment has no local Maven and no Docker daemon.** Build, test, image build, and live `docker compose up` were **not re-executed by the Release Manager**. All such results in this release set are (a) **static/structural inspection** or (b) **recorded executed evidence** from Phase 7 (QA, Docker host) and Phase 9 (DevOps, kind cluster). Deployers must run the smoke steps (DEPLOYMENT_REPORT.md §4) on their target host before production sign-off. |
| K-7 | Low | **RoleController endpoints partly unimplemented** (getRoleByName/updateRole/deleteRole/assignPermission return 404). Building them is new functionality, deferred post-1.0 (phase-7 §7.3). |

## Build / dependencies

| ID | Severity | Limitation |
|----|----------|-----------|
| L-1 | Medium | **MapStruct `1.6.0.Beta1`** is pinned in the parent (a pre-release on the production path). Pin a stable `1.6.x` and add `org.projectlombok:lombok-mapstruct-binding` to the annotation-processor paths; verify generated mappers after the bump. |
| L-2 | Low | **Coverage is uneven and not aggregated.** Per-module JaCoCo floor is an honest ratcheting `0.20`; the CI aggregate floor is `40%`. Line coverage spans ~21%–91%; branch coverage is single-digit on most modules. Add a JaCoCo aggregate module and advance the ratchet toward the `0.90` target. |
| L-3 | Medium | **Spring Cloud `2024.0.3` targets Boot `3.4.x`; project is on Boot `3.3.1`** (minor skew). Pin a matching train in a maintenance release. |
| L-4 | Low | **Wildcard imports** remain in several main sources (permitted by the style guide; convention nit only). |

## CI / CD

| ID | Severity | Limitation |
|----|----------|-----------|
| C-1 | Low | **`code-quality.yml` masks failures:** `spotless:check || spotless:apply` always succeeds. Make `spotless:check` a hard gate. |
| C-2 | Low | **OWASP Dependency-Check is advisory** in `security.yml` (`|| true`). The enforced dependency path is `ci-cd.yml` `dependency-scan`; consider failing `security.yml` at a chosen CVSS too. |
| C-3 | Low | **Some GitHub Action versions are dated** in `security.yml` (`upload-artifact@v3`, `codeql-action@v2`). `ci-cd.yml`/`qa-automation.yml` already use `@v4`. Refresh the older workflow. |

## Documentation

| ID | Severity | Limitation |
|----|----------|-----------|
| D-1 | Low | **Duplicate ADR number 0005** (`jwt-rbac-authentication` vs `Event-Driven-Architecture-Strategy`). Renumber the latter (e.g. 0018) and add it to the ADR index. |
| D-2 | Low | **Event catalog drift:** `docs/events/event-catalog.md` still references obsolete "Order Service / `OrderCreated`" events instead of `PurchaseOrder*` / `SalesOrder*`. |
| D-3 | Low | **Root-level doc sprawl** (20+ top-level `*.md`). Consolidate under `docs/` post-1.0. |

## Reliability / performance (deferred by design)

| ID | Severity | Limitation |
|----|----------|-----------|
| R-1 | Medium | **Correlation/trace IDs not propagated end-to-end.** Each `DomainEvent` gets a fresh UUID; cross-service correlation needs request-scoped MDC propagation (acknowledged TODO in `common`). |
| R-2 | Low | **Saga/compensation not implemented** (ADR-0015 deferred); current cross-service consistency relies on the outbox + idempotent consumers. |
| R-3 | Low | **Load tests never run in a gate** and **Redis caching (ADR-0011) not yet applied** to read paths; no live performance baseline. |

---

## Resolved since RC1 (for the record)

| Was | Now |
|-----|-----|
| **RC1 K-1** — no single full-stack orchestration Compose | ✅ **Resolved.** `docker-compose.yml` runs the 8 services + gateway + full infra. |
| **RC1 K-5** — no deployment layer / Helm / K8s | ✅ **Resolved (Phase 9).** `helm/smartstock` umbrella chart (lint + kubeconform clean), installed to kind; PVs `Bound`, StatefulSets `Running` under restricted PSS. |
| **RC1 CR-1..4** — un-buildable QA tier + unachievable 90% gate | ✅ **Resolved (Phase 7).** Reactor green (209 tests); honest ratcheting coverage floor. |
| Port-scheme divergence (800x vs 808x) | ✅ **Resolved.** Unified to canonical 800x. |
| Schema/query/mapping IT defects (BUG-1/2/6, inventory/supplier/warehouse) | ✅ **Fixed (Phase 7 §7.3–7.4).** |

---

## Summary

**Release 1.0 is GA-ready** for the 8 implemented services + API Gateway. The remaining items are
**caveats and tracked debt**, not blockers: publish images to a registry (K-3), close the local
runtime verification on the deployer's host (K-6), re-promote the advisory Testcontainers suites
(K-4), and continue the coverage ratchet (L-2). The 6 skeleton services are intentionally out of
scope.
