# SmartStock AI - Quick Start Guide

## Current Status: Foundation Infrastructure Complete ✓

### What's Been Done
- ✅ Parent project structure (pom.xml) with Java 21, Spring Boot 3.3.1
- ✅ Common module for shared utilities (ApiResponse, DomainEvent)
- ✅ Identity Service domain models (User, Role, Permission, RefreshToken)
- ✅ 4 Repository interfaces (fully optimized with named parameters)
- ✅ 2 Domain events (UserCreatedEvent, UserAuthenticatedEvent)
- ✅ Complete Flyway database migration (V1__initial_schema.sql)
- ✅ Spring Boot configuration (application.yml)
- ✅ Bootstrap application class

### What's Needed Next

#### Phase 1A: Complete Identity Service (HIGHEST PRIORITY)

**Step 1: JWT Infrastructure** (Create these files)
```
services/identity-service/src/main/java/com/smartstock/identity/infrastructure/
├── security/
│   ├── JwtProvider.java (Generate and sign tokens)
│   ├── JwtValidator.java (Validate and extract claims)
│   └── JwtProperties.java (Configuration class)
└── config/
    └── SecurityConfiguration.java (Spring Security setup)
```

**Step 2: Application Services** (Create these files)
```
services/identity-service/src/main/java/com/smartstock/identity/application/
├── service/
│   ├── UserService.java (CRUD operations)
│   ├── AuthenticationService.java (Login, register, logout)
│   ├── TokenService.java (Token generation, refresh)
│   └── RoleService.java (Role and permission management)
└── dto/
    ├── LoginRequestDto.java
    ├── RegisterRequestDto.java
    ├── TokenResponseDto.java
    ├── UserResponseDto.java
    └── (Create mappers for each)
```

**Step 3: REST Controllers** (Create these files)
```
services/identity-service/src/main/java/com/smartstock/identity/interfaces/
├── controller/
│   ├── AuthController.java (POST /auth/register, /auth/login, /auth/refresh, /auth/logout)
│   ├── UserController.java (GET /users/{id}, POST /users, PUT /users/{id}, DELETE /users/{id})
│   └── RoleController.java (Admin endpoints for role management)
└── exception/
    ├── GlobalExceptionHandler.java
    ├── BusinessException.java
    ├── ResourceNotFoundException.java
    ├── InvalidCredentialsException.java
    └── UserAlreadyExistsException.java
```

**Step 4: Tests** (Create these files)
```
services/identity-service/src/test/java/com/smartstock/identity/
├── domain/repository/
│   └── UserRepositoryTest.java (Test repository queries)
├── application/service/
│   ├── UserServiceTest.java
│   ├── AuthenticationServiceTest.java
│   └── TokenServiceTest.java
├── interfaces/controller/
│   └── AuthControllerTest.java
└── integration/
    └── AuthenticationIntegrationTest.java (End-to-end flow)
```

---

## Prerequisites to Install

```bash
# Java 21
java -version  # Should show Java 21+

# Maven 3.8.1+
mvn -version

# PostgreSQL 12+
psql --version

# Git (for version control)
git --version
```

---

## Setup Instructions

### 1. Database Setup
```bash
# Create database
createdb smartstock_identity

# Create user
createuser smartstock
psql -c "ALTER USER smartstock WITH PASSWORD 'smartstock';"

# Grant privileges
psql -c "GRANT ALL PRIVILEGES ON DATABASE smartstock_identity TO smartstock;"
```

### 2. Build Project
```bash
cd /home/youssef/Projects/SmartStock
mvn clean install -DskipTests

# If building just Identity Service:
cd services/identity-service
mvn clean package -DskipTests
```

### 3. Run Identity Service
```bash
cd services/identity-service
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8081"
```

### 4. Test API Endpoints
```bash
# Register user
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "password": "SecurePassword123!"
  }'

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123!"
  }'

# Refresh token
curl -X POST http://localhost:8081/api/v1/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

---

## Architecture Reminder

### Service Boundaries (Respect These!)
- **Identity Service** owns user authentication
- **Other services** must call Identity Service to verify tokens
- **NO direct database access** between services
- **All communication** through REST APIs or Kafka events

### Layering (Follow This Pattern)
```
interfaces/  ← REST controllers, DTOs, exception handling
    ↓
