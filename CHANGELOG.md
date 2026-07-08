# Changelog

All notable changes to SmartStock AI will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Changelog entries are automatically generated from commit messages using [Conventional Commits](https://www.conventionalcommits.org/) and [Release Please](https://github.com/googleapis/release-please).

## 1.0.0 (2026-07-08)


### Features

* **analytics:** durable event-capture sink (H-4) ([8111c1a](https://github.com/youssefnahdi23/SmartStock/commit/8111c1a29aa2dcd5e96ca57dd08e8439ba042dbb))
* **api-gateway:** implement Milestone 2 — API Gateway ([b459895](https://github.com/youssefnahdi23/SmartStock/commit/b4598955f5f7e7159a1b8b53ce29e4c49e56ded1))
* **compose:** full-stack Docker Compose + fix broken docker datasource profiles (C-5/M-5) ([6e95aea](https://github.com/youssefnahdi23/SmartStock/commit/6e95aeacc190605c39b0c336ecf6f974e43e527b))
* **compose:** full-stack Docker Compose hardening + smoke-test rewrite (M-2) ([3d1cb47](https://github.com/youssefnahdi23/SmartStock/commit/3d1cb47126ee47e69e26ed94568c049dbd2d80ef))
* **consumer:** retry, DLQ and idempotency for Kafka consumers (H-3) ([9852c4f](https://github.com/youssefnahdi23/SmartStock/commit/9852c4f5e8939be4c146eade88e794449a99eac8))
* **customer-service:** implement Customer Service milestone with compliance fixes ([5fa5ef9](https://github.com/youssefnahdi23/SmartStock/commit/5fa5ef9316f3947668bf75c46665901e1036a682))
* **devops:** add Helm/K8s deployment skeleton and helm-validate CI job ([a77b690](https://github.com/youssefnahdi23/SmartStock/commit/a77b690a1de32750933721cacb87c2d165dbf8fe))
* **devops:** production deployment — PV/rolling-updates/backup/DR (Phase 9) ([9a8a277](https://github.com/youssefnahdi23/SmartStock/commit/9a8a277e48bb5c27f30518c24936155d772222df))
* **events:** complete reliable event integration ([58adf64](https://github.com/youssefnahdi23/SmartStock/commit/58adf64227011744e0e912c5356cbb6c29adc77c))
* **gateway:** serve a web console UI at the gateway root ([2a7a6d3](https://github.com/youssefnahdi23/SmartStock/commit/2a7a6d38ee0e59ef35ffcbf8b477e0a11257a584))
* **identity-service:** implement Milestone 1 — Identity & Security Foundation ([98cd706](https://github.com/youssefnahdi23/SmartStock/commit/98cd7067f88f291adca9565bbcdb1761463787c5))
* **inventory-service:** implement Milestone 3 — Inventory Service ([1267a84](https://github.com/youssefnahdi23/SmartStock/commit/1267a84dbc6f0c33c4c69823c4fdbc554e8c62d8))
* **outbox:** transactional outbox replaces fire-and-forget events (C-2) ([f942fbd](https://github.com/youssefnahdi23/SmartStock/commit/f942fbd8ed44bebb9c7f0d83d6311b5f7847d244))
* **product-service:** implement Milestone 3 — Product Service ([305e339](https://github.com/youssefnahdi23/SmartStock/commit/305e3390097c3b83355181d4a5792375d7b749fa))
* **purchase-order-service:** implement Purchase Order Service — Milestone 3 (partial) ([821f132](https://github.com/youssefnahdi23/SmartStock/commit/821f132b56dc174a7edbcee9ae2e442b3a8ff944))
* **sales-order-service:** implement Sales Order Service — Milestone 3 ([9618115](https://github.com/youssefnahdi23/SmartStock/commit/9618115ec1d0acec0824d28db008bfe00f98e49f))
* **supplier-service:** implement Milestone 3 — Supplier Service ([853cad1](https://github.com/youssefnahdi23/SmartStock/commit/853cad12aa7be40d18264f8d44b845e00d9a0bf8))
* **warehouse-service:** implement Milestone 3 — Warehouse Service ([20e41fa](https://github.com/youssefnahdi23/SmartStock/commit/20e41fab37327cb5c731a34bfc3ae6152c105141))


### Bug Fixes

* **api:** make all service APIs reachable through the gateway ([d7d04da](https://github.com/youssefnahdi23/SmartStock/commit/d7d04da168bc873597ca9837759a88f198ebc528))
* **build:** override mockito to 5.8.0 and fix JwtAuthenticationFilter compile error ([72858e1](https://github.com/youssefnahdi23/SmartStock/commit/72858e1b2f33459b3a19c74055b00b1344031de4))
* **common:** skip spring-boot repackage plugin — common is a library JAR, not an executable ([fb8744d](https://github.com/youssefnahdi23/SmartStock/commit/fb8744d154d4b002aed6223ab8a13d5de7913f85))
* **core-services:** apply compliance review fixes across all 4 services ([82f49dc](https://github.com/youssefnahdi23/SmartStock/commit/82f49dcc7e757c82e35cf39f7c4d03029e85b400))
* **dockerfile:** switch to pre-built JAR approach with eclipse-temurin:21-jre-alpine ([46a085b](https://github.com/youssefnahdi23/SmartStock/commit/46a085b5f9bf476d7920d7130c851fe4355244cb))
* **events:** canonical Topics.* across all producers + contract tests (M-3) ([226a9f6](https://github.com/youssefnahdi23/SmartStock/commit/226a9f62faed7ad57648720028982a34fe02a802))
* **events:** wire customer stats to real sales-order topic/event (C-4) ([c73fb23](https://github.com/youssefnahdi23/SmartStock/commit/c73fb23ad54b8dc4fde63a19ff1e85fcacf52a8f))
* **identity:** correct 404/405 handling and gate permission catalog (RBAC) ([5353c67](https://github.com/youssefnahdi23/SmartStock/commit/5353c67827756878cf30d14ee157161b2416a3a5))
* **identity:** resolve refresh-token, jsonb, and user-search defects found by IT ([c5ea527](https://github.com/youssefnahdi23/SmartStock/commit/c5ea527d70e119da072218b4ea043570710e28ac))
* **identity:** seed permissions for M-3 domain services (V4 migration) ([65d4312](https://github.com/youssefnahdi23/SmartStock/commit/65d43125d7732e03ffa2f5913023f1c886fb87db))
* **identity:** seed remaining permissions the services actually check (V5) ([f6144e0](https://github.com/youssefnahdi23/SmartStock/commit/f6144e016012467d67a889798f78fc96bff5162f))
* **infra:** repair observability stack and smoke-test happy path ([0722228](https://github.com/youssefnahdi23/SmartStock/commit/0722228ac97f55000db24f813b2a0658778c21d1))
* **inventory-service:** resolve duplicate Flyway V2 (C-1) + add CI version guard ([84fcba9](https://github.com/youssefnahdi23/SmartStock/commit/84fcba9ad20bd1a3534d54787a17216dfd74976a))
* **inventory:** optimistic locking prevents oversell/lost updates (C-3) ([aa90953](https://github.com/youssefnahdi23/SmartStock/commit/aa90953aea1d9552f078351359831872755efa13))
* **qa:** repair customer-service event-flow integration test (Phase 7) ([ac4a5dc](https://github.com/youssefnahdi23/SmartStock/commit/ac4a5dca063d5b1d7d9477233dfbc31fecde7632))
* **qa:** unblock Phase 7 test suite and reconcile coverage gate for CI/CD ([7d07fcd](https://github.com/youssefnahdi23/SmartStock/commit/7d07fcdd4409540c0fb2db9ced39456813f179d7))
* **services:** resolve cross-service query, schema, and IT-path defects ([1c9273a](https://github.com/youssefnahdi23/SmartStock/commit/1c9273aaf1eb71a3d216d34e5efe8373a402c952))
* **services:** resolve startup crashes and gateway routing (M-2 stabilization) ([396b2fc](https://github.com/youssefnahdi23/SmartStock/commit/396b2fc21e736bf01621f27e2e860e6c7e7ef6f4))
* **supplier:** align risk-assessment column types with entity (schema-validation) ([e68f26b](https://github.com/youssefnahdi23/SmartStock/commit/e68f26b743098db3911db7db857123bafbe8676e))
* **tests:** exclude TestContainers integration tests from default build; upgrade TC to 1.20.4 ([bfb253c](https://github.com/youssefnahdi23/SmartStock/commit/bfb253c71529bdbbf5b5d236bf2c4b25e91c8e99))


### Documentation

* add comprehensive infrastructure bootstrap summary ([a285901](https://github.com/youssefnahdi23/SmartStock/commit/a28590141126a1d1717ce836f364674b4ab5175e))
* add enterprise Event Catalog with 32+ domain events ([9d090af](https://github.com/youssefnahdi23/SmartStock/commit/9d090af181edca593978b7e7d7cf7d97e2eee504))
* add services API guide (HTML + PDF) ([1f12816](https://github.com/youssefnahdi23/SmartStock/commit/1f1281601f204b6725f798a3ee50377b474fdba0))
* Generate comprehensive microservice catalog documentation ([1698bf7](https://github.com/youssefnahdi23/SmartStock/commit/1698bf7085aa72b54c2e44cbe8dcd86cb9d3f32b))
* **release:** SmartStock AI 1.0.0 GA release documentation (Phase 10) ([5aa1ed3](https://github.com/youssefnahdi23/SmartStock/commit/5aa1ed324f2801cfdab574a0388bb03fe016d026))
* **review:** record all-services IT stabilization results (§7.4) ([e4723dd](https://github.com/youssefnahdi23/SmartStock/commit/e4723dd5ae7f7cdd3b9cbeaf1b7a5ff3cfa598a0))
* **reviews:** stabilization final report + progress closure ([9081b70](https://github.com/youssefnahdi23/SmartStock/commit/9081b701afcd2f41034f9a451b474fa6d0a03ff3))
* **roadmap:** mark Inventory Service implemented in M4 progress ([5605e14](https://github.com/youssefnahdi23/SmartStock/commit/5605e1488e8b727ee8888797856e816ca59b2598))
* **roadmap:** mark Warehouse Service implemented in M2 progress ([23a9467](https://github.com/youssefnahdi23/SmartStock/commit/23a946795717415c258d6382fb8e9b719e371491))

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
