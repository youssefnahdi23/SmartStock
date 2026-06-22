# SmartStock Development Infrastructure Guide

## Overview

This directory contains all infrastructure configuration files for the SmartStock development environment.

- **docker-compose.yml** - Defines all services
- **prometheus.yml** - Prometheus metrics configuration
- **loki-config.yml** - Loki log aggregation configuration
- **tempo-config.yml** - Tempo distributed tracing configuration
- **grafana/provisioning/** - Grafana datasources and dashboards

## Services

### Databases (PostgreSQL)

Four PostgreSQL instances for microservices:

```
postgres-identity:8   Port 5432
postgres-product:     Port 5433
postgres-inventory:   Port 5434
postgres-warehouse:   Port 5435
```

**Connection String**: `postgresql://{user}:{password}@{host}:{port}/{database}`

**Environment Variables**:
- `DB_USER` - Database user (default: smartstock)
- `DB_PASSWORD` - Database password (default: smartstock123)

### Caching & Sessions

**Redis**
- **Port**: 6379
- **Password**: `${REDIS_PASSWORD}`
- **Environment Variable**: `REDIS_PASSWORD` (default: smartstock123)

### Message Brokers

#### Kafka (Recommended for Events)
- **Port**: 9092 (external), 29092 (internal)
- **Zookeeper**: Port 2181
- **Environment Variables**: `LOG_LEVEL`

#### RabbitMQ (Alternative)
- **AMQP Port**: 5672
- **Management UI**: http://localhost:15672
- **Credentials**: `${RABBITMQ_USER}:${RABBITMQ_PASSWORD}`
- **Environment Variables**:
  - `RABBITMQ_USER` (default: guest)
  - `RABBITMQ_PASSWORD` (default: guest)

### Object Storage

**MinIO** (S3-compatible)
- **API Port**: 9000
- **Console Port**: 9001 (http://localhost:9001)
- **Credentials**: `${MINIO_USER}:${MINIO_PASSWORD}`
- **Environment Variables**:
  - `MINIO_USER` (default: minioadmin)
  - `MINIO_PASSWORD` (default: minioadmin)

## Observability Stack

### Metrics (Prometheus)

**Port**: 9090
**URL**: http://localhost:9090

**Configuration**:
- Scrape interval: 15 seconds
- Retention: 7 days (configurable)
- Metrics Path: `/actuator/prometheus`

**Configured Services**:
- identity-service (8081)
- product-service (8082)
- inventory-service (8083)
- warehouse-service (8084)

### Dashboards (Grafana)

**Port**: 3000
**URL**: http://localhost:3000

**Credentials**:
- Username: `${GRAFANA_USER}` (default: admin)
- Password: `${GRAFANA_PASSWORD}` (default: admin123)

**Datasources**:
- Prometheus (metrics)
- Loki (logs)
- Tempo (traces)

### Logging (Loki)

**Port**: 3100
**URL**: http://localhost:3100

**Configuration**:
- Retention: 7 days (configurable)
- Query interface: Via Grafana

### Tracing (Tempo)

**Port**: 3200
**OTLP Receiver**:
- gRPC: Port 4317
- HTTP: Port 4318

**Configuration**:
- Retention: 10 days (configurable)
- Query interface: Via Grafana

## Administration

### pgAdmin (PostgreSQL Administration)

**Port**: 5050
**URL**: http://localhost:5050

**Credentials**:
- Email: `${PGADMIN_EMAIL}` (default: admin@smartstock.dev)
- Password: `${PGADMIN_PASSWORD}` (default: admin123)

**Pre-configured Servers**:
- postgres-identity (5432)
- postgres-product (5433)
- postgres-inventory (5434)
- postgres-warehouse (5435)

### Mailpit (Email Testing)

**SMTP Port**: 1025
**Web UI Port**: 8025
**URL**: http://localhost:8025

**Configuration**:
- Accept any auth: true
- Allow insecure auth: true
- No credentials required for testing

## Quick Start

### 1. Start All Services

```bash
make docker-up
# or
docker-compose up -d
```

### 2. Verify All Services

```bash
docker-compose ps
```

### 3. Access Web Interfaces

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin123 |
| pgAdmin | http://localhost:5050 | admin@smartstock.dev / admin123 |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| RabbitMQ | http://localhost:15672 | guest / guest |
| Mailpit | http://localhost:8025 | (none) |
| Prometheus | http://localhost:9090 | (none) |
| Loki | http://localhost:3100 | (none) |

## Environment Variables

### Database

```bash
DB_USER=smartstock
DB_PASSWORD=smartstock123
```

### Cache

```bash
REDIS_PASSWORD=smartstock123
```

### Message Brokers

```bash
LOG_LEVEL=INFO
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

### Storage

```bash
MINIO_USER=minioadmin
MINIO_PASSWORD=minioadmin
```

### Administration

```bash
PGADMIN_EMAIL=admin@smartstock.dev
PGADMIN_PASSWORD=admin123
GRAFANA_USER=admin
GRAFANA_PASSWORD=admin123
```

### Observability

```bash
LOG_LEVEL=INFO
PROMETHEUS_RETENTION_DAYS=7
LOKI_RETENTION_DAYS=7
TEMPO_RETENTION_HOURS=168
TZ=UTC
```

## Configuration Files

### Prometheus (prometheus.yml)

Scrapes metrics from microservices on `/actuator/prometheus` endpoint.

**Default Scrape Jobs**:
- prometheus (self-monitoring)
- identity-service
- product-service
- inventory-service
- warehouse-service

**To Add New Service**:
```yaml
- job_name: 'new-service'
  static_configs:
    - targets: ['new-service:8085']
  metrics_path: '/actuator/prometheus'
```

### Loki (loki-config.yml)

Aggregates logs from all services.

**Features**:
- 7-day retention
- Local filesystem storage
- Snappy compression

### Tempo (tempo-config.yml)

Collects distributed traces from services using OpenTelemetry.

**Receivers**:
- gRPC: :4317
- HTTP: :4318

**To Export Traces from Services**:
```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4317
OTEL_TRACES_EXPORTER=otlp
```

### Grafana (grafana/provisioning/)

**datasources.yml**: Automatically configures Prometheus, Loki, Tempo

**dashboards.yml**: Loads dashboards from `/var/lib/grafana/dashboards`

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs [service-name]

# Verify Docker resources
docker stats
```

### Database Connection Issues

```bash
# Test connection
docker exec smartstock-postgres-identity \
  psql -U smartstock -d smartstock_identity -c "SELECT 1"
```

### Grafana Datasources Not Available

1. Verify Prometheus is running: `curl http://localhost:9090`
2. Verify Loki is running: `curl http://localhost:3100/ready`
3. Verify Tempo is running: `curl http://localhost:3200/status`

### RabbitMQ Management UI Not Accessible

1. Verify container is running: `docker ps | grep rabbitmq`
2. Wait 10 seconds after startup for management plugin
3. Access at http://localhost:15672

## Production Deployment

For production, see:
- [docs/deployment/](../deployment/)
- [ADR-0010-deployment-kubernetes.md](../../docs/decisions/ADR-0010-deployment-kubernetes.md)

**Key Differences**:
- Use managed services (AWS RDS, Azure CosmosDB, etc.)
- Use Kubernetes for orchestration
- Use HashiCorp Vault for secrets
- Configure persistent volumes properly
- Use external observability services (DataDog, New Relic, etc.)

## Security Notes

**Development Only**:
- Default credentials used
- No authentication on some services
- Secrets in .env file (never commit!)

**Before Production**:
- Change all default passwords
- Enable authentication on services
- Use secure communication (TLS/HTTPS)
- Store secrets in HashiCorp Vault
- Implement network policies

## Cleanup

### Stop All Services

```bash
make docker-down
# or
docker-compose down
```

### Remove All Data

```bash
make docker-clean
# or
docker-compose down -v
```

**Warning**: This deletes all database data, logs, and traces!

## Related Documentation

- [Makefile](../../Makefile) - Build commands
- [docker-compose.yml](../../docker-compose.yml) - Service definitions
- [QUICK_START_GUIDE.md](../../QUICK_START_GUIDE.md) - Development setup
- [docs/deployment/](../deployment/) - Production deployment

## Support

For issues or questions:
1. Check [CONTRIBUTING.md](../../CONTRIBUTING.md)
2. Review service logs: `docker-compose logs [service]`
3. Check [SECURITY.md](../../SECURITY.md) for security issues
