# ADR-0010: Deployment Strategy - Docker and Kubernetes for Enterprise Scalability

## Status
Accepted

## Context
SmartStock AI must be deployable across different environments:
- **Development**: Developers run services locally
- **Staging**: Pre-production environment for testing
- **Production**: Enterprise deployment for customers

The deployment strategy must support:
- **Consistency**: Same code runs in dev, staging, and production
- **Scalability**: Services scale from 1 to 100+ instances
- **Reliability**: Automatic recovery from failures
- **Speed**: Deploy new versions quickly without downtime
- **Observability**: Monitor and debug deployed services
- **Multi-Tenant**: Support multiple customer instances
- **Cost Efficiency**: Don't pay for resources not being used

The choice between Docker/Compose, VMs, and Kubernetes significantly impacts scalability and operational burden.

## Decision
Implement **Multi-Tier Deployment Strategy**:

### 1. **Phase 1 & 2: Docker Compose for Development and Small Deployments**

**Target**: Startups, small enterprises, development teams

**Infrastructure**
- Single server (minimum 8 CPU, 16GB RAM, 200GB SSD)
- Docker Compose orchestration
- PostgreSQL, Redis, Kafka, MinIO on same server
- Monitoring with Prometheus + Grafana (optional)

**Deployment Process**
```bash
# Build images
docker build -t smartstock/inventory-service:v1.0.0 services/inventory/

# Deploy with compose
docker-compose up -d

# Scale if needed
docker-compose up -d --scale inventory-service=3
```

**Advantages**
- Simple to understand and deploy
- Low operational overhead
- Suitable for single-location deployments
- Easy debugging (services on same network)
- Cost-effective for small scale

**Limitations**
- No automatic failure recovery
- Limited to single server (or manual cluster setup)
- No load balancing between services
- Manual scaling (no auto-scaling)
- Limited multi-region support

### 2. **Phase 3: Kubernetes for Enterprise Deployments**

**Target**: Large enterprises, mission-critical deployments

**Infrastructure**
- Kubernetes cluster (3+ master nodes, 5+ worker nodes)
- Managed service (EKS, AKS, GKE) or self-hosted
- PostgreSQL clusters with replication
- Kafka cluster with 3+ brokers
- Persistent storage (EBS, managed disks)
- Load balancing (cloud LB or nginx ingress)

**Deployment Process (via Helm)**
```bash
# Package services as Helm charts
helm package services/inventory/

# Deploy to cluster
helm install smartstock-inventory ./inventory-1.0.0.tgz -n smartstock

# Upgrade to new version
helm upgrade smartstock-inventory ./inventory-1.0.1.tgz -n smartstock

# Auto-scaling
kubectl autoscale deployment inventory-service --min=2 --max=10 --cpu-percent=70
```

**Key Features**
- **Self-Healing**: Automatic restart of failed containers
- **Load Balancing**: Distribute traffic across instances
- **Rolling Updates**: Deploy new versions without downtime
- **Auto-Scaling**: Scale based on CPU/memory/custom metrics
- **Multi-Zone**: Spread across availability zones for redundancy
- **Secrets Management**: Kubernetes Secrets for credentials
- **Network Policies**: Fine-grained network access control
- **Service Mesh Ready**: Can integrate with Istio/Linkerd later

### 3. **Containerization Strategy**

**Base Images**
```dockerfile
# Inventory Service Dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy executable JAR
COPY target/inventory-service-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run as non-root user
USER nobody

# JVM options
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Xmx1g -Xms512m"

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Best Practices**
- Alpine Linux base image (small, secure)
- JDK 21 (latest LTS)
- Non-root user (security)
- Health checks enabled
- Optimized JVM settings (ZGC, memory tuning)
- Layer caching for faster builds

**Image Registry**
- Private Docker registry (ECR, ACR, Harbor)
- Image signing for security
- Automated scanning for vulnerabilities
- Tag with semantic versioning (v1.0.0)
- Keep images small (< 500MB per image)

### 4. **Infrastructure as Code**

**Kubernetes Manifests** (for Phase 3)
```yaml
---
apiVersion: v1
kind: Namespace
metadata:
  name: smartstock
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-service
  namespace: smartstock
spec:
  replicas: 3
  selector:
    matchLabels:
      app: inventory-service
  template:
    metadata:
      labels:
        app: inventory-service
    spec:
      containers:
      - name: inventory-service
        image: smartstock/inventory-service:v1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: host
        - name: KAFKA_BROKERS
          value: "kafka-0:9092,kafka-1:9092,kafka-2:9092"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: inventory-service
  namespace: smartstock
spec:
  selector:
    app: inventory-service
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

**Helm Chart** (templated Kubernetes manifests)
- Enables parameterized deployments
- Supports environment-specific values
- Dependency management
- Easy rollback

### 5. **Database Deployment**

**Development**: Single PostgreSQL container (Docker)
**Staging**: PostgreSQL with replication (2 replicas)
**Production**: Managed service (AWS RDS, Azure Database) or self-hosted cluster

**Backup Strategy**
- Daily automated backups
- 7-day retention for operational databases
- 1-year retention for audit logs
- Point-in-time recovery capability
- Off-site backup replication

