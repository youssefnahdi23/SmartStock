# SmartStock AI — Known Limitations (RC1)

**Release:** 1.0.0-rc.1 · **Date:** 2026-06-26

This is the honest list of what is **not** done or **not** verified in RC1, with
severity for GA planning. *GA-blocker* items must be resolved before 1.0.0 GA.

---

## Functional scope

| ID | Severity | Limitation |
|----|----------|-----------|
| K-1 | **GA-blocker** | **No single full-stack orchestration compose.** `docker-compose.yml` provisions infrastructure (8 per-service DBs, Redis, Kafka, Zookeeper, Prometheus, Grafana, Loki, Tempo, MinIO) **plus the API Gateway**. The 8 implemented application services are built (Maven) and containerized (standardized Dockerfiles) but are not yet wired into a compose file; run them from JARs/images per DEPLOYMENT_REPORT.md §4. A full multi-service compose is the recommended next deliverable. |
| K-2 | Info | **6 skeleton services are not feature-complete:** `audit` (8009), `notification` (8010), `reporting` (8011), `analytics` (8012), `data-export` (8013), `order` (8014). They expose only a health endpoint + placeholder tests. In RC1 they were standardized (ports, env-driven config, JSON logging, hardened Dockerfile) but remain **out of functional scope**. |
| K-3 | Low | **`order-service` is superseded** by `purchase-order-service` + `sales-order-service` (ADR/M-3). It still occupies a module, port, and DB allocation. Recommend removing the module once confirmed unused. |
| K-4 | Info | **`desktop-client` (JavaFX, ADR-0007) is not present.** Its reactor module is intentionally disabled in the root `pom.xml`; re-enable when the directory and POM exist. |

## Runtime verification

| ID | Severity | Limitation |
|----|----------|-----------|
| K-5 | **GA-blocker** | **Live runtime not exercised.** The Docker daemon is unavailable in the release environment, so container builds, service↔infra connectivity, Flyway execution against a live DB, and Prometheus scrape success were **not executed** — only statically verified. Functional testers must close these on a Docker-capable host. |
| K-6 | Medium | **Integration tests not run by default.** `*IntegrationTest` / `*RepositoryTest` (Testcontainers) are excluded from offline `mvn test`. They must run in a Docker-enabled CI job to gate GA. |

## Build / dependencies

| ID | Severity | Limitation |
|----|----------|-----------|
| L-1 | Medium | **MapStruct `1.6.0.Beta1`** is pinned in the parent (a pre-release on the production path). Pin a stable `1.6.x` release and add `org.projectlombok:lombok-mapstruct-binding` to the annotation-processor paths. Deferred at RC1 to avoid destabilizing a green build; verify generated mappers after the bump. |
| L-2 | Low | **Coverage is uneven** and **not aggregated.** CI uploads a root `target/site/jacoco/jacoco.xml` that does not exist (reports are per-module). Add a JaCoCo aggregate module or fix the Codecov path. |
| L-3 | Low | **Wildcard imports** remain in a few controllers/entities; prefer explicit imports per the style guide. |

## CI / CD

| ID | Severity | Limitation |
|----|----------|-----------|
| C-1 | Low | **Code Quality workflow masks failures:** `mvn spotless:check || mvn spotless:apply` always succeeds. Make `spotless:check` a hard gate. |
| C-2 | Low | **Dated GitHub Action versions** (`actions/upload-artifact@v3`, `codecov-action@v3`, `github/codeql-action@v2`, `actions/cache@v3`). Refresh to current majors. |
| C-3 | Low | `build-test.yml` provisions a Postgres service but the default `mvn test` excludes integration tests — add an explicit integration-test job that uses it. |

## Configuration

| ID | Severity | Limitation |
|----|----------|-----------|
| G-1 | Resolved (note) | Two port schemes (800x vs 808x) previously diverged across `application.yml`, `.env.example`, and `prometheus.yml`. **Unified to 800x** in RC1. The 800x scheme is now canonical across services, Dockerfiles, compose, env, and Prometheus. |
| G-2 | Low | **`docker-compose.services.yml` is deprecated** (shared DB violates database-per-service; build context contradicts canonical Dockerfiles; container names collide). Retained only so `Makefile.services` targets do not break; migrate those targets to `docker-compose.yml`, then remove. |
| G-3 | Low | **Skeleton DBs not in compose.** Only the 8 implemented services have Postgres instances in `docker-compose.yml`; skeleton DBs (audit/notification/reporting/analytics/data-export/order) are intentionally omitted until those services are implemented. |

---

## Summary

RC1 is **ready for functional testing** of the 8 implemented services + API Gateway.
The two GA-blockers are **K-1** (full-stack compose) and **K-5** (live runtime
verification on a Docker host). Everything else is tracked, low-risk, or intentional.
