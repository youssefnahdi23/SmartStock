# SECURITY.md

## Security Policy

SmartStock AI is committed to maintaining high security standards across all services and deployments.

## Reporting Security Vulnerabilities

**DO NOT** open public GitHub issues to report security vulnerabilities.

Instead, please email **security@smartstock.dev** with:

1. Description of the vulnerability
2. Steps to reproduce (if applicable)
3. Potential impact
4. Suggested fix (if you have one)

We will acknowledge receipt within 48 hours and provide updates as we investigate.

## Security Standards

All code in SmartStock must follow these security principles (see ADRs for details):

### Authentication & Authorization (ADR-0005)
- JWT tokens for stateless authentication
- Role-Based Access Control (RBAC) with granular permissions
- Refresh tokens for secure token rotation
- BCrypt for password hashing (minimum cost factor: 12)
- No password logging or token logging

### Database Security (ADR-0003)
- Parameterized queries only (no SQL injection)
- Foreign key constraints for referential integrity
- Column-level encryption for sensitive data (PII, financial)
- Audit logging for all data modifications
- Automated backups with encryption at rest

### API Security (ADR-0008, ADR-0016)
- HTTPS/TLS 1.2+ mandatory
- API versioning for controlled deprecation
- Rate limiting to prevent abuse
- CORS configured strictly (no wildcards)
- Input validation and sanitization
- Request signing for inter-service communication

### Infrastructure Security (ADR-0010)
- Network policies restricting service-to-service communication
- Secrets management via environment variables or HashiCorp Vault
- Container image scanning for vulnerabilities
- Runtime security policies
- Log aggregation and monitoring

### Code Security
- No hardcoded secrets or credentials
- Dependencies scanned with OWASP Dependency-Check
- Static analysis with SonarQube
- No known vulnerabilities (CVE) in dependencies
- Annual security audit

## Dependency Vulnerabilities

We use automated tools to detect and address dependency vulnerabilities:

- **GitHub Dependabot**: Automated PR creation for security updates
- **Renovate**: Semantic versioning and update grouping
- **OWASP Dependency-Check**: Build-time vulnerability scanning
- **Snyk**: Container and dependency scanning

## Security Testing

All services include:

- Unit tests with security focus (e.g., permission checks)
- Integration tests validating authentication flows
- Penetration testing (annual)
- Fuzzing tests for input validation
- HTTPS/TLS configuration testing

## Secure Coding Guidelines

### Java/Spring Boot
```java
// ✓ GOOD: Parameterized query
repository.findByUsername(username);

// ✗ BAD: SQL injection risk
String query = "SELECT * FROM users WHERE username = '" + username + "'";

// ✓ GOOD: BCrypt password hashing
new BCryptPasswordEncoder(12).encode(password)

// ✗ BAD: Plain text passwords
passwordField.setText(password)

// ✓ GOOD: No sensitive data in logs
logger.info("User {} logged in", userId)

// ✗ BAD: Logging sensitive data
logger.info("User {} logged in with password {}", userId, password)
```

### Environment Configuration
```yaml
# ✓ GOOD: Environment variable substitution
database:
  password: ${DB_PASSWORD}

# ✗ BAD: Hardcoded secrets
database:
  password: "hardcoded-secret-123"
```

## Dependencies & Third-Party Libraries

Before adding new dependencies:

1. Verify the library is actively maintained
2. Check for known CVEs
3. Ensure it aligns with our technology stack
4. Review licensing compatibility

Add dependencies via Maven POM with explicit versions:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-client</artifactId>
    <version>${spring-security.version}</version>
</dependency>
```

## Deployment Security

All deployments must:

1. Use encrypted communication (TLS/HTTPS)
2. Enable authentication and authorization
3. Configure network policies
4. Implement audit logging
5. Monitor for anomalies
6. Maintain encrypted backups
7. Follow least-privilege principle

## Incident Response

If a security incident occurs:

1. **Immediate**: Isolate affected systems
2. **Assessment**: Determine scope and impact
3. **Notification**: Alert affected parties within 24 hours
4. **Remediation**: Fix root cause and deploy fix
5. **Post-Mortem**: Document lessons learned
6. **Prevention**: Implement safeguards

## Compliance

SmartStock aims to comply with:

- OWASP Top 10
- CWE/SANS Top 25
- SOC 2 Type II controls
- GDPR (for EU users)
- Data protection regulations

## Security Resources

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CWE/SANS Top 25](https://cwe.mitre.org/top25/2022/)

## Questions?

For security questions (non-vulnerability), please:

1. Check this document and related ADRs
2. Review Spring Security documentation
3. Ask in the `#security` Slack channel (if available)
4. Email architecture team

**Last Updated**: June 2026
