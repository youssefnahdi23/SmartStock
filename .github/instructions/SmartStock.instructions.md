# SmartStock AI - GitHub Copilot Instructions

---

# PROJECT TYPE

SmartStock AI is a commercial-grade Enterprise Inventory Intelligence Platform.

NOT a CRUD project.
NOT a tutorial system.

It is a distributed enterprise system designed for:

- Microservices architecture
- Event-driven architecture
- Domain Driven Design (DDD)
- Clean Architecture
- AI-ready data collection platform (AI is NOT core logic)

---

# 1. CORE ENGINEERING RULE

Always behave like a Principal Software Engineer in a large enterprise system.

Every code decision must prioritize:

- Long-term maintainability
- Scalability
- Observability
- Security
- Data integrity
- AI-readiness (data quality, not ML logic)

---

# 2. ARCHITECTURE GUARANTEE (STRICT RULE)

YOU MUST NEVER:

- Mix business logic across services
- Access another service database directly
- Break Clean Architecture layers
- Put logic in controllers
- Bypass service boundaries
- Introduce hidden dependencies between services

Each service is fully independent.

---

# 3. ADR-FIRST DEVELOPMENT (CRITICAL)

Before implementing any feature:

1. Check /docs/decisions (ADRs)
2. Identify relevant ADR
3. If no ADR exists → DO NOT IMPLEMENT
4. Instead, propose a new ADR

If implementation conflicts with an ADR:

- ADR is the source of truth
- Suggest updating ADR first
- NEVER silently override architecture

---

# 4. SERVICE BOUNDARY RULES

Each microservice MUST:

- Own its database
- Own its domain logic
- Own its API
- Own its events
- Be independently deployable

Services communicate ONLY via:

- REST APIs
- Domain Events (async messaging)

NO exceptions.

---

# 5. EVENT-DRIVEN ARCHITECTURE RULE

Every business action MUST emit an event.

Events are:

- Immutable
- Versioned
- Replayable
- Stored for analytics

Example events:

- ProductCreated
- StockUpdated
- InventoryAdjusted
- WarehouseTransferCompleted
- SupplierDeliveryRegistered

Events MUST NOT contain business logic.

---

# 6. DATA STRATEGY (AI READINESS FOUNDATION)

The system is designed to generate high-quality structured data.

Every event MUST include:

- timestamp
- entity IDs
- quantity/value changes
- user context
- service source
- optional metadata (warehouse, supplier, location)

DO NOT optimize for AI models now.
Optimize for DATA QUALITY.

AI is a downstream consumer ONLY.

---

# 7. CLEAN ARCHITECTURE RULE

Every service MUST follow:

controller → application → domain → infrastructure

Rules:

- Controllers: HTTP only
- Application: orchestration
- Domain: business rules
- Infrastructure: DB, messaging, external APIs

NEVER cross layers.

---

# 8. DATABASE RULES (STRICT)

- One database per service
- No shared tables
- No cross-service joins
- Use Flyway migrations
- Always include:
  - id (UUID)
  - created_at
  - updated_at
  - optional deleted_at

NEVER use "SELECT *".

Always paginate large datasets.

---

# 9. API DESIGN RULES

REST ONLY.

Rules:

- Use nouns (not verbs)
- Always version APIs (/api/v1)
- Always use DTOs (never expose entities)
- Always return consistent response format

---

# 10. SECURITY RULES

Always enforce:

- Spring Security
- JWT Authentication
- Role-Based Access Control (RBAC)
- BCrypt hashing
- Input validation
- Rate limiting
- HTTPS assumptions

NEVER:

- Hardcode credentials
- Log sensitive data
- Expose internal system details

---

# 11. LOGGING & OBSERVABILITY

Every request MUST include:

- requestId
- correlationId
- serviceName
- userId (if available)

NEVER log:

- passwords
- JWT tokens
- sensitive personal data

Logs must be structured (JSON preferred).

---

# 12. ERROR HANDLING RULE

All services must use centralized error handling.

All responses must follow:

{
  "timestamp": "",
  "status": 000,
  "error": "",
  "message": "",
  "path": ""
}

NEVER expose stack traces to clients.

---

# 13. TESTING RULES

Every feature MUST include:

- Unit tests
- Integration tests
- Service tests

Minimum coverage target: 80%

Critical business logic: 90%+

---

# 14. PERFORMANCE RULES

- Use caching where appropriate (Redis)
- Avoid N+1 queries
- Use pagination always
- Optimize DB indexes
- Avoid unnecessary object creation

---

# 15. JAVA & SPRING RULES

Java 21+

Rules:

- Constructor injection only
- No field injection
- Prefer records for DTOs
- Use Optional properly
- Keep classes small

Spring Boot:

- Actuator enabled
- Validation enabled
- Flyway enabled
- Security NEVER disabled

---

# 16. JAVAFX DESKTOP RULES

Architecture: MVVM

Rules:

- UI contains NO business logic
- All logic in ViewModels/services
- Use FXML + CSS only
- No Swing / Electron allowed
- Must support offline caching

---

# 17. ANTI-PATTERNS (DO NOT DO THIS)

NEVER:

- Create monolithic services inside microservices
- Duplicate business logic across services
- Hardcode business rules in controllers
- Skip DTOs
- Bypass event system
- Direct DB access across services
- Use global shared state
- Ignore ADRs
- Skip documentation updates

---

# 18. ADR ENFORCEMENT SUMMARY

ADRs are authoritative.

If ADR exists:

→ Follow it exactly

If no ADR exists:

→ Stop and request architecture decision

No exceptions.

---

# 19. FINAL PRINCIPLE

SmartStock AI is not just software.

It is an enterprise-grade distributed data system.

Every decision must prioritize:

- long-term scalability
- data quality for future AI systems
- strict architectural boundaries
- production-grade reliability