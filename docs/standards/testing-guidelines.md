# Testing Guidelines

## Required Test Types

- Unit Tests
- Integration Tests
- API Tests

---

## Coverage Target

Minimum 80% coverage (target 90%+)

---

## Rules

- Test business logic only
- Mock external dependencies
- No production database in unit tests

---

## Naming

- testShouldReturnProductWhenExists()
- testShouldThrowExceptionWhenInvalidInput()

---

## Frameworks

- JUnit 5
- Mockito
- Testcontainers (for integration tests)