application/ ← Business logic, service orchestration
    ↓
domain/      ← Domain models, repositories, events
    ↓
infrastructure/ ← Security, external APIs, persistence
```

### Event Publishing
Every important user action should emit an event to Kafka:
```java
// Example in UserService
UserCreatedEvent event = new UserCreatedEvent(
    user.getId(),
    user.getUsername(),
    user.getEmail(),
    user.getFirstName(),
    user.getLastName(),
    "identity-service"
);
eventPublisher.publishEvent(event);
```

---

## Code Quality Standards

### Before Committing
1. ✅ All tests pass locally
2. ✅ Code coverage > 80%
3. ✅ No hardcoded values (use configuration)
4. ✅ No sensitive data in logs
5. ✅ Follow naming conventions (camelCase for variables, PascalCase for classes)
6. ✅ Add JavaDoc for public classes/methods
7. ✅ Use constructor injection (no @Autowired on fields)

### Git Commit Message Format
```
[IDENTITY] Add JWT token provider

- Implement JwtProvider class with token generation
- Add JwtValidator for token validation
- Configure JWT properties in SecurityConfiguration
- Add unit tests for JWT utilities (90% coverage)

Related ADR: ADR-0005 (JWT + RBAC)
```

---

## Useful Commands

### Maven
```bash
# Build without tests
mvn clean package -DskipTests

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html

# Run specific test
mvn test -Dtest=UserServiceTest

# Run all tests in package
mvn test -Dtest=com.smartstock.identity.application.service.*
```

### Database
```bash
# Connect to database
psql -h localhost -U smartstock -d smartstock_identity

# List tables
\dt public.*

# Run SQL query
\c smartstock_identity
SELECT * FROM users;

# View indexes
\di public.*
```

### Spring Boot
```bash
# Check health
curl http://localhost:8081/api/v1/actuator/health

# View metrics
curl http://localhost:8081/api/v1/actuator/metrics

# View active configuration
curl http://localhost:8081/api/v1/actuator/env
```

---

## Troubleshooting

### Build Failures
- Ensure Java 21 is the default JDK
- Clear Maven cache: `rm -rf ~/.m2/repository`
- Rebuild: `mvn clean install`

### Database Connection Issues
- Verify PostgreSQL is running
- Check credentials in application.yml
- Verify database exists and user has permissions

### Port Already in Use
- Change port in application.yml
- Or kill existing process: `lsof -i :8081 | awk '{print $2}' | grep -v PID | xargs kill -9`

### Tests Failing
- Check if database is running
- Verify Flyway migrations executed successfully
- Check logs for specific errors

---

## Next Session Checklist

- [ ] Implement JWT utilities (JwtProvider, JwtValidator)
- [ ] Create SecurityConfiguration (Spring Security)
- [ ] Implement UserService with CRUD operations
- [ ] Implement AuthenticationService (login, register, refresh)
- [ ] Create DTOs and MapStruct mappers
- [ ] Create REST controllers
- [ ] Implement global exception handler
- [ ] Write unit tests (target 80%+ coverage)
- [ ] Write integration tests
- [ ] Verify API endpoints work
- [ ] Generate OpenAPI/Swagger documentation
- [ ] Deploy locally and test with other services
- [ ] Code review and documentation

---

## References

- Architecture Decisions: `/docs/decisions/`
- Development Standards: `/docs/standards/`
- CDCDetails: `/docs/cdc.md`
- SmartStock Instructions: `/.github/instructions/SmartStock.instructions.md`

---

## Support

For questions about:
- **Architecture**: Review `/docs/decisions/ADR-*.md`
- **Coding Standards**: Review `/docs/standards/*.md`
- **Spring Boot**: Refer to Spring official docs
- **DDD patterns**: Review domain layer structure
- **Testing**: Review testing guidelines

---

**Last Updated:** 2026-06-22  
**Status:** Ready for Phase 1A - Identity Service Completion
