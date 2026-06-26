# ADR-0017: Configuration Management - Externalized Configuration Across Environments

## Status
Accepted

## Context
SmartStock AI must run in multiple environments with different configurations:
- **Development**: Local developer machine (Kubernetes via Compose, local databases)
- **Staging**: Pre-production (Kubernetes, staging databases, staging credentials)
- **Production**: Enterprise customer deployment (Kubernetes, production databases, prod credentials)

Configuration includes:
- **Secrets**: Database passwords, API keys, JWT signing keys (sensitive)
- **Non-Secrets**: Service URLs, feature flags, database connection pool size, log levels
- **Environment-Specific**: Different AWS regions, different Redis clusters

Without centralized configuration:
- **Hardcoded Values**: Credentials baked into code (security risk)
- **Deployment Burden**: Rebuild images for each environment
- **Configuration Drift**: Different configs in different environments
- **Outages**: Forgot to update a config value in production

## Decision
Implement **12-Factor App Configuration with Spring Cloud Config**:

### 1. **Configuration Sources** (Priority Order)

**Priority 1: Environment Variables** (highest priority)
```bash
DATABASE_URL= <ENV_DB_URL>
DATABASE_PASSWORD= <ENV_DB_PASSWORD>
KAFKA_BROKERS= <ENV_KAFKA_BROKERS>
```

**Priority 2: Spring Cloud Config Server**
- Centralized configuration repository
- Different configs per environment (Git-based)
- Real-time updates without restart (for non-critical configs)

**Priority 3: Application properties files**
```yaml
# application.yml (defaults, no secrets)
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  mvc:
    log-request-details: false
    
logging:
  level:
    root: INFO
    com.smartstock: DEBUG
```

**Priority 4: Java System Properties**
```bash
-Dserver.port=8080 -Dspring.profiles.active=production
```

### 2. **Secrets Management**

**Never in Code**: Secrets never committed to Git
```bash
❌ WRONG: application.yml contains password
spring:
  datasource:
    password: "<DB_PASSWORD>"
```

**Option 1: Environment Variables (Simple)**
```bash
# .env file (local development only, NEVER in Git)
DATABASE_PASSWORD=<DB_PASSWORD>
REDIS_PASSWORD=<REDIS_PASSWORD>
JWT_SIGNING_KEY=<JWT_SECRET>

# Load in Docker Compose
services:
  inventory-service:
    environment:
      - DATABASE_PASSWORD=${DATABASE_PASSWORD}
```

**Option 2: Kubernetes Secrets (Production)**
```bash
# Create secret
kubectl create secret generic db-credentials \
  --from-literal=password=<DB_PASSWORD> \
  --from-literal=username=<DB_USERNAME>

# Reference in deployment
env:
- name: DATABASE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: password
```

**Option 3: HashiCorp Vault (Enterprise)**
```yaml
# Spring Cloud Vault integration
spring:
  cloud:
    vault:
      host: vault.smartstock.com
      port: 8200
      scheme: https
      authentication: kubernetes
      kv:
        engine-version: 2
        backend-path: secret
        profiles-separator: /
```

### 3. **Configuration Structure by Environment**

```
Git Repository: smartstock-config
├── application.yml           # Common config (all environments)
├── application-dev.yml       # Development overrides
├── application-staging.yml   # Staging overrides
└── application-prod.yml      # Production overrides

# Common defaults (no secrets)
application.yml:
spring:
  jpa:
    show-sql: false
  cache:
    type: redis
  
# Development overrides
application-dev.yml:
spring:
  jpa:
    show-sql: true            # Debug SQL in development
    hibernate:
      ddl-auto: create-drop   # Recreate schema on restart
  redis:
    host: localhost
    port: 6379

logging:
  level:
    com.smartstock: DEBUG      # More verbose logging

# Production overrides
application-prod.yml:
spring:
  jpa:
    hibernate:
      ddl-auto: validate       # Don't auto-modify production schema
  redis:
    host: redis-prod.internal
    port: 6379
  datasource:
    hikari:
      maximum-pool-size: 50    # More connections for production load

logging:
  level:
    com.smartstock: INFO       # Less verbose logging
```

### 4. **Feature Flags**

