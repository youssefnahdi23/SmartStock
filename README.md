# SmartStock AI - Enterprise Inventory Intelligence Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21+-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

SmartStock AI is a **commercial-grade Enterprise Inventory Intelligence Platform** designed for scalability, maintainability, and future AI integration. It's built using **microservices architecture**, **event-driven patterns**, and **Domain-Driven Design** principles.

## 🎯 Project Vision

**Phase 1**: Enterprise Inventory Platform  
**Phase 2**: Business Analytics Platform  
**Phase 3**: Data Platform  
**Phase 4**: AI Platform (forecasting, optimization, recommendations)

The system is architected to be an **enterprise-grade data generation machine** that produces structured, high-quality business data. AI is never core business logic—it consumes data and provides insights.

## 🏗️ Architecture

### Microservices (12 Services)

| Service | Purpose | Database | Events |
|---------|---------|----------|--------|
| **Identity Service** | Authentication, RBAC, JWT | PostgreSQL | `UserCreated`, `UserAuthenticated` |
| **Product Service** | Product catalog, metadata | PostgreSQL | `ProductCreated`, `ProductUpdated` |
| **Inventory Service** | Stock tracking, movements | PostgreSQL | `StockIn`, `StockOut`, `InventoryAdjusted` |
| **Warehouse Service** | Multi-warehouse management | PostgreSQL | `WarehouseCreated`, `StockTransferred` |
| **Supplier Service** | Supplier profiles, performance | PostgreSQL | `SupplierCreated`, `DeliveryRegistered` |
| **Customer Service** | Customer management, profiles | PostgreSQL | `CustomerCreated`, `CustomerUpdated` |
| **Order Service** | Purchase & sales orders | PostgreSQL | `OrderCreated`, `OrderCompleted` |
| **Audit Service** | Immutable audit logs | PostgreSQL | Consumes all events |
| **Notification Service** | Alerts, emails, SMS | PostgreSQL | Triggered by events |
| **Reporting Service** | Business reports & dashboards | PostgreSQL | Aggregates data from events |
| **Analytics Service** | Analytics engine (non-AI) | PostgreSQL | Processes historical data |
| **Data Export Service** | Export to data lake | PostgreSQL + MinIO | Exports datasets for AI |

### Technology Stack

**Backend**:
- Java 21 + Spring Boot 3.3.1 LTS
- Spring Data JPA + Hibernate
- PostgreSQL (per-service database)
- Kafka for event streaming
- Redis for caching & sessions
- MinIO for object storage (S3-compatible)

**Desktop**:
- JavaFX (native, performant)
- MVVM architecture
- Offline caching support

**Infrastructure**:
- Docker & Docker Compose (dev)
- Kubernetes + Helm (production)
- Prometheus + Grafana (monitoring)
- OpenTelemetry (distributed tracing)
- Flyway (database migrations)

## 📋 Quick Start

### Prerequisites

```bash
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Git
```

### Development Setup (2 minutes)

```bash
# Clone repository
git clone https://github.com/youssefnahdi23/SmartStock.git
cd SmartStock

# One-command setup
make quick-start

# This will:
# 1. Verify Java 21+ and Maven
# 2. Create .env file with defaults
# 3. Start all Docker services (PostgreSQL, Kafka, Redis, MinIO)
# 4. Display service endpoints
```

### Manual Build & Test

```bash
# Build all services
make build

# Run tests
make test

# Generate coverage report
make test-coverage

# View Docker logs
make docker-logs

# Stop services
make docker-down
```

### Available Commands

```bash
make help              # Show all available commands
make setup             # Setup development environment
make build             # Build all services
make test              # Run all tests
make docker-up         # Start services
make docker-down       # Stop services
make docker-clean      # Remove services and volumes
make format            # Format code
make lint              # Run code quality checks
make docs              # Generate Javadoc
```

See [Makefile](Makefile) for complete list of commands.

## 📚 Documentation

### For Developers

- **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute, code style, PR process
- **[QUICK_START_GUIDE.md](QUICK_START_GUIDE.md)** - Detailed development setup
- **[README_IMPLEMENTATION.md](README_IMPLEMENTATION.md)** - Implementation guide & architecture
- **[IMPLEMENTATION_PHASE1_SUMMARY.md](IMPLEMENTATION_PHASE1_SUMMARY.md)** - Phase 1 completion details

### Architecture Decisions (ADRs)

All architectural decisions are documented as Architecture Decision Records (ADRs). These are **authoritative**—all code must comply.

```
docs/decisions/
├── ADR-0001-microservices-architecture.md     (Why microservices?)
├── ADR-0002-rest-event-driven-communication.md (REST + Kafka)
├── ADR-0003-database-per-service.md            (No shared DB)
├── ADR-0004-kafka-event-broker.md              (Event streaming)
├── ADR-0005-jwt-rbac-authentication.md         (Auth strategy)
├── ADR-0006-data-lake-ai-readiness.md         (AI data pipeline)
├── ADR-0007-javafx-desktop-app.md             (Desktop UI)
├── ADR-0008-api-gateway.md                    (API management)
├── ADR-0009-observability.md                  (Logging & monitoring)
├── ADR-0010-deployment-kubernetes.md          (Deployment)
├── ADR-0011-redis-caching.md                  (Caching strategy)
├── ADR-0012-domain-driven-design-bounded-contexts.md (DDD)
├── ADR-0013-resilience-patterns-circuit-breakers-retries.md (Resilience)
├── ADR-0014-testing-strategy.md               (Testing)
├── ADR-0015-saga-pattern-distributed-transactions.md (Transactions)
├── ADR-0016-api-versioning.md                 (API versions)
└── ADR-0017-configuration-management.md       (Config)
```

