# SmartStock AI — Stabilization Progress

Living log of the stabilization effort. Plan: [stabilization-plan.md](stabilization-plan.md). Source of truth: [technical-debt.md](technical-debt.md).

| Milestone | Debt | Status | Commit |
|-----------|------|--------|--------|
| M1 | C-1 | ✅ Done | _pending_ |
| M2 | C-5, M-5 | ⏳ Not started | — |
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
