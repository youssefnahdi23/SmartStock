# Full-Stack Docker Compose — SmartStock AI

> Stabilization Milestone 2 · One-command local runtime for the complete SmartStock platform.

---

## Quick Start

```bash
# 1. Generate a JWT secret (required)
openssl rand -base64 64

# 2. Copy and fill the env file
cp .env.example .env
# Edit .env — paste the generated secret as JWT_SECRET

# 3. Build all service JARs
mvn -f services/pom.xml clean package -DskipTests

# 4. Start the full stack
docker compose up -d --build

# 5. Verify
bash scripts/smoke-test.sh
```

Everything runs on a single `docker compose up` command. No service configuration is hardcoded — all values come from the `.env` file.

---

## Architecture

```
                          ┌─────────────┐
                          │  API Gateway │  :8080
                          │  (SCG + CB)  │
                          └──────┬───────┘
               ┌─────────────────┼──────────────────┐
               ▼                 ▼                  ▼
        :8001–8008         :6379 Redis        :9092 Kafka
     8 Business            (rate limiter      (event bus,
       Services            / sessions)        Zookeeper)
               │
        ┌──────┴────────────────────┐
        │  8 × PostgreSQL :5432–5439 │
        └────────────────────────────┘

Observability:  Prometheus :9090  Grafana :3000  Loki :3100  Tempo :3200
Admin:          pgAdmin :5050     Mailpit :8025   MinIO :9000/:9001
```

---

## Service Port Map

| Container                    | Host Port | Purpose                  |
|------------------------------|-----------|--------------------------|
| `smartstock-api-gateway`     | 8080      | Single entry point       |
| `smartstock-identity-service`| 8001      | Auth / users / roles     |
| `smartstock-product-service` | 8002      | Product catalogue        |
| `smartstock-inventory-service`| 8003     | Stock levels             |
| `smartstock-warehouse-service`| 8004     | Warehouse management     |
| `smartstock-supplier-service`| 8005      | Supplier management      |
| `smartstock-customer-service`| 8006      | Customer management      |
| `smartstock-purchase-order-service` | 8007 | Purchase orders       |
| `smartstock-sales-order-service`    | 8008 | Sales orders          |
| `smartstock-postgres-identity`   | 5432  | Identity DB              |
| `smartstock-postgres-product`    | 5433  | Product DB               |
| `smartstock-postgres-inventory`  | 5434  | Inventory DB             |
| `smartstock-postgres-warehouse`  | 5435  | Warehouse DB             |
| `smartstock-postgres-supplier`   | 5436  | Supplier DB              |
| `smartstock-postgres-customer`   | 5437  | Customer DB              |
| `smartstock-postgres-purchase-order` | 5438 | Purchase Order DB    |
| `smartstock-postgres-sales-order`    | 5439 | Sales Order DB       |
| `smartstock-redis`           | 6379      | Cache / sessions         |
| `smartstock-kafka`           | 9092      | Kafka (external)         |
| `smartstock-kafka`           | 29092     | Kafka (internal/Docker)  |
| `smartstock-zookeeper`       | 2181      | ZooKeeper                |
| `smartstock-rabbitmq`        | 5672/15672| RabbitMQ + UI            |
| `smartstock-minio`           | 9000/9001 | Object storage + console |
| `smartstock-prometheus`      | 9090      | Metrics                  |
| `smartstock-grafana`         | 3000      | Dashboards               |
| `smartstock-loki`            | 3100      | Log aggregation          |
| `smartstock-tempo`           | 3200/4317/4318 | Tracing           |
| `smartstock-pgadmin`         | 5050      | PostgreSQL admin UI      |
| `smartstock-mailpit`         | 1025/8025 | SMTP sink + web UI       |

---

## Environment Variables

All configuration is read from `.env`. Copy `.env.example` and customize:

```bash
cp .env.example .env
```

### Required

| Variable       | Description                                      |
|----------------|--------------------------------------------------|
| `JWT_SECRET`   | HS512 signing key — **minimum 64 characters**. Generate with `openssl rand -base64 64`. `docker compose up` will fail if this is missing or the placeholder value. |

### Key Optional Overrides

| Variable           | Default          | Description                        |
|--------------------|------------------|------------------------------------|
| `DB_USER`          | `smartstock`     | PostgreSQL username (all DBs)      |
| `DB_PASSWORD`      | `smartstock123`  | PostgreSQL password                |
| `REDIS_PASSWORD`   | `smartstock123`  | Redis AUTH password                |
| `GRAFANA_USER`     | `admin`          | Grafana admin username             |
| `GRAFANA_PASSWORD` | `admin123`       | Grafana admin password             |
| `PGADMIN_EMAIL`    | `admin@smartstock.dev` | pgAdmin login email          |
| `PGADMIN_PASSWORD` | `admin123`       | pgAdmin login password             |
| `MINIO_USER`       | `minioadmin`     | MinIO root user                    |
| `MINIO_PASSWORD`   | `minioadmin`     | MinIO root password                |
| `LOG_LEVEL`        | `INFO`           | Log verbosity for infra containers |
| `TZ`               | `UTC`            | Timezone                           |
| `RATE_LIMIT_REPLENISH` | `100`      | Gateway rate-limiter tokens/s      |
| `RATE_LIMIT_BURST` | `200`            | Gateway burst capacity             |

---

## Dependency & Startup Order

```
Zookeeper (healthy)
    └─► Kafka (healthy)
            └─► identity-service, product-service, … (each waits for its DB + Kafka)

PostgreSQL containers (healthy per-DB)
    └─► Corresponding app service

Redis (healthy)
    └─► api-gateway

api-gateway starts after Redis; circuit breakers absorb the window before
downstream services become available.
```

