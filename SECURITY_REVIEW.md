# SmartStock AI — Security Review (RC1)

**Release:** 1.0.0-rc.1 · **Date:** 2026-06-26 · **Owner:** Release Engineering
**Scope:** Secrets, authN/authZ, transport/config hardening, dependency & CI security.

---

## 1. Executive summary

No high-entropy or production secrets are committed. Authentication is JWT-based and
enforced at the API Gateway with per-service `SecurityConfig`. Remaining items are
**low-entropy dev defaults** (intentional, for one-command local startup) and
**test-only signing secrets** (required for HS512 unit tests). RC1 is acceptable for
functional testing provided `JWT_SECRET` and all `*_PASSWORD` values are overridden
in any shared/staging/production environment.

**Risk posture: ACCEPTABLE for RC1 functional testing.**

---

## 2. Secrets management

| Check | Result |
|-------|--------|
| High-entropy secrets in source/config | ✅ none (repo-wide sweep) |
| JWT signing secret | ✅ externalized as `${JWT_SECRET:<JWT_SECRET>}` (placeholder default, 18 occurrences) |
| Plaintext credentials in `docker-compose.yml` | ✅ none — all `${VAR:-default}` |
| Plaintext credentials in legacy `docker-compose.services.yml` | ✅ remediated to env indirection + file deprecated |
| Secrets in CI workflows | ✅ referenced via `${{ secrets.* }}` only |
| Secrets in `.env.example` | ✅ placeholders / labeled dev defaults only |

### Residual (accepted) secrets
- **Test signing secrets** under `src/test/resources/application-test.yml`
  (identity, warehouse, api-gateway) — clearly labeled test-only, required for HS512
  tests. *Action:* configure GitGuardian to ignore `**/src/test/**`.
- **Low-entropy dev defaults** (`smartstock123`, `admin123`, `minioadmin`) in compose
  defaults — acceptable for local dev; **must be overridden** outside development.

---

## 3. Authentication & authorization

| Control | Status | Notes |
|---------|--------|-------|
| JWT (HS512) issuance | ✅ | identity-service; access 1h / refresh 30d per ADR-0005 |
| Edge enforcement | ✅ | `JwtAuthenticationGlobalFilter` on the gateway rejects invalid/expired tokens |
| Per-service `SecurityConfig` | ✅ | stateless sessions; constructor injection; no field `@Autowired` |
| RBAC | ✅ | role model in identity-service |
| Account lockout / password policy | ✅ | 5 attempts → 30 min lock; 90-day expiry; history 5 (ADR-0005) |
| CORS | ⚠️ | allowed origins env-driven (`CORS_ORIGINS`); default includes localhost — restrict in prod |
| Dead Keycloak OAuth2 issuer in skeletons | ✅ removed | skeletons previously declared an unreachable `issuer-uri`; removed in RC1 |

---

## 4. Transport & configuration hardening

| Item | Status | Notes |
|------|--------|-------|
| Error responses | ✅ | `include-stacktrace: never`, `include-exception: false` |
| Actuator exposure | ✅ | only `health,info,prometheus,metrics`(+`flyway`/`gateway`) exposed |
| Containers run as non-root | ✅ | all 15 service images use a dedicated non-root user |
| Secrets via env only | ✅ | no secrets baked into images |
| TLS termination | ⚠️ | not configured in compose — terminate at ingress/LB in real environments (GA) |

---

## 5. Dependency & supply-chain security

| Control | Status |
|---------|--------|
| OWASP Dependency-Check (CI) | ✅ `-DfailBuildOnCVSS=5.0` (security.yml) |
| Snyk (CI) | ✅ (continue-on-error) |
| GitGuardian secret scan (CI) | ✅ |
| Centralized, BOM-managed versions | ✅ |
| MapStruct `1.6.0.Beta1` (pre-release in prod path) | ⚠️ documented (KNOWN_LIMITATIONS L-1) |
| Dated CI action versions (`upload-artifact@v3`, `codeql-action@v2`, `codecov@v3`) | ⚠️ refresh recommended |

---

## 6. Findings & actions

| ID | Severity | Finding | Status |
|----|----------|---------|--------|
| S-1 | High | Committed high-entropy JWT secrets | ✅ Fixed (pre-RC1 architect pass) — placeholders |
| S-2 | Medium | Plaintext creds in legacy compose | ✅ Fixed — env indirection + deprecation |
| S-3 | Medium | Skeletons referenced unreachable Keycloak issuer | ✅ Fixed — removed |
| S-4 | Low | Low-entropy dev defaults in compose/.env | ⚠️ Accepted for dev; override outside dev |
| S-5 | Low | Test signing secrets under `src/test` | ⚠️ Accepted; scope GitGuardian ignore |
| S-6 | Low | No TLS in compose | ⏭️ GA — terminate at ingress |
| S-7 | Low | CORS default allows localhost | ⚠️ Restrict via `CORS_ORIGINS` in prod |

**No GA-blocking security defects identified.** Before any non-dev deployment:
override `JWT_SECRET` + all `*_PASSWORD`, restrict CORS, and terminate TLS at the edge.
