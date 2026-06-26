# SmartStock AI — Release Candidate 1 (RC1) Checklist

**Release:** 1.0.0-rc.1
**Date:** 2026-06-26
**Owner:** Release Engineering
**Build target:** Java 21 (Temurin) · Spring Boot 3.3.1 · Maven 3.9.6
**Scope:** 16 Maven modules — 8 implemented business services, API Gateway, shared `common` library, and 6 skeleton services (not feature-complete).

> Status legend: ✅ verified · ⚠️ verified-with-caveat (see KNOWN_LIMITATIONS.md) · ⛔ blocked · ⏭️ out of RC1 scope

---

## 1. Build & Dependencies

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 1.1 | Root reactor validates | ✅ | `mvn validate` (root) succeeds; `desktop-client` correctly disabled until present. |
| 1.2 | Full reactor compiles | ✅ | `mvn -f services/pom.xml clean test` → exit 0 across 16 modules. |
| 1.3 | All modules package | ✅ | `mvn -f services/pom.xml package -DskipTests` → JARs for all service modules. |
| 1.4 | Java/Maven version enforced | ✅ | `maven-enforcer-plugin` requires Java 21, Maven ≥ 3.8.1. |
| 1.5 | Dependency versions centralized | ✅ | Versions managed in `services/pom.xml` (`<properties>` + BOM imports). |
| 1.6 | No dependency conflicts breaking build | ✅ | Reactor resolves; Spring Boot parent + Cloud/OTel/Testcontainers BOMs aligned. |
| 1.7 | MapStruct Beta + missing lombok-binding | ⚠️ | `1.6.0.Beta1` in use; documented (KNOWN_LIMITATIONS L-1) — not changed to protect green build. |

## 2. Configuration Consistency

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 2.1 | Canonical port scheme (800x) | ✅ | identity 8001 … sales-order 8008, skeletons 8009–8014, gateway 8080. |
| 2.2 | `.env.example` matches service defaults | ✅ | Re-aligned 808x→800x; added purchase/sales-order; skeleton ports unique. |
| 2.3 | Prometheus scrape targets correct | ✅ | `infrastructure/prometheus.yml` rewritten: correct ports + `/api/v1/actuator/prometheus` path + all services. |
| 2.4 | Skeleton service configs standardized | ✅ | Env-driven datasource/Kafka, unique ports, JSON logging, dead Keycloak issuer removed. |
| 2.5 | All config via environment variables | ✅ | `${VAR:-default}` indirection in all `application.yml`, compose, and Dockerfiles. |
| 2.6 | Logging format consistent (JSON) | ✅ | All 14 services + gateway emit structured JSON log lines. |

## 3. Security

| # | Item | Status | Evidence |
|---|------|--------|----------|
| 3.1 | No high-entropy secrets committed | ✅ | Sweep clean; only labeled test secrets under `src/test`. |
| 3.2 | JWT secret externalized | ✅ | `${JWT_SECRET:<JWT_SECRET>}` placeholder; must be supplied via env/secret store. |
| 3.3 | No plaintext creds in compose | ✅ | `docker-compose.yml` + legacy file use `${VAR:-default}` indirection. |
| 3.4 | CI secret scanning | ✅ | GitGuardian + Snyk + OWASP Dependency-Check workflows present. |
| 3.5 | Per-service `SecurityConfig` + JWT filter | ✅ | Constructor-injected, stateless; gateway enforces JWT at the edge. |
| 3.6 | See SECURITY_REVIEW.md | ✅ | Full findings documented. |

## 4. Per-Service Production Readiness (8 implemented services + gateway)

| Service | Port | Starts¹ | Infra wired | Health | Metrics | Docker | Env-only | Tests | OpenAPI |
|---------|------|---------|-------------|--------|---------|--------|----------|-------|---------|
| identity | 8001 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| product | 8002 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| inventory | 8003 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| warehouse | 8004 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| supplier | 8005 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| customer | 8006 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| purchase-order | 8007 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| sales-order | 8008 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| api-gateway | 8080 | ⚠️ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | n/a |

¹ ⚠️ = startup **statically verified** (config, datasource, Flyway, actuator). Live boot requires the infrastructure stack and was not executed because the Docker daemon is unavailable in this environment — see DEPLOYMENT_REPORT.md §"Runtime verification".

## 5. Skeleton Services (NOT in RC1 functional scope)

| Service | Port | Build | Docker | Health | Status |
|---------|------|-------|--------|--------|--------|
| audit | 8009 | ✅ | ✅ (standardized) | ✅ | ⏭️ skeleton |
| notification | 8010 | ✅ | ✅ (standardized) | ✅ | ⏭️ skeleton |
| reporting | 8011 | ✅ | ✅ (standardized) | ✅ | ⏭️ skeleton |
| analytics | 8012 | ✅ | ✅ (standardized) | ✅ | ⏭️ skeleton |
| data-export | 8013 | ✅ | ✅ (standardized) | ✅ | ⏭️ skeleton |
| order (legacy) | 8014 | ✅ | ✅ (standardized) | ✅ | ⏭️ superseded by purchase/sales-order |

## 6. Infrastructure & Observability

| # | Item | Status |
|---|------|--------|
| 6.1 | PostgreSQL — per-service DBs (8 implemented) in `docker-compose.yml` | ✅ (added supplier/customer/purchase-order/sales-order) |
| 6.2 | Redis (cache/session) | ✅ |
| 6.3 | Kafka + Zookeeper | ✅ |
| 6.4 | Prometheus scrape config | ✅ |
| 6.5 | Grafana provisioning (Prometheus/Loki/Tempo datasources) | ✅ |
| 6.6 | Loki (logs) / Tempo (traces) | ✅ |
| 6.7 | Full multi-service orchestration compose | ⚠️ (infra+gateway in `docker-compose.yml`; per-service app stack is a documented follow-up) |

## 7. CI/CD

| # | Item | Status |
|---|------|--------|
| 7.1 | Build & Test workflow | ✅ |
| 7.2 | Code Quality (Spotless/Sonar/OWASP) | ⚠️ (Spotless `check || apply` masks failures; pinned action versions dated) |
| 7.3 | Security (Snyk/GitGuardian/OWASP) | ✅ |
| 7.4 | Release Please | ✅ |

## 8. Documentation

| # | Item | Status |
|---|------|--------|
| 8.1 | RELEASE_CHECKLIST.md / DEPLOYMENT_REPORT.md / TEST_REPORT.md / SECURITY_REVIEW.md / KNOWN_LIMITATIONS.md | ✅ (this set) |
| 8.2 | CHANGELOG updated for rc.1 | ✅ |
| 8.3 | ADRs / API / DB / Event specs | ✅ (pre-existing under `docs/`) |

---

## Go / No-Go

**Recommendation: GO for functional testing** of the 8 implemented services + API Gateway, against the `docker-compose.yml` infrastructure stack with `JWT_SECRET` supplied.

**Blocking before GA (1.0.0):** live runtime smoke test on a Docker host (see DEPLOYMENT_REPORT.md), full-stack orchestration compose, and resolution of items in KNOWN_LIMITATIONS.md marked *GA-blocker*.
