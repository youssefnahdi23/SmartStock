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
    _helpers.tpl        names, labels, image ref, secret-name resolution
    configmap.yaml      shared non-secret env (Kafka/OTEL/Loki/profile)
    secret.yaml         DEV-ONLY rendered secret (guarded by secrets.create)
    deployment.yaml     generic per-service Deployment (ranges over services)
    service.yaml        generic per-service ClusterIP Service
    hpa.yaml            HPA where services.<svc>.hpa.enabled
    serviceaccount.yaml per-release SA (token automount disabled)
    networkpolicy.yaml  default-deny + intra-namespace (prod)
    ingress.yaml        single public entrypoint → api-gateway
    NOTES.txt           post-install summary
k8s/namespace.yaml      namespace with restricted Pod Security Standards
```

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

- [ ] `helm lint` / `helm template` verified in CI; a `kubeconform` / `kubeval` schema gate wired into the pipeline.
- [ ] Real infra: Postgres-per-service (StatefulSets or managed), Kafka, Redis.
- [ ] Split **liveness/readiness probe groups** (`/actuator/health/liveness|readiness`) once `management.endpoint.health.probes.enabled=true` in the services.
- [ ] `PodDisruptionBudget` for HPA-managed services; `topologySpreadConstraints`.
- [ ] Verify DB names for purchase-order / sales-order against `docker-compose.yml`.
- [ ] Gatling p99 ≤200ms validation (M8 gate 3) and Trivy-clean images (M8 gate 4 — already enforced in ci-cd.yml).
- [ ] CD stage: `helm upgrade` from CI on tag, with rollout-status gating.
