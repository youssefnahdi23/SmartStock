# Spring Boot Guidelines

## Architecture Rule

Follow Clean Architecture:

controller → service → domain → repository

---

## Dependency Injection

- Use constructor injection ONLY
- Never use field injection

---

## Controllers

- Must be thin
- Only handle HTTP layer
- No business logic

---

## Services

- Contain business logic
- Stateless
- Reusable across controllers

---

## Repositories

- Only data access
- No business logic
- Use Spring Data JPA

---

## Error Handling

- Use @ControllerAdvice
- Standard error response format

---

## Security

- Spring Security enabled always
- JWT required for all endpoints
- Role-based access mandatory

---

## APIs

- RESTful design
- Use nouns, not verbs
- Example: `/products`, NOT `/getProducts`
