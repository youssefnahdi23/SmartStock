# SmartStock — Kubernetes / Helm Deployment

Skeleton Helm chart for deploying the SmartStock microservices to Kubernetes
(roadmap **M8 / Phase 9 — DevOps**). One chart renders a Deployment + Service
(+ optional HPA / NetworkPolicy / Ingress) per application service from the
`services` map in [`smartstock/values.yaml`](smartstock/values.yaml).

> **Status: skeleton.** Structurally complete and `helm template`-clean, but not
> yet validated against a live cluster. Infra (PostgreSQL, Kafka, Redis, Tempo,
> Loki) is expected to be provided — see *Infrastructure* below.

## Layout

```
helm/smartstock/
  Chart.yaml            umbrella chart (+ commented Bitnami infra subcharts)
  values.yaml           defaults + the services map
  values-dev.yaml       kind/minikube overlay (chart-rendered secrets)
  values-prod.yaml      prod overlay (external secrets, HPA, Ingress, NetworkPolicy)
  templates/
    _helpers.tpl              names, labels, image ref, secret-name resolution
    configmap.yaml            shared non-secret env (Kafka/OTEL/Loki/profile)
    secret.yaml               DEV-ONLY rendered secret (guarded by secrets.create)
    externalsecret.yaml       prod ExternalSecret (ESO) → secrets.existingSecret
    deployment.yaml           per-service Deployment: rolling updates, graceful
                              shutdown, config checksum, topology spread
    service.yaml              per-service ClusterIP Service
    hpa.yaml                  HPA (CPU + optional memory) with scale behavior
    pdb.yaml                  PodDisruptionBudget for multi-replica/HPA services
    postgres-statefulset.yaml self-hosted Postgres StatefulSet + PVC per DB (dev)
    backup-cronjob.yaml       daily pg_dump → S3/MinIO with retention pruning
    serviceaccount.yaml       per-release SA (token automount disabled)
    networkpolicy.yaml        default-deny + intra-namespace (prod)
    ingress.yaml              single public entrypoint → api-gateway
    NOTES.txt                 post-install summary
k8s/namespace.yaml            namespace with restricted Pod Security Standards
```

## Production capabilities (Phase 9)

| Concern | How | Toggle |
|---------|-----|--------|
| **Rolling updates** | `RollingUpdate` (maxSurge 25%, maxUnavailable 0), `minReadySeconds`, `progressDeadlineSeconds`, config/secret checksum annotations force rollout on change | always on; tune `rollingUpdate.*` |
| **Graceful shutdown** | `terminationGracePeriodSeconds` + preStop endpoint-drain sleep | `terminationGracePeriodSeconds`, `preStopSleepSeconds` |
| **Persistent Volumes** | Postgres `StatefulSet` + `volumeClaimTemplates` (dynamic PVC per DB) | `postgres.enabled` (dev), managed in prod |
| **HPA** | CPU + optional memory targets, scale-up/down stabilization | `services.<svc>.hpa.*`, `hpaBehavior.*` |
| **PodDisruptionBudget** | rendered for multi-replica/HPA services only | `podDisruptionBudget.enabled` |
| **Topology spread** | across nodes/zones for HA | `topologySpread.enabled` |
| **Secrets** | dev: chart-rendered; prod: ExternalSecret → external store | `secrets.create`, `externalSecret.enabled` |
| **Backup** | daily `pg_dump` CronJob → S3/MinIO, retention pruning | `backup.enabled` |
| **Disaster recovery** | runbook + restore script | [docs/deployment/DISASTER_RECOVERY.md](../docs/deployment/DISASTER_RECOVERY.md) |

## Secrets strategy (two-tier)

| Mode | `secrets.create` | Source | Use |
|------|------------------|--------|-----|
| Dev  | `true`  | Chart renders a `Secret` from values | kind/minikube only — **never commit real values** |
| Prod | `false` | References `secrets.existingSecret` | managed by **External Secrets Operator** (recommended), Sealed Secrets, or SOPS |

The pods consume three keys in all modes: `JWT_SECRET`, `DB_USER`, `DB_PASSWORD`.
In prod, provision them out-of-band (e.g. an `ExternalSecret` syncing from AWS
Secrets Manager / Vault into a `Secret` named `smartstock-secrets`) — the chart
never sees plaintext.

## Install

Dev (kind/minikube):
```bash
kubectl apply -f k8s/namespace.yaml
helm upgrade --install smartstock-dev ./helm/smartstock \
  -n smartstock \
  -f helm/smartstock/values-dev.yaml \
  --set global.imageNamespace=<owner>/smartstock
```

Prod:
```bash
# 1. Provision the external Secret (ExternalSecret / SealedSecret / sops) first.
# 2. Then:
helm upgrade --install smartstock ./helm/smartstock \
  -n smartstock-prod --create-namespace \
  -f helm/smartstock/values-prod.yaml \
  --set global.imageTag=<immutable-tag-or-digest>
```

Render/lint without a cluster:
```bash
helm lint ./helm/smartstock -f helm/smartstock/values-dev.yaml
helm template smartstock ./helm/smartstock -f helm/smartstock/values-dev.yaml
```

## Infrastructure

Application pods assume reachable `postgres-<svc>`, `kafka`, `redis`, `tempo`,
and `loki` Services. Options:
1. **Local:** enable the Bitnami subcharts in `Chart.yaml` (`helm dependency update`).
2. **Managed:** point `services.<svc>.database.host` and `config.*` at managed endpoints in the prod overlay.

## Known gaps before this is production-ready (M8 backlog)

- [x] `helm lint` / `helm template` verified in CI; `kubeconform` schema gate wired into the pipeline (`helm-validate` job).
- [x] Postgres-per-service as **StatefulSets** (self-hosted, `postgres.enabled`) or **managed** (prod overlay hosts).
- [x] `PodDisruptionBudget` for HPA-managed/multi-replica services + `topologySpreadConstraints` (prod overlay).
- [x] Verify DB names for purchase-order / sales-order against `docker-compose.yml` (`smartstock_purchase_order`, `smartstock_sales_order` — match).
- [x] Backup strategy (`pg_dump` CronJob → S3) and disaster-recovery runbook (`docs/deployment/DISASTER_RECOVERY.md`).
- [ ] Managed **Kafka** and **Redis** endpoints wired for prod (config points at placeholders; provision real infra).
- [ ] Split **liveness/readiness probe groups** (`/actuator/health/liveness|readiness`) once `management.endpoint.health.probes.enabled=true` in the services.
- [ ] Gatling p99 ≤200ms validation (M8 gate 3) and Trivy-clean images (M8 gate 4 — already enforced in ci-cd.yml).
- [ ] CD stage: `helm upgrade` from CI on tag, with rollout-status gating.
- [x] Live-cluster validation: dev overlay installed on kind (k8s 1.29.2) — restricted-PSS admission, 8/8 Postgres StatefulSets Ready, 8/8 PVCs Bound, PV data-persistence across pod restart; prod overlay passes server-side dry-run (HPA/PDB/CronJob/Ingress/NetworkPolicy). See DEPLOYMENT_REPORT §8.1.
- [ ] Publish service images to GHCR so app Deployments reach `Ready` (currently `ImagePullBackOff` locally — images not built), then run `scripts/smoke-test.sh` end-to-end (also needs Kafka deployed).
