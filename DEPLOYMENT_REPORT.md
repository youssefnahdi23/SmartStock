# SmartStock AI — Deployment Report (RC1)

**Release:** 1.0.0-rc.1 · **Date:** 2026-06-26 · **Owner:** Release Engineering

This report describes how to deploy SmartStock AI for functional testing, the
verified state of each deployment artifact, and the gaps that remain.

---

## 1. Topology (canonical)

```
                          ┌────────────────────┐
   client ──HTTP:8080──▶  │   api-gateway      │  (JWT enforced, rate-limited)
                          └─────────┬──────────┘
                                    │ routes /api/v1/**
   ┌──────────────┬─────────────────┼─────────────────┬──────────────┐
   ▼              ▼                  ▼                 ▼              ▼
 identity:8001  product:8002   inventory:8003   warehouse:8004 ... sales-order:8008
   │              │                  │                 │
   ▼              ▼                  ▼                 ▼
 pg:5432       pg:5433            pg:5434           pg:5435  ... (one DB per service)

 Shared infra:  Kafka:9092 · Zookeeper:2181 · Redis:6379 · MinIO:9000
 Observability: Prometheus:9090 · Grafana:3000 · Loki:3100 · Tempo:3200/4317/4318
```

### Port allocation (canonical 800x)

| Service | App port | Host DB port | Service | App port | Host DB port |
|---------|----------|--------------|---------|----------|--------------|
| identity | 8001 | 5432 | customer | 8006 | 5437 |
| product | 8002 | 5433 | purchase-order | 8007 | 5438 |
| inventory | 8003 | 5434 | sales-order | 8008 | 5439 |
| warehouse | 8004 | 5435 | api-gateway | 8080 | — |
| supplier | 8005 | 5436 | | | |

Skeleton services (not in functional scope): audit 8009, notification 8010,
reporting 8011, analytics 8012, data-export 8013, order 8014.

> All services run under `server.servlet.context-path: /api/v1`, so actuator is
> served at **`/api/v1/actuator/*`**. The reactive gateway has no context-path and
> serves actuator at **`/actuator/*`**. Prometheus and the Dockerfile healthchecks
> are aligned to this.

---

## 2. Build artifacts — VERIFIED

| Gate | Result | Evidence |
|------|--------|----------|
| `mvn -f services/pom.xml clean test` | ✅ exit 0 | 169 tests, 0 failures/errors/skipped (67 surefire reports) |
| `mvn -f services/pom.xml package -DskipTests` | ✅ exit 0 | 15 service JARs + `smartstock-common-1.0.0.jar` |
| JAR ↔ Dockerfile `COPY` glob match | ✅ | every `smartstock-<svc>-1.0.0.jar` matches `smartstock-<svc>-*.jar` |

Produced JARs (all `1.0.0`): identity, product, inventory, warehouse, supplier,
customer, purchase-order, sales-order, audit, notification, reporting, analytics,
data-export, order, api-gateway, common.

---

## 3. Docker images

All 15 service Dockerfiles (8 implemented + gateway + 6 skeletons) now follow one
**hardened, pre-built-JAR** pattern:

- Base `eclipse-temurin:21-jre-alpine`
- **Non-root** runtime user per service
- Container-aware JVM: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
- `HEALTHCHECK` on `/api/v1/actuator/health` (gateway: `/actuator/health`), correct per-service port
- Build context = **repository root**

> RC1 change: the 6 skeleton Dockerfiles previously used a broken multi-stage
> build (`COPY parent-pom.xml ../pom.xml 2>/dev/null || true` is invalid Docker
> syntax; `EXPOSE 8080` collided across services). They were rewritten to the
> standard pattern.

**Build command (from repo root, after `mvn package`):**
```bash
docker build -f services/identity-service/Dockerfile -t smartstock/identity-service:1.0.0 .
# …repeat per service; context MUST be the repo root (.)
```

⚠️ **Not executed:** image builds were not run because the Docker daemon is
unavailable in the release environment. Dockerfiles were validated statically and
against the produced JAR names.

---

## 4. Deploy for functional testing

### Step 1 — Configuration
```bash
cp .env.example .env
# REQUIRED: set a strong JWT_SECRET (>= 64 bytes for HS512):
#   openssl rand -base64 64
# Override DB_PASSWORD / REDIS_PASSWORD / GRAFANA_PASSWORD for any shared env.
```

### Step 2 — Infrastructure + databases + gateway
```bash
docker compose up -d          # postgres x8, redis, kafka, zookeeper,
                              # prometheus, grafana, loki, tempo, minio, api-gateway
docker compose ps             # confirm health
```

### Step 3 — Application services
Until a full multi-service compose lands (see KNOWN_LIMITATIONS K-1), run the 8
services from the built JARs or images, pointing at the infra above, e.g.:
```bash
SERVER_PORT=8001 DB_HOST=localhost DB_PORT=5432 DB_NAME=smartstock_identity \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 JWT_SECRET="$JWT_SECRET" \
java -jar services/identity-service/target/smartstock-identity-service-1.0.0.jar
```

### Step 4 — Smoke checks
```bash
curl -fsS http://localhost:8001/api/v1/actuator/health   # {"status":"UP"}
curl -fsS http://localhost:8001/api/v1/actuator/prometheus | head
curl -fsS http://localhost:8001/api/v1/swagger-ui.html -o /dev/null && echo "OpenAPI UI OK"
curl -fsS http://localhost:8080/actuator/health           # gateway
open http://localhost:9090/targets                        # Prometheus targets UP
open http://localhost:3000                                # Grafana (Prometheus datasource)
```

---

## 5. Runtime verification status

| Check | Method | Result |
|-------|--------|--------|
| Compiles & tests | Maven (executed) | ✅ |
| Packages | Maven (executed) | ✅ |
| Config/env indirection | static review (executed) | ✅ |
| Flyway migration naming | static review | ✅ (each service has ≥1 `V*__*.sql`) |
| Live container boot | Docker run | ⛔ not executed (no daemon) |
| Service↔infra connectivity | Docker run | ⛔ not executed (no daemon) |
| Prometheus scrape success | Docker run | ⛔ not executed (no daemon) |

**Functional testers must perform Steps 1–4 on a Docker-capable host** to close the
runtime gates before GA sign-off.

---

## 6. Rollback

Stateless services — rollback = redeploy previous image tag. Database changes are
Flyway-versioned and forward-only; no destructive migrations are present in RC1.
`docker compose down` (without `-v`) preserves named volumes.