Spring Boot services activate the `docker` profile (`SPRING_PROFILES_ACTIVE=docker`), which applies `application-docker.yml` overrides pointing at the Docker-network hostnames (`postgres-<name>:5432`, `kafka:29092`).

---

## Health Endpoints

| Service              | Health URL                                      |
|----------------------|-------------------------------------------------|
| api-gateway          | `http://localhost:8080/actuator/health`         |
| identity-service     | `http://localhost:8001/api/v1/actuator/health`  |
| product-service      | `http://localhost:8002/api/v1/actuator/health`  |
| inventory-service    | `http://localhost:8003/api/v1/actuator/health`  |
| warehouse-service    | `http://localhost:8004/api/v1/actuator/health`  |
| supplier-service     | `http://localhost:8005/api/v1/actuator/health`  |
| customer-service     | `http://localhost:8006/api/v1/actuator/health`  |
| purchase-order-service | `http://localhost:8007/api/v1/actuator/health` |
| sales-order-service  | `http://localhost:8008/api/v1/actuator/health`  |
| Prometheus           | `http://localhost:9090/-/healthy`               |
| Grafana              | `http://localhost:3000/api/health`              |
| Loki                 | `http://localhost:3100/ready`                   |
| Tempo                | `http://localhost:3200/status`                  |

All application health responses include component details (`show-details: always`), covering DB connectivity, Kafka, and disk space.

---

## Smoke Test

```bash
bash scripts/smoke-test.sh
```

The script runs three phases:

| Phase | What it checks | Blocking? |
|-------|----------------|-----------|
| 0 | TCP reachability of all PostgreSQL, Redis, Kafka, Zookeeper ports | No — warns only |
| 0b | HTTP liveness of Prometheus, Grafana, Loki, Tempo, Mailpit | No — warns only |
| 1 | Polls every app service `/actuator/health` until `status: UP` or `TIMEOUT` seconds | **Yes — fails on timeout** |
| 2 | Business-flow happy path (auth → product → stock-in → sales order) | Opt-in via `RUN_HAPPY_PATH=1` |

Environment variables:

```bash
HOST=localhost          # target host
TIMEOUT=300            # seconds to wait per service
NO_COLOR=1             # disable ANSI output (useful in CI)
RUN_HAPPY_PATH=1       # also run business-flow assertions
SMOKE_USER=admin       # login username for Phase 2
SMOKE_PASSWORD=secret  # login password for Phase 2
```

---

## Common Operations

```bash
# Start everything
docker compose up -d --build

# View logs for a service
docker compose logs -f identity-service

# Stop and remove containers (volumes preserved)
docker compose down

# Stop and wipe all volumes (full reset)
docker compose down -v

# Rebuild a single service
docker compose build identity-service
docker compose up -d --no-deps identity-service

# Check container health status
docker compose ps

# Open a psql shell in the identity DB
docker exec -it smartstock-postgres-identity \
  psql -U smartstock -d smartstock_identity

# Produce a test Kafka message
docker exec -it smartstock-kafka \
  kafka-console-producer --bootstrap-server localhost:9092 --topic identity.user.events
```

---

## Observability

After the stack is up, the following UIs are available:

| UI | URL | Default credentials |
|----|-----|---------------------|
| Grafana | http://localhost:3000 | admin / admin123 |
| Prometheus | http://localhost:9090 | — |
| pgAdmin | http://localhost:5050 | admin@smartstock.dev / admin123 |
| MinIO console | http://localhost:9001 | minioadmin / minioadmin |
| RabbitMQ management | http://localhost:15672 | guest / guest |
| Mailpit | http://localhost:8025 | — |
| Swagger (via gateway) | http://localhost:8080/swagger-ui.html | — |

Prometheus scrapes all services at `/api/v1/actuator/prometheus` every 15 seconds. Grafana is pre-provisioned with Prometheus and Loki datasources. Distributed traces are collected by Tempo via OTLP (gRPC `:4317`, HTTP `:4318`).

---

## Kafka Topics

Topics are auto-created on first use (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`). The canonical topic names are:

| Service | Topics |
|---------|--------|
| identity-service | `identity.user.events`, `identity.auth.events` |
| product-service | `product.events` |
| inventory-service | `inventory.events` |
| purchase-order-service | `purchase-order.events`, DLQ: `purchase-order.events.DLT` |
| sales-order-service | `sales-order.events`, DLQ: `sales-order.events.DLT` |

Services use the transactional outbox pattern for reliable event publishing (see [stabilization report](../reviews/stabilization-final-report.md)).

---

## Troubleshooting

**`docker compose up` fails with "JWT_SECRET must be set"**
→ Copy `.env.example` to `.env` and set `JWT_SECRET` to the output of `openssl rand -base64 64`.

**Service stuck in `starting` / health never UP**
→ `docker compose logs <service>` — common causes: Flyway migration mismatch, Kafka not yet elected leader, DB password mismatch.

**Kafka health check keeps failing**
→ Zookeeper can take 20–30 s to elect a leader. Kafka retries 10 times with 15 s interval (2.5 min total). If it still fails, check `docker compose logs zookeeper`.

**`scripts/smoke-test.sh` shows TIMEOUT for a service**
→ Increase `TIMEOUT` (default 300 s) for slow machines: `TIMEOUT=600 bash scripts/smoke-test.sh`.

**Port already in use**
→ Identify the conflict with `netstat -ano | findstr :<port>` (Windows) or `lsof -i :<port>` (Linux/macOS), then stop the conflicting process or remap the host port in `docker-compose.yml`.
