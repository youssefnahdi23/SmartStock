# Pull Request

## Summary

### Description

Provide a clear and concise description of the changes introduced in this Pull Request.

---

## Type of Change

Select all that apply.

* [ ] Feature
* [ ] Bug Fix
* [ ] Refactoring
* [ ] Documentation
* [ ] Performance Improvement
* [ ] Security Improvement
* [ ] Test Improvements
* [ ] CI/CD
* [ ] Infrastructure
* [ ] Database Migration
* [ ] Architecture Change
* [ ] Breaking Change

---

## Related Work

Issue(s):

ADR(s):

Milestone:

Affected Service(s):

---

## Architectural Impact

Does this change modify the software architecture?

* [ ] No
* [ ] Yes (If yes, reference the ADR)

ADR Reference:

---

## Checklist

### Architecture

* [ ] Follows Clean Architecture
* [ ] Respects Domain-Driven Design boundaries
* [ ] Does not introduce unnecessary coupling
* [ ] Does not violate microservice boundaries
* [ ] Business logic remains inside the domain/application layer

---

### API

* [ ] API follows REST conventions
* [ ] DTOs are used instead of entities
* [ ] OpenAPI documentation updated (if applicable)
* [ ] API versioning respected

---

### Database

* [ ] Flyway migration included (if required)
* [ ] No cross-service database access
* [ ] Indexes reviewed
* [ ] Constraints reviewed

---

### Events

* [ ] Required domain events published
* [ ] Event schema documented
* [ ] Event versioning considered

---

### Security

* [ ] Authorization rules reviewed
* [ ] Input validation implemented
* [ ] No secrets committed
* [ ] Sensitive data protected
* [ ] Audit logging maintained

---

### Performance

* [ ] Pagination implemented where necessary
* [ ] SQL queries optimized
* [ ] Caching considered
* [ ] No obvious performance regressions

---

### Testing

* [ ] Unit tests added or updated
* [ ] Integration tests added or updated
* [ ] Existing tests pass

---

### Documentation

* [ ] ADR updated (if architecture changed)
* [ ] Documentation updated
* [ ] README updated (if necessary)
* [ ] API documentation updated

---

### AI Readiness

If this feature generates business data:

* [ ] Events contain sufficient metadata
* [ ] Historical data is preserved
* [ ] Export compatibility maintained
* [ ] Future analytics compatibility maintained

---

## Risks

Describe any known risks introduced by this Pull Request.

---

## Rollback Plan

Explain how this change can be safely rolled back if necessary.

---

## Screenshots (if applicable)

Include screenshots or GIFs for UI changes.

---

## Additional Notes

Provide any additional information reviewers should know before approving this Pull Request.