**Important**: Before implementing features, check relevant ADRs. Implementation must follow recorded decisions.

### Engineering Standards

```
docs/standards/
├── CODE_STYLE_GUIDE.md
├── SECURITY_STANDARDS.md
├── DATABASE_STANDARDS.md
├── API_STANDARDS.md
├── TESTING_STANDARDS.md
└── DEPLOYMENT_STANDARDS.md
```

## 🔐 Security

- **Authentication**: JWT with refresh token rotation
- **Authorization**: Role-Based Access Control (RBAC)
- **Passwords**: BCrypt (cost factor 12+)
- **Database**: Parameterized queries, soft delete, audit logging
- **APIs**: HTTPS/TLS, rate limiting, CORS restricted
- **Secrets**: Environment variables only, never committed

⚠️ **Security vulnerabilities**: Email security@smartstock.dev (not public GitHub issues)

See [SECURITY.md](SECURITY.md) for detailed security guidelines.

## 📊 Database Schema

Each service owns its database (PostgreSQL). No shared tables between services.

Example (Identity Service):
```sql
-- Tables
- users (aggregate root)
- roles (RBAC)
- permissions (granular access)
- refresh_tokens (secure token rotation)
- audit_logs (immutable)

-- Indexes optimized for queries
-- Foreign keys for referential integrity
-- Soft delete with audit fields
```

Migrations use Flyway (automatic on startup).

## 🎛️ Infrastructure

### Development (Docker Compose)

```bash
docker-compose up -d

# Services:
- PostgreSQL (4 instances) on ports 5432-5435
- Kafka on port 9092
- Zookeeper on port 2181
- Redis on port 6379
- MinIO on port 9000 (S3-compatible)
```

### Production (Kubernetes)

See [ADR-0010-deployment-kubernetes.md](docs/decisions/ADR-0010-deployment-kubernetes.md)

```bash
# Deploy to K8s cluster
helm install smartstock ./helm/smartstock \
  --namespace smartstock \
  --values values-prod.yaml
```

## 🧪 Testing

Target coverage: **80% minimum**, **90% for critical business logic**

```bash
# Run all tests
make test

# Generate coverage report
make test-coverage

# Run integration tests
make integration-test

# View coverage
open target/site/jacoco/index.html
```

## 🚀 Deployment

### Local Development
```bash
make quick-start
make build
make test
```

### Staging/Production
See [DEPLOYMENT_STANDARDS.md](docs/standards/DEPLOYMENT_STANDARDS.md)

## 🤝 Contributing

We welcome contributions! Please read [CONTRIBUTING.md](CONTRIBUTING.md) first.

1. Check [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
2. Review relevant ADRs in `/docs/decisions/`
3. Follow [CONTRIBUTING.md](CONTRIBUTING.md) guidelines
4. Create a feature branch
5. Submit a pull request

## 📜 License

This project is licensed under the [MIT License](LICENSE).

## 📞 Support

### Documentation
- [SmartStock Instructions](/.github/instructions/SmartStock.instructions.md)
- [Cahier de Charge](/.github/instructions/CDC.instructions.md)
- [Quick Start Guide](QUICK_START_GUIDE.md)

### Questions?
1. Check documentation first
2. Search GitHub issues
3. Ask in discussions
4. Open an issue with detailed context

## 🎓 Learning Resources

### Microservices
- [Building Microservices - Sam Newman](https://samnewman.io/books/building_microservices_2nd_edition/)
- [Microservices Patterns - Chris Richardson](https://microservices.io/)

### Domain-Driven Design
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design - Vaughn Vernon](https://vaughnvernon.com/)

### Event-Driven Architecture
- [Building Event-Driven Microservices - Adam Bellemare](https://www.oreilly.com/library/view/building-event-driven-microservices/9781492068052/)

### Spring Boot
- [Spring in Action - Craig Walls](https://www.manning.com/books/spring-in-action-sixth-edition)
- [Official Spring Boot Docs](https://spring.io/projects/spring-boot)

## 🗺️ Project Roadmap

### ✅ Phase 1 (Current)
- [x] Microservices foundation
- [x] Identity Service (auth, RBAC)
- [x] Database migrations
- [x] Event infrastructure
- [ ] All 12 services implemented
- [ ] End-to-end tests
- [ ] API Gateway
- [ ] Desktop client

### 🔄 Phase 2 (Planned)
- [ ] Analytics engine
- [ ] Business dashboards
- [ ] Historical data aggregation
- [ ] KPI calculations

### 🔮 Phase 3 (Future)
- [ ] Data lake (MinIO)
- [ ] Data pipeline
- [ ] Feature store

### 🤖 Phase 4 (Future)
- [ ] AI/ML models
- [ ] Demand forecasting
- [ ] Stock optimization
- [ ] Supplier recommendations

## 📊 Project Status

- **Repository**: [youssefnahdi23/SmartStock](https://github.com/youssefnahdi23/SmartStock)
- **License**: MIT
- **Java Version**: 21+
- **Spring Boot**: 3.3.1 LTS
- **Status**: Active Development (Phase 1)

---

**Last Updated**: June 2026  
**Maintained by**: Youssef Nahdi ([@youssefnahdi23](https://github.com/youssefnahdi23))