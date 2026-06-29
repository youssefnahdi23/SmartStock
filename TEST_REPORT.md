# SmartStock AI — Test Report (RC1)

**Release:** 1.0.0-rc.1 · **Date:** 2026-06-26 · **Owner:** Release Engineering
**Command:** `mvn -f services/pom.xml clean test` (Maven 3.9.6, Temurin 21)

---

## 1. Summary

| Metric | Value |
|--------|-------|
| Result | ✅ **PASS** (reactor exit 0) |
| Tests run | **169** |
| Failures | **0** |
| Errors | **0** |
| Skipped | **0** |
| Surefire report files | 67 |
| Modules built | 16 |

Re-run **after** all RC1 configuration/Dockerfile changes — still 169/0/0/0. No
regression introduced by the hardening changes.

> The build log contains `ERROR`-level lines from `GlobalExceptionHandlerTest`
> (api-gateway) — these are **intentional** negative-path assertions that exercise
> the gateway's error handler, not failures.

---

## 2. Scope of executed tests

**Unit + slice tests** run by default. Integration tests are **excluded** from the
default Surefire run via `**/*IntegrationTest.java` / `**/*RepositoryTest.java`
exclusions, because they require Testcontainers (Docker), which is unavailable in
this environment.

| Test category | Default `mvn test` | Notes |
|---------------|--------------------|-------|
| Unit (service/domain/mapper/filter) | ✅ executed | 169 tests |
| `@WebMvcTest` / `@WebFluxTest` slices | ✅ executed | included in the 169 |
| `*IntegrationTest` (`@SpringBootTest` + Testcontainers) | ⏭️ excluded offline | run in a Docker-enabled CI job |
| `*RepositoryTest` (Testcontainers Postgres) | ⏭️ excluded offline | run in a Docker-enabled CI job |

---

## 3. Coverage

- JaCoCo agent + `report` bound to the `test` phase in `services/pom.xml`
  (per-module `target/site/jacoco/jacoco.xml`).
- Coverage is **uneven**: implemented services carry meaningful unit tests; the 6
  skeleton services ship placeholder unit + integration tests only.
- ⚠️ CI uploads `./target/site/jacoco/jacoco.xml` (root) to Codecov, but JaCoCo
  reports are per-module — no aggregated root report exists. See KNOWN_LIMITATIONS K-6.

---

## 4. Per-module test inventory (source files)

| Module | Has unit tests | Has integration test | Notes |
|--------|----------------|----------------------|-------|
| identity-service | ✅ | ✅ (Testcontainers) | richest suite |
| product-service | ✅ | ✅ | |
| inventory-service | ✅ | ✅ | |
| warehouse-service | ✅ | ✅ | |
| supplier-service | ✅ | ✅ | |
| customer-service | ✅ | ✅ | |
| purchase-order-service | ✅ | ✅ | |
| sales-order-service | ✅ | ✅ | |
| api-gateway | ✅ | n/a | filter/exception/route unit tests |
| common | ✅ | n/a | shared util/DTO tests |
| audit / notification / reporting / analytics / data-export / order | ⚠️ placeholder | ⚠️ placeholder | skeletons |

(≈59 test source files; 456 main source files.)

---

## 5. Test gaps / recommendations (for GA)

1. **Run the integration suite on a Docker host** — gate GA on green Testcontainers
   integration + repository tests.
2. **Aggregate coverage** — add a JaCoCo aggregate module or fix the Codecov path so
   coverage trends are meaningful.
3. **Skeleton services** — replace placeholder tests with real tests when each is
   implemented (tracked in KNOWN_LIMITATIONS).
4. **Contract/E2E** — add gateway→service contract tests and a cross-service E2E flow
   (e.g. purchase-order → inventory → event) once the full-stack compose exists.

---

## 6. Verdict

Unit/slice test gate is **green and reproducible offline**. Integration tests are
**present but not executed here** (Docker unavailable). RC1 is cleared for
functional testing; the integration gate must be closed before GA.
