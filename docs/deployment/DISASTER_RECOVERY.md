# SmartStock — Backup & Disaster Recovery Runbook

**Scope:** production Kubernetes deployment (Helm chart `helm/smartstock`, prod
overlay `values-prod.yaml`). Covers backup strategy, recovery objectives, and
step-by-step restore/failover procedures.

> This is an operational runbook. Rehearse it (§7) — an untested backup is not a
> backup.

---

## 1. Recovery objectives

| Metric | Target | Basis |
|--------|--------|-------|
| **RPO** (max data loss) | ≤ 24h (logical dumps) · ≤ 5 min (with managed PITR) | daily CronJob; managed-Postgres WAL |
| **RTO** (time to restore) | ≤ 60 min for a single service · ≤ 4h full platform | measured in DR drills |
| Backup retention | 30 days (prod) / 14 days (default) | `backup.retentionDays` |
| Backup location | S3 bucket `smartstock-prod-backups`, versioned + SSE | `backup.s3.*` |

Services are **stateless**; all durable state lives in the per-service PostgreSQL
databases and Kafka. Recovery therefore centres on the databases.

---

## 2. What is backed up

| Asset | Mechanism | Where |
|-------|-----------|-------|
| Per-service Postgres DBs | `pg_dump` CronJob (daily 02:00 UTC), gzipped | `s3://<bucket>/<db>/<db>-<ts>.sql.gz` |
| Postgres (managed) | Provider automated backups + PITR/WAL | RDS/Cloud SQL snapshots |
| Secrets | External store (AWS Secrets Manager / Vault) via ESO | source of truth is the store, **not** the cluster |
| Config | Git (`helm/`, `k8s/`, overlays) — this repo | version control |
| Object storage (MinIO/S3) | bucket versioning + cross-region replication | provider |
| Kafka topics | replication factor ≥ 3; outbox pattern makes events replayable | broker |

The backup CronJob is rendered by
[`templates/backup-cronjob.yaml`](../../helm/smartstock/templates/backup-cronjob.yaml)
and enabled with `backup.enabled=true`.

---

## 3. Verify backups are running

```bash
kubectl -n smartstock-prod get cronjob smartstock-db-backup
kubectl -n smartstock-prod get jobs -l app.kubernetes.io/component=backup
# Inspect the most recent run
kubectl -n smartstock-prod logs job/<latest-backup-job>
# List what's in object storage
aws s3 ls s3://smartstock-prod-backups/ --recursive | tail
```

Trigger an out-of-band backup (e.g. before a risky migration):

```bash
kubectl -n smartstock-prod create job --from=cronjob/smartstock-db-backup backup-manual-$(date +%s)
```

---

## 4. Restore a single database

Use [`scripts/db-restore.sh`](../../scripts/db-restore.sh). Run it from a pod/box
with network access to the DB and the S3 bucket.

```bash
export S3_BUCKET=smartstock-prod-backups
export PGHOST=postgres-inventory.data.svc PGUSER=smartstock PGPASSWORD=****
export AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_DEFAULT_REGION=us-east-1

# newest dump:
scripts/db-restore.sh smartstock_inventory latest
# a specific point:
scripts/db-restore.sh smartstock_inventory 20260701T020000Z
# from a clean DB (drops + recreates first):
DROP_EXISTING=true scripts/db-restore.sh smartstock_inventory latest
```

Then restart the owning service so Flyway reconciles and connections re-pool:

```bash
kubectl -n smartstock-prod rollout restart deploy/inventory-service
kubectl -n smartstock-prod rollout status  deploy/inventory-service
```

---

## 5. Disaster scenarios

### 5.1 Single service DB corruption / bad data
1. Scale the service to 0 to stop writes: `kubectl scale deploy/<svc> --replicas=0`.
2. Restore per §4 (`DROP_EXISTING=true` for corruption).
3. Scale back / `rollout restart`; verify `/api/v1/actuator/health`.

### 5.2 Bad application release
Rolling updates keep old ReplicaSets (`revisionHistoryLimit: 5`). No restore needed:
```bash
kubectl -n smartstock-prod rollout undo deploy/<svc>          # previous
kubectl -n smartstock-prod rollout undo deploy/<svc> --to-revision=<n>
```
Migrations are **forward-only** (Flyway); a rolled-back image stays schema-compatible.

### 5.3 Node / zone loss
Handled automatically: `topologySpread` spreads replicas across zones, PDBs keep a
quorum during drains, and the scheduler reschedules pods. Confirm capacity:
`kubectl get pods -o wide` and cluster-autoscaler events.

### 5.4 Full namespace / cluster loss (rebuild)
1. **Cluster:** provision a new cluster (IaC) with the same StorageClasses and a CNI.
2. **Secrets:** install External Secrets Operator + the `ClusterSecretStore`; the
   `ExternalSecret` repopulates `smartstock-secrets` from the external store.
3. **Namespace + platform:**
   ```bash
   kubectl apply -f k8s/namespace.yaml
   helm upgrade --install smartstock ./helm/smartstock \
     -n smartstock-prod -f helm/smartstock/values-prod.yaml \
     --set global.imageTag=<last-known-good>
   ```
4. **Data:** point services at managed Postgres restored from the provider snapshot
   (fastest, meets RPO). If dumps are the only source, restore each DB per §4.
5. **Verify** per §6.

### 5.5 Region loss (managed Postgres + S3)
Fail over to the cross-region read replica / restore from cross-region backups,
repoint `services.<svc>.database.host` in the DR-region overlay, and redeploy the
chart to the standby cluster.

---

## 6. Post-recovery verification

```bash
kubectl -n smartstock-prod get pods                       # all Running/Ready
for p in 8001 8002 8003 8004 8005 8006 8007 8008; do :; done
kubectl -n smartstock-prod exec deploy/api-gateway -- \
  wget -qO- localhost:8080/actuator/health                # {"status":"UP"}
# Smoke the business flow:
bash scripts/smoke-test.sh                                 # end-to-end checks
```
Confirm Prometheus targets are UP and error-rate/latency SLOs are green in Grafana.

---

## 7. DR drill (quarterly)

1. Spin up an isolated namespace/cluster.
2. Restore the latest dumps of all 8 DBs (§4) and deploy the chart.
3. Run `scripts/smoke-test.sh`; record **actual** RTO and any data gap (RPO).
4. File deltas against this runbook. Sign-off in `docs/reviews/`.

---

## 8. Ownership & escalation

| Role | Responsibility |
|------|----------------|
| On-call SRE | Execute this runbook; declare incident severity |
| Release Eng | Image rollbacks (§5.2), chart redeploy |
| Data owner | Approve destructive restores (§5.1 `DROP_EXISTING`) |

Related: [DEPLOYMENT_REPORT.md](../../DEPLOYMENT_REPORT.md) ·
[helm/README.md](../../helm/README.md) · [KNOWN_LIMITATIONS.md](../../KNOWN_LIMITATIONS.md)
