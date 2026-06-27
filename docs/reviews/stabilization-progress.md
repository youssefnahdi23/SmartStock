# SmartStock AI — Stabilization Progress

Living log of the stabilization effort. Plan: [stabilization-plan.md](stabilization-plan.md). Source of truth: [technical-debt.md](technical-debt.md).

| Milestone | Debt | Status | Commit |
|-----------|------|--------|--------|
| M1 | C-1 | ✅ Done | 84fcba9 |
| M2 | C-5, M-5 | ✅ Done | _pending_ |
| M3 | C-4 | ⏳ Not started | — |
| M4 | C-2 | ⏳ Not started | — |
| M5 | C-3 | ⏳ Not started | — |
| M6 | H-3 | ⏳ Not started | — |
| M7 | H-1 | ⏳ Not started | — |
| M8 | H-4 | ⏳ Not started | — |
| M9 | H-7 | ⏳ Not started | — |

---

## M1 — C-1: Duplicate Flyway version in inventory-service ✅

**Problem:** `inventory-service` shipped two migrations at version `V2`
(`V2__add_stockin_inspection_columns.sql` and `V2__seed_data.sql`). Flyway aborts on
startup with *"Found more than one migration with version 2"* — a guaranteed cold-start
failure on a fresh DB.

**Fix:**
- Renamed `V2__seed_data.sql` → `V3__seed_data.sql` (`git mv`, history preserved). The
  column-add keeps `V2`; the seed now runs after it — ordering preserved and correct.
- Audited all 14 services with migrations: inventory was the **only** service with a
  collision (warehouse already uses V1/V2/V3 cleanly; others are V1 or V1/V2).
- Added [scripts/check-flyway-versions.sh](../../scripts/check-flyway-versions.sh): a
  portable, DB-free guard that fails on any duplicate version within a service. Excludes
  `target/` build output.
- Wired the guard into [build-test.yml](../../.github/workflows/build-test.yml) as the
  first step, so this class of defect fails the build, not the deploy.

**Verification (run 2026-06-27):**
- `bash scripts/check-flyway-versions.sh` → `OK: no duplicate Flyway migration versions found.`
- `ls services/inventory-service/.../db/migration` → `V1`, `V2`, `V3` (no collision).
- `mvn -f services/pom.xml -pl common,inventory-service -am test` → **BUILD SUCCESS**, 18 tests, 0 failures.

**Notes:** Full live Flyway `migrate`/`validate` against a Postgres instance is exercised
by the integration suite (M9) once a Docker host is available (K-5). The static guard
covers the specific C-1 failure mode deterministically in CI.

---

## M2 — C-5 + M-5: Full-stack compose + runtime verification ✅

**Problem:** `docker-compose.yml` ran infra + 8 DBs + gateway only — the 8 application
services were never wired in, so there was no one-command full-stack run and therefore no
way to exercise the wired system (the reason C-1…C-4 went undetected). A deprecated
`docker-compose.services.yml` duplicated config with the wrong build context and colliding
ports. **Worse:** every business service's `docker` Spring profile hardcoded a broken
datasource — host `postgres` (no such container; only `postgres-<svc>` exist) and stale DB
names (`customer_db`, `purchase_db`, `sales_db`, shared `smartstock`). Because the
Dockerfiles activate the `docker` profile, these overrode any compose env var → guaranteed
connection failure on boot.

**Fix:**
- Rewrote all 7 business-service `application-docker.yml` profiles (product, inventory,
  warehouse, supplier, customer, purchase-order, sales-order) to clean, **env-driven**
  config with correct per-service defaults (`postgres-<svc>` / `smartstock_<svc>`),
  `kafka:29092` internal listener, and consistent actuator/prometheus exposure. Fixed
  supplier's wrong `server.port: 8080`. identity-service has no docker profile and uses its
  env-driven base config (correct).
- Added the 8 application services to [docker-compose.yml](../../docker-compose.yml) on the
  canonical 800x ports, each with `depends_on` its `postgres-<svc>` + `kafka` at
  `condition: service_healthy`, env-driven config, and the JWT secret as a `${JWT_SECRET:-<JWT_SECRET>}`
  placeholder (no hardcoded secret).
- Removed the deprecated `docker-compose.services.yml`; repointed `Makefile.services`
  targets to the unified file and corrected the printed URLs to 800x + gateway.
- Added [scripts/smoke-test.sh](../../scripts/smoke-test.sh) (readiness gate across all 9
  endpoints + happy-path scaffold) and
  [runtime-verification.md](runtime-verification.md) documenting the one-command bring-up.

**Verification (run 2026-06-27):**
- `docker compose config -q` → **OK** (parses + resolves env, no daemon needed).
- `docker compose config --services` → all 8 app services + gateway + infra present;
  app-service count = **8**.
- `bash -n scripts/smoke-test.sh` → syntax OK.

**Notes (honest status):** the Docker **daemon is unavailable here** (K-5), so live
`docker compose up --build` and `smoke-test.sh` execution remain to be run on a Docker
host — commands are in runtime-verification.md. Everything that can be validated without a
daemon (compose parse, service graph, env resolution, script syntax) passes. Live runtime
proof is folded into M9's Docker-enabled CI job.
