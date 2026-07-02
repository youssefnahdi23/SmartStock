# SmartStock AI — Deployment Report (Release 1.0)

**Release:** 1.0.0 · **Date:** 2026-07-02 · **Owner:** Release Manager (Phase 10)

How to deploy SmartStock AI 1.0, the verified state of each deployment artifact, and the gaps that
remain. Two deployment paths are supported: **Docker Compose** (single host) and **Helm/Kubernetes**
(production).

> **Verification basis.** Deployment artifacts (Dockerfiles, `docker-compose.yml`, the Helm chart,
> K8s manifests, DR scripts) were verified by **static inspection** in Phase 10 and by **recorded
> executed evidence** from Phase 9 DevOps (Helm lint + kubeconform + a live **kind** install). The
> Phase 10 environment has **no Docker daemon**, so container builds and `docker compose up` were
> **not re-executed here** — deployers must run §4 smoke checks on their target host (KL K-6).

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

 Shared infra:  Kafka:9092 · Zookeeper:2181 · Redis:6379 · RabbitMQ · MinIO:9000
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

Skeleton services (not in 1.0 functional scope): audit 8009, notification 8010, reporting 8011,
analytics 8012, data-export 8013, order 8014.

> Services run under `server.servlet.context-path: /api/v1`, so actuator is at
> **`/api/v1/actuator/*`**. The reactive gateway has no context-path and serves actuator at
> **`/actuator/*`**. Prometheus scrape config and Dockerfile healthchecks are aligned to this.

---

## 2. Build artifacts

| Gate | Result | Source |
|------|--------|--------|
| `mvn -f services/pom.xml clean verify` | ✅ exit 0 (18 modules, 209 tests, coverage gate green) | Phase 7 (executed, Maven 3.9.6 / Java 21) |
| `mvn -f services/pom.xml package` | ✅ 15 service JARs + `smartstock-common-1.0.0.jar` | Phase 7 (executed) |
| JAR ↔ Dockerfile `COPY` glob match | ✅ every `smartstock-<svc>-1.0.0.jar` matches its `COPY` glob | static (Phase 10) |
| Re-execution in Phase 10 env | ⚠️ not run — no local Maven (KL K-6) | — |

All artifacts version `1.0.0` (root pom + `.release-please-manifest.json`).

---

## 3. Docker images

15 service Dockerfiles (8 implemented + gateway + 6 skeletons) follow one **hardened, pre-built-JAR**
pattern:

- Base `eclipse-temurin:21-jre-alpine`
- **Non-root** runtime user per service
- Container-aware JVM: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
- `HEALTHCHECK` on `/api/v1/actuator/health` (gateway: `/actuator/health`), correct per-service port
- Build context = **repository root**

**Build (from repo root, after `mvn package`):**
```bash
docker build -f services/identity-service/Dockerfile -t smartstock/identity-service:1.0.0 .
# …repeat per service; context MUST be the repo root (.)
```

> ⚠️ Image builds were **not executed** in the Phase 10 environment (no Docker daemon). Dockerfiles
> were validated statically and against the produced JAR names. The CI `docker-build` job
> (`ci-cd.yml`) builds and publishes them.

---

## 4. Deploy — Docker Compose (single host)

### Step 1 — Configuration
```bash
cp .env.example .env
# REQUIRED: set a strong JWT_SECRET (>= 64 bytes for HS512):
#   openssl rand -base64 64
# Override DB_PASSWORD / REDIS_PASSWORD / GRAFANA_PASSWORD for any shared env.
```

### Step 2 — Full stack
```bash
docker compose up -d          # 8 postgres + redis + kafka + zookeeper + rabbitmq + minio
                              # + prometheus/grafana/loki/tempo + 8 services + api-gateway
docker compose ps             # confirm health
```

### Step 3 — Smoke checks
```bash
curl -fsS http://localhost:8001/api/v1/actuator/health   # {"status":"UP"}
curl -fsS http://localhost:8001/api/v1/actuator/prometheus | head
curl -fsS http://localhost:8001/api/v1/swagger-ui.html -o /dev/null && echo "OpenAPI UI OK"
curl -fsS http://localhost:8080/actuator/health           # gateway
open http://localhost:9090/targets                        # Prometheus targets UP
open http://localhost:3000                                # Grafana (Prometheus/Loki/Tempo datasources)
```

`scripts/smoke-test.sh` and `scripts/smoke-observability.sh` automate these.

---

## 5. Deploy — Kubernetes / Helm (production)

