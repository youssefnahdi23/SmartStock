# Full-Stack Runtime Verification

Procedure to bring up the entire SmartStock platform with one command and verify it
boots, connects, and serves traffic (closes debt **C-5**, depends on the full-stack
compose from **M-5**).

## Prerequisites

- Docker Engine + Compose v2 (`docker compose version`).
- JDK 21 + Maven (build the JARs the service images copy in).
- A `.env` (copy `.env.example`) — at minimum set `JWT_SECRET` and `DB_PASSWORD`.

## One-command full stack

```bash
# 1. Build all service JARs (images COPY services/<svc>/target/*.jar; root build context)
mvn -f services/pom.xml clean package -DskipTests

# 2. Bring up infra + 8 databases + Kafka + the 8 application services + gateway
docker compose up -d --build

# 3. Watch health
docker compose ps
```

`docker-compose.yml` is now the single source of truth. Each application service
`depends_on` its own Postgres (`postgres-<svc>`) and Kafka with
`condition: service_healthy`, so Compose starts them only once their backing stores are
ready. The deprecated `docker-compose.services.yml` has been removed; `Makefile.services`
targets (`make -f Makefile.services services-up`, etc.) now drive the unified file.

## Canonical ports

| Service | Container | Port | Health |
|---------|-----------|------|--------|
| api-gateway | smartstock-api-gateway | 8080 | `/actuator/health` |
| identity-service | smartstock-identity-service | 8001 | `/api/v1/actuator/health` |
| product-service | smartstock-product-service | 8002 | `/api/v1/actuator/health` |
| inventory-service | smartstock-inventory-service | 8003 | `/api/v1/actuator/health` |
| warehouse-service | smartstock-warehouse-service | 8004 | `/api/v1/actuator/health` |
| supplier-service | smartstock-supplier-service | 8005 | `/api/v1/actuator/health` |
| customer-service | smartstock-customer-service | 8006 | `/api/v1/actuator/health` |
| purchase-order-service | smartstock-purchase-order-service | 8007 | `/api/v1/actuator/health` |
| sales-order-service | smartstock-sales-order-service | 8008 | `/api/v1/actuator/health` |

Each service's `docker` Spring profile (`application-docker.yml`) was corrected during
M2: datasource host/name now resolve to the matching `postgres-<svc>` /
`smartstock_<svc>` (several previously pointed at a non-existent `postgres` host and stale
`*_db` names — a guaranteed connection failure), and all values remain env-overridable so
Compose is authoritative.

## Smoke test

```bash
bash scripts/smoke-test.sh                 # Phase 1: readiness gate across all 9 endpoints
RUN_HAPPY_PATH=1 SMOKE_PASSWORD=… bash scripts/smoke-test.sh   # + business happy path
```

Phase 1 polls every health endpoint until `status:UP` (or fails). Phase 2 exercises the
documented end-to-end flow — create product → stock-in → sales order → deliver — and
asserts inventory decremented and customer statistics updated. Phase 2's event-driven
assertions become meaningful only after **M3** (real event wiring) and **M6**
(idempotent consumers); until then Phase 1 is the gating check.

## CI

The `docker compose config` parse/validation is wired into CI by **M9**, and the
integration suite (Testcontainers) runs in a Docker-enabled job there. Local validation:

```bash
docker compose config -q && echo OK     # parses + resolves env, no daemon required
```

## Current status (2026-06-27)

The Docker daemon is unavailable in this environment (KNOWN_LIMITATIONS K-5), so the
compose file and smoke script are **authored and statically validated**
(`docker compose config` passes; `bash -n scripts/smoke-test.sh` passes). Live
`docker compose up` + `smoke-test.sh` execution is the one remaining step, to be run on
any Docker host using the commands above.
