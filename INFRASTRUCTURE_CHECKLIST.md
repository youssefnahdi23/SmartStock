# Infrastructure Verification Checklist

## Before Deployment

- [ ] `.env` file created from `.env.example`
- [ ] All required ports are available (see Port Mapping below)
- [ ] Docker and Docker Compose installed and running
- [ ] Disk space available: 10GB+ recommended
- [ ] Memory available: 4GB+ recommended

## Port Mapping

### Databases
- [ ] PostgreSQL (Identity): 5432
- [ ] PostgreSQL (Product): 5433
- [ ] PostgreSQL (Inventory): 5434
- [ ] PostgreSQL (Warehouse): 5435

### Caching & Storage
- [ ] Redis: 6379
- [ ] MinIO API: 9000
- [ ] MinIO Console: 9001

### Message Brokers
- [ ] Kafka: 9092, 29092
- [ ] Zookeeper: 2181
- [ ] RabbitMQ AMQP: 5672
- [ ] RabbitMQ Management: 15672

### Observability
- [ ] Prometheus: 9090
- [ ] Grafana: 3000
- [ ] Loki: 3100
- [ ] Tempo: 3200, 4317, 4318

### Administration & Tools
- [ ] pgAdmin: 5050
- [ ] Mailpit SMTP: 1025
- [ ] Mailpit Web: 8025

## Starting Infrastructure

```bash
# Check if ports are available
netstat -tuln | grep -E "(5432|6379|9092|3000|3100|3200)"

# Create environment file
cp .env.example .env

# Start all services
make docker-up
# or
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Verify all services are healthy
docker-compose ps --format "table {{.Names}}\t{{.Status}}"
```

## Service Health Verification

### PostgreSQL
```bash
docker exec smartstock-postgres-identity \
  pg_isready -U smartstock -d smartstock_identity
# Expected: accepting connections

docker exec smartstock-postgres-identity \
  psql -U smartstock -d smartstock_identity -c "SELECT version();"
```

### Redis
```bash
docker exec smartstock-redis \
  redis-cli -a smartstock123 ping
# Expected: PONG

docker exec smartstock-redis \
  redis-cli -a smartstock123 info server
```

### Kafka
```bash
docker exec smartstock-kafka \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092
# Expected: api_version
```

### RabbitMQ
```bash
docker exec smartstock-rabbitmq \
  rabbitmq-diagnostics ping
# Expected: successful

# Access Management UI
curl http://localhost:15672/api/health/ready
```

### Prometheus
```bash
curl http://localhost:9090/-/healthy
# Expected: 200 OK

# Check targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets'
```

### Grafana
```bash
curl http://localhost:3000/api/health
# Expected: database = ok

# Verify datasources
curl http://localhost:3000/api/datasources
```

### Loki
```bash
curl http://localhost:3100/ready
# Expected: 200 OK
```

### Tempo
```bash
curl http://localhost:3200/status
# Expected: 200 OK
```

### pgAdmin
```bash
curl http://localhost:5050/misc/ping
# Expected: 200 OK
```

### Mailpit
```bash
curl http://localhost:8025/api/v1/info
# Expected: 200 OK
```

## Configuration Verification

### Prometheus Configuration
```bash
curl http://localhost:9090/api/v1/query?query=up
# Should return metrics from scraped services
```

### Grafana Datasources
```bash
# Prometheus should be available
curl http://localhost:3000/api/datasources | jq '.[] | select(.type=="prometheus")'

# Loki should be available
curl http://localhost:3000/api/datasources | jq '.[] | select(.type=="loki")'

# Tempo should be available
curl http://localhost:3000/api/datasources | jq '.[] | select(.type=="tempo")'
```

## Database Initialization

```bash
# Connect to each database and verify schema
for port in 5432 5433 5434 5435; do
  docker exec smartstock-postgres-identity \
    psql -U smartstock -p $port -c "\dt"
done

# Expected: List of tables (initially empty for new databases)
```

## Troubleshooting

