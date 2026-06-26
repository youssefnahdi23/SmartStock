# Changelog

All notable changes to SmartStock AI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Changelog entries are automatically generated from commit messages using [Conventional Commits](https://www.conventionalcommits.org/) and [Release Please](https://github.com/googleapis/release-please).

## [1.0.0-rc.1] - 2026-06-26

Release Engineering hardening pass — production-readiness review for Release
Candidate 1. No business logic, public API, or architecture changes.

### Fixed

- **Config consistency:** unified all services onto the canonical 800x port scheme
  (identity 8001 … sales-order 8008, skeletons 8009–8014, gateway 8080); removed the
  divergent 808x scheme from `.env.example`.
- **Env-var usage / connectivity:** made Kafka `bootstrap-servers` env-driven in 5
  implemented services, and normalized datasource config (env-driven `DB_HOST`/
  `DB_PORT`/`DB_NAME`, canonical `smartstock_*` database names, `smartstock` dev
  credentials) across customer/inventory/supplier/purchase-order/sales-order/warehouse
  — previously hardcoded `localhost`, non-canonical DB names (`customer_db` etc.) and
  `postgres` credentials that would not match the provisioned databases.
- **Observability:** rewrote `infrastructure/prometheus.yml` to scrape correct ports
  and the correct `/api/v1/actuator/prometheus` path for all services (gateway at
  `/actuator/prometheus`); previous config scraped wrong ports/paths and only 4 services.
- **Skeleton services** (audit, notification, reporting, analytics, data-export, order):
  env-driven datasource/Kafka, unique ports, structured JSON logging, removed dead
  Keycloak OAuth2 issuer, and corrected `<name>` POM template leftovers.
- **Docker:** replaced the 6 broken multi-stage skeleton Dockerfiles with the hardened
  pre-built-JAR pattern (non-root user, container-aware JVM flags, correct
  healthcheck path/port, repo-root build context).
- **Infrastructure:** added the 4 missing per-service PostgreSQL instances
  (supplier 5436, customer 5437, purchase-order 5438, sales-order 5439) to
  `docker-compose.yml`.
- **Security:** removed plaintext credentials from `docker-compose.services.yml`
  (now env-indirected) and deprecated that legacy file.

### Added

- RC1 documentation set: `RELEASE_CHECKLIST.md`, `DEPLOYMENT_REPORT.md`,
  `TEST_REPORT.md`, `SECURITY_REVIEW.md`, `KNOWN_LIMITATIONS.md`.

### Verified

- `mvn -f services/pom.xml clean test` → 169 tests, 0 failures/errors/skipped.
- `mvn -f services/pom.xml package -DskipTests` → all 16 modules produce JARs whose
  names match the Dockerfile `COPY` globs.

## [1.0.0] - 2026-06-23

### Features

- Initial release of SmartStock AI Enterprise Inventory Intelligence Platform
- Identity Service with JWT authentication and RBAC
- Microservices architecture with PostgreSQL per service
- API Gateway integration
- Event-driven architecture with Kafka
- Docker Compose infrastructure bootstrap
- CI/CD pipelines for build, test, code quality, and security