The Helm umbrella chart in [`helm/smartstock`](helm/README.md) renders the full platform per
environment overlay. One `services` map drives a Deployment + Service (+ optional HPA / PDB /
StatefulSet) per service.

| Deliverable | Artifact | Status |
|-------------|----------|--------|
| Kubernetes namespace | `k8s/namespace.yaml` (restricted PSS) | ✅ |
| Helm chart | `helm/smartstock` umbrella, dev/prod overlays | ✅ lint + template clean |
| Ingress | `templates/ingress.yaml` → api-gateway, TLS via cert-manager | ✅ prod |
| Secrets | dev: chart-rendered · prod: `ExternalSecret` (ESO) → external store | ✅ two-tier |
| ConfigMaps | `templates/configmap.yaml` (shared non-secret env) | ✅ |
| Persistent Volumes | Postgres `StatefulSet` + `volumeClaimTemplates` per DB | ✅ |
| Horizontal Pod Autoscaler | CPU + memory targets | ✅ inventory / sales-order / gateway |
| Rolling Updates | maxSurge 25% / maxUnavailable 0, minReadySeconds, config-checksum, preStop drain, PDB, topology spread | ✅ |
| Backup | daily `pg_dump` CronJob → S3/MinIO, retention pruning | ✅ (`backup.enabled`) |
| Disaster Recovery | runbook + `scripts/db-restore.sh` | ✅ [docs/deployment/DISASTER_RECOVERY.md](docs/deployment/DISASTER_RECOVERY.md) |

### Deploy (prod)
```bash
# 1. Install External Secrets Operator + a ClusterSecretStore (one-time).
# 2. Provision the S3 backup-credentials Secret (smartstock-backup-s3).
kubectl apply -f k8s/namespace.yaml
helm upgrade --install smartstock ./helm/smartstock \
  -n smartstock-prod --create-namespace \
  -f helm/smartstock/values-prod.yaml \
  --set global.imageNamespace=<owner>/smartstock \
  --set global.imageTag=<immutable-tag-or-digest>
kubectl -n smartstock-prod rollout status deploy/api-gateway
```

### Deploy (local kind/minikube — self-hosts Postgres)
```bash
kubectl apply -f k8s/namespace.yaml
helm upgrade --install smartstock-dev ./helm/smartstock \
  -n smartstock -f helm/smartstock/values-dev.yaml \
  --set global.imageNamespace=<owner>/smartstock
```

---

## 6. Rollback

Stateless services — rollback = redeploy the previous image tag. Database changes are
Flyway-versioned and **forward-only**; no destructive migrations are present in 1.0.
`docker compose down` (without `-v`) preserves named volumes.

On Kubernetes: `kubectl rollout undo deploy/<svc>` (5 revisions retained via `revisionHistoryLimit`).

---

## 7. Verification status

| Check | Method | Result |
|-------|--------|--------|
| Compiles, tests, packages | Maven | ✅ recorded (Phase 7) |
| Config/env indirection | static review | ✅ (Phase 10) |
| Flyway migration naming (76 migrations) | static + `check-flyway-versions.sh` | ✅ |
| Chart lint (dev + prod) | `helm lint` | ✅ 0 failed (Phase 9) |
| Render + schema (dev/prod) | `helm template \| kubeconform -strict` | ✅ (Phase 9) |
| Live kind install (dev overlay) | `helm install` | ✅ 34 objects; **PVs 8/8 Bound**; StatefulSets `Running` under restricted PSS (Phase 9) |
| PV data persistence | pod delete + reattach | ✅ row survived pod restart (Phase 9) |
| App pods `Ready` on cluster | `kubectl` | ⚠️ `ImagePullBackOff` — images not published to a registry here (KL K-3) |
| Live `docker compose up` in Phase 10 | Docker | ⛔ not executed — no daemon (KL K-6) |

### Phase-9 bug caught by the live cluster
`strategy.rollingUpdate.maxUnavailable` was quoted → the API server rejected `"0"` as a non-percent
string (kubeconform did **not** catch it; the live API did). Fixed by emitting IntOrString unquoted.

---

## 8. Remaining gates for a fully-live production run

1. **Publish service images** to the registry (CI → GHCR), then re-run the Helm install to reach
   `Ready` app pods (closes K-3).
2. **Execute §4 smoke checks** on the deployer's Docker/K8s host to close the local runtime gate
   (K-6).
3. **Run `scripts/smoke-test.sh` end-to-end** against a cluster with Kafka deployed.

The deployment **artifacts themselves are verified**; the residual items are environment/registry
provisioning, not defects.