**Enable/Disable Features Without Redeployment**
```java
@Service
public class OrderService {
  @Value("${features.order-notifications.enabled:true}")
  private boolean orderNotificationsEnabled;
  
  public void createOrder(CreateOrderRequest request) {
    Order order = repository.save(new Order(request));
    
    if (orderNotificationsEnabled) {
      notificationService.sendOrderConfirmation(order);
    }
    
    return order;
  }
}
```

**Configure via environment variables**
```bash
FEATURES_ORDER_NOTIFICATIONS_ENABLED=false  # Disable notifications
FEATURES_BULK_IMPORT_ENABLED=true          # Enable new feature
```

### 5. **Configuration in Code**

**Spring Boot Configuration Classes**
```java
@Configuration
@ConfigurationProperties(prefix = "smartstock")
@Validated
public class SmartStockProperties {
  
  @Valid
  private Database database = new Database();
  
  @Valid
  private Kafka kafka = new Kafka();
  
  @Valid
  private Redis redis = new Redis();
  
  public static class Database {
    @NotBlank
    private String url;
    
    @NotBlank
    private String username;
    
    @Min(1) @Max(100)
    private int connectionPoolSize = 10;
    
    // getters/setters
  }
  
  public static class Kafka {
    @NotEmpty
    private List<String> brokers;
    
    @Min(1000)
    private int sessionTimeoutMs = 10000;
    
    // getters/setters
  }
  
  public static class Redis {
    @NotBlank
    private String host;
    
    @Min(1) @Max(65535)
    private int port = 6379;
    
    // getters/setters
  }
}
```

### 6. **Configuration Validation**

**Startup Validation**
```yaml
# application.yml
spring:
  config:
    import: optional:configserver:http://config-server:8888

smartstock:
  database:
    url: ${DATABASE_URL}          # Environment variable
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  kafka:
    brokers: ${KAFKA_BROKERS}
```

Service fails to start if:
- Required environment variables missing
- Invalid configuration values
- Configuration conflicts

## Alternatives Considered

### Option 1: Hardcoded Configuration
Values hardcoded in source code

**Pros:**
- Simple initially

**Cons:**
- Passwords exposed in Git (security risk)
- Rebuild needed for each environment
- Difficult to rotate credentials
- Unacceptable for enterprise

### Option 2: Configuration Management Tool (Consul, etcd)
External distributed config service

**Pros:**
- Sophisticated
- Real-time updates

**Cons:**
- Additional service to run
- Complexity
- Overkill for current phase

### Option 3: Manual Configuration Files
Manually create config files per environment

**Pros:**
- Simple

**Cons:**
- Configuration drift
- Error-prone
- No version control
- Difficult to troubleshoot

## Consequences

### Positive
- **Security**: Secrets never in code
- **Flexibility**: Different configs per environment
- **Scalability**: Configuration changes don't require rebuild
- **Auditability**: Git history tracks configuration changes
- **Compliance**: Meets regulatory requirements (PCI, GDPR)
- **Feature Control**: Enable/disable features per environment
- **Developer Experience**: Easy local development setup
- **Production Safety**: Production config isolated from development

### Negative
- **Complexity**: Managing multiple configurations
- **Configuration Drift**: Different environments may diverge
- **Debugging Difficulty**: Hard to tell which config is active
- **Secrets Management**: Must protect secrets carefully
- **Operational Burden**: Must manage config servers and version control

### Trade-offs
- **Simplicity vs. Security**: Accept complexity for security
- **Flexibility vs. Consistency**: Accept config variations for environment needs

## Future Considerations

1. **Dynamic Configuration**: Hot reload configs without restart
   - Feature flags changed without rebuild
   - Gradual rollout of configuration changes

2. **Configuration Audit**: Track who changed what when
   - Git commits track changes
   - Signed commits for prod changes
   - Approval workflows for production changes

3. **Configuration Encryption**: Encrypt sensitive values at rest
   - Git-encrypted secrets
   - Encrypted in Spring Cloud Config

4. **Compliance Reporting**: Prove configuration compliance
   - Generate audit reports
   - Show who accessed secrets
   - Prove password rotation

## Implementation Guidance

- All microservices must externalize configuration
- Environment variables take precedence
- Provide sensible defaults in application.yml
- Never commit secrets to Git
- Use .gitignore to exclude local config files
- Document all configuration options in service README
- Validate configuration on startup (fail fast)
- Use Spring Cloud Config for centralized management
- Kubernetes Secrets for production secret management
- Feature flags via environment variables or config server
- Regular password rotation (quarterly minimum)