### 6. **Deployment Workflow**

```
1. Developer commits code
   ↓
2. CI/CD Pipeline triggered (GitHub Actions, GitLab CI, Jenkins)
   ├─ Build Docker image
   ├─ Run unit tests
   ├─ Run integration tests
   ├─ Scan for vulnerabilities
   ├─ Push to registry
   ↓
3. Automatic deployment to staging
   ├─ Deploy via Docker Compose or Helm
   ├─ Run smoke tests
   ├─ Run performance tests
   ↓
4. Manual approval required
   ↓
5. Deploy to production
   ├─ Rolling update (0 downtime)
   ├─ Monitor for issues
   ├─ Automatic rollback if errors detected
```

## Alternatives Considered

### Option 1: Virtual Machines Only
Deploy to VMs without containers

**Pros:**
- Familiar to traditional DevOps teams
- Good isolation between services

**Cons:**
- Resource overhead (OS per service)
- Slow to provision
- Complex image management
- Difficult to scale dynamically
- Higher costs

### Option 2: Kubernetes Only (Skip Docker Compose)
Use Kubernetes from day one

**Pros:**
- Enterprise-grade from start
- Unified deployment model

**Cons:**
- Overengineering for early stage
- High operational complexity
- Expensive to run (minimum cluster cost)
- Steep learning curve
- Overkill for development/staging

### Option 3: Serverless (AWS Lambda, Google Cloud Functions)
Deploy services as serverless functions

**Pros:**
- Auto-scaling built-in
- Pay per invocation
- No infrastructure management

**Cons:**
- Cold start latency (problematic for real-time inventory)
- Difficult to maintain stateful services (Kafka consumers)
- Vendor lock-in
- Not suitable for background workers
- Limited execution time per invocation

## Consequences

### Positive
- **Development to Production Parity**: Same Docker image runs everywhere
- **Horizontal Scalability**: Services scale easily with Kubernetes (Phase 3)
- **Rapid Deployment**: Deploy new versions in minutes
- **Self-Healing**: Kubernetes automatically restarts failed containers
- **Resource Efficiency**: Containers overhead minimal compared to VMs
- **Cost Effective**: Docker Compose cheap initially; Kubernetes cost-effective at scale
- **Team Flexibility**: Developers can run full stack locally
- **Operational Simplicity**: Infrastructure as Code makes deployments reproducible
- **Multi-Region Ready**: Kubernetes enables multi-region deployments
- **Disaster Recovery**: Kubernetes enables cross-region failover

### Negative
- **Learning Curve**: Docker and Kubernetes require new skills
- **Operational Overhead**: More services to monitor (Docker daemon, Kubernetes master)
- **Debugging Difficulty**: Distributed execution harder to debug
- **Network Complexity**: Container networking more complex than VMs
- **State Management**: Stateful services (databases) harder in containers
- **Resource Limits**: Must pre-allocate resources; can waste capacity
- **Initial Investment**: Phase 1 uses Docker Compose but must plan Kubernetes migration
- **Security Attack Surface**: More components = more things to secure

### Trade-offs
- **Simplicity vs. Scalability**: Docker Compose simpler now; Kubernetes scales later
- **Cost vs. Availability**: Docker Compose cheaper; Kubernetes more available
- **Development Speed vs. Operations Burden**: Docker Compose faster to develop; Kubernetes handles production burden

## Future Considerations

1. **Service Mesh Integration** (Phase 4): Add Istio or Linkerd
   - Advanced traffic management
   - Automatic retry and timeout handling
   - Distributed tracing at infrastructure level
   - Security policies (mTLS, network policies)

2. **GitOps Deployment**: Use tools like ArgoCD or Flux
   - Git as single source of truth
   - Automatic sync between Git and cluster
   - Pull-based deployments (more secure)
   - Easy rollback via Git commits

3. **Progressive Delivery**: Canary and blue-green deployments
   - Deploy to 10% of traffic first
   - Monitor for errors
   - Gradually increase traffic
   - Automatic rollback if error rate increases

4. **Cost Optimization**: RI reservations, spot instances, autoscaling
   - Reserve capacity for baseline load
   - Use spot instances for burst capacity
   - Scheduled scaling for predictable patterns

5. **Multi-Region Deployment**: Global redundancy
   - Active-active deployments across regions
   - Cross-region data replication
   - Global load balancing

6. **Compliance and Security**: Scan and enforce policies
   - Image vulnerability scanning (Trivy, Snyk)
   - Runtime security monitoring
   - Pod security policies
   - Network policies enforcement

## Implementation Guidance

- Phase 1: Use docker-compose.yml in repository root
- Phase 2: Add Kubernetes manifests in k8s/ directory
- Phase 3: Migrate to Helm charts for multi-environment management
- All services build to Docker image automatically via CI/CD
- Container images versioned with semantic versioning
- Blue-green deployments for zero-downtime updates
- Health checks implemented in all services
- Secrets never stored in Docker images; use runtime injection
- Logs streamed to stdout (collected by container orchestration)
- Resource limits set on all containers (prevent noisy neighbor)