### Service Logs
```bash
# View logs for all services
docker-compose logs

# View logs for specific service
docker-compose logs postgres-identity
docker-compose logs prometheus
docker-compose logs grafana

# Follow logs in real-time
docker-compose logs -f [service]

# View last 100 lines
docker-compose logs --tail=100 [service]
```

### Resource Usage
```bash
# Check Docker resource usage
docker stats

# Check disk usage
docker system df

# Check network
docker network ls
docker network inspect smartstock-network
```

### Restart Services
```bash
# Restart specific service
docker-compose restart postgres-identity

# Restart all services
docker-compose restart

# Stop all services
docker-compose stop

# Start all services
docker-compose start
```

### Clean Environment
```bash
# Remove all containers (keep volumes)
docker-compose down

# Remove all containers and volumes
docker-compose down -v

# Remove unused resources
docker system prune
```

## Access Web Interfaces

### PostgreSQL Administration (pgAdmin)
- **URL**: http://localhost:5050
- **Email**: admin@smartstock.dev
- **Password**: admin123

**Steps**:
1. Login with email/password
2. Right-click "Servers" → "Register" → "Server"
3. Fill in server details for each PostgreSQL instance

### Grafana Dashboards
- **URL**: http://localhost:3000
- **Username**: admin
- **Password**: admin123

**Features**:
- View metrics from Prometheus
- View logs from Loki
- View traces from Tempo

### Prometheus Metrics
- **URL**: http://localhost:9090

**Features**:
- Query metrics: http://localhost:9090/graph
- View scraped targets: http://localhost:9090/targets
- View alerting rules: http://localhost:9090/alerts

### MinIO Console
- **URL**: http://localhost:9001
- **Access Key**: minioadmin
- **Secret Key**: minioadmin

**Features**:
- Create buckets
- Upload files
- Manage objects

### RabbitMQ Management
- **URL**: http://localhost:15672
- **Username**: guest
- **Password**: guest

**Features**:
- Queue management
- Connection monitoring
- Message tracking

### Mailpit Email Testing
- **URL**: http://localhost:8025

**Features**:
- View sent emails
- Test email notifications
- Web hook testing

## Performance Optimization

### For 4GB RAM System
```bash
# Reduce retention in prometheus.yml
--storage.tsdb.retention.time=1d

# Reduce Loki retention
retention_period: 24h

# Limit Tempo retention
retention_deletes_enabled: true
retention_period: 48h
```

### For Limited Disk Space
```bash
# Monitor disk usage
docker system df

# Clean up old images
docker image prune -a --filter "until=24h"

# Clean up stopped containers
docker container prune --filter "until=24h"
```

## Backup & Recovery

### Backup Volumes
```bash
# Backup database volumes
docker run --rm \
  -v smartstock-postgres-identity-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/postgres-identity.tar.gz -C /data .

# Backup Grafana data
docker run --rm \
  -v smartstock-grafana-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar czf /backup/grafana.tar.gz -C /data .
```

### Restore Volumes
```bash
# Restore database
docker run --rm \
  -v smartstock-postgres-identity-data:/data \
  -v $(pwd)/backups:/backup \
  alpine tar xzf /backup/postgres-identity.tar.gz -C /data

# Restart services
docker-compose restart postgres-identity
```

## Security Checklist

- [ ] Change default passwords before production
- [ ] Enable SSL/TLS for web interfaces
- [ ] Configure network policies
- [ ] Enable authentication on Prometheus
- [ ] Restrict access to admin interfaces
- [ ] Rotate credentials regularly
- [ ] Enable audit logging
- [ ] Monitor access logs

## Next Steps

1. [Deploy Microservices](../../docs/deployment/)
2. [Configure Observability](../../docs/standards/OBSERVABILITY_STANDARDS.md)
3. [Set Up Monitoring Alerts](infrastructure/prometheus.yml)
4. [Review Security](../../SECURITY.md)
5. [Production Deployment](../../docs/deployment/)
