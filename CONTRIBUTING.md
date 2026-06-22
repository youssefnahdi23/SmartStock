# Contributing to SmartStock AI

Thank you for your interest in contributing to SmartStock AI! This document provides guidelines and instructions for contributing.

## Code of Conduct

All contributors must follow our [Code of Conduct](CODE_OF_CONDUCT.md). We are committed to providing a welcoming and inspiring community for all.

## How to Contribute

### 1. Reporting Bugs

Before creating a bug report, check the issue list to avoid duplicates.

**When reporting a bug, include:**

- Clear, descriptive title
- Detailed reproduction steps
- Expected vs. actual behavior
- Environment (Java version, OS, Spring Boot version, etc.)
- Relevant logs or screenshots
- Possible solution (if you have ideas)

### 2. Suggesting Features

Check existing issues first to avoid duplicates.

**When suggesting a feature, include:**

- Clear use case and business value
- How it aligns with SmartStock's vision (enterprise inventory intelligence)
- Proposed API design (if applicable)
- How it affects the data collection strategy for AI readiness

**Note**: Features must align with our Architecture Decision Records (ADRs). If your feature conflicts with an ADR, we'll discuss updating the ADR first.

### 3. Submitting Code Changes

#### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Git
- IDE: IntelliJ IDEA or VS Code with Java extensions

#### Setup Development Environment

```bash
# Clone the repository
git clone https://github.com/youssefnahdi23/SmartStock.git
cd SmartStock

# Install dependencies
mvn clean install

# Start infrastructure services
docker-compose up -d

# Run tests
mvn test
```

#### Development Workflow

1. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/bug-description
   ```
   Use `feature/` or `fix/` prefixes for clarity.

2. **Make Changes**
   - Follow the code style guidelines (see below)
   - Add or update tests for your changes
   - Update documentation if needed
   - Respect all ADRs (see `/docs/decisions/`)

3. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: clear description of change"
   ```
   Use conventional commits (feat:, fix:, docs:, test:, refactor:, etc.)

4. **Run Quality Checks**
   ```bash
   # Format code
   mvn spotless:apply

   # Run tests
   mvn test

   # Run integration tests
   mvn verify

   # Check code coverage
   mvn jacoco:report
   ```

5. **Push and Create Pull Request**
   ```bash
   git push origin feature/your-feature-name
   ```
   Open a PR on GitHub with:
   - Clear title describing the change
   - Description of what changed and why
   - Reference to related issues (#123)
   - ADR compliance confirmation
   - Test coverage information

#### Code Style Guidelines

**General Principles**
- Write for readability, not cleverness
- Follow SOLID principles
- Respect Clean Architecture layers
- Prefer composition over inheritance
- Keep classes and methods focused and small

**Naming Conventions**
```java
// Classes: PascalCase
public class UserService { }

// Methods/variables: camelCase
public void authenticateUser() { }
private String userName;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_LOGIN_ATTEMPTS = 5;

// Packages: lowercase, reverse domain notation
com.smartstock.identity.application
```

**Java Code Standards**
```java
// Constructor injection only (never field injection)
@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// Use Optional for nullable values
public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
}

// Use records for DTOs
public record LoginRequestDto(
    String username,
    String password
) { }

// Avoid null checks; use Optional
user.ifPresent(u -> process(u));
user.orElseThrow(() -> new UserNotFoundException());
```

**Comments**
- Comment WHY, not WHAT
- Avoid obvious comments
- Javadoc for public APIs

```java
// ✓ GOOD: Explains business logic
// Retry up to 3 times because production occasionally has transient DB failures
public void saveWithRetry(User user) { }

// ✗ BAD: States obvious code
// Set user to active
user.setActive(true);
```

**Testing**
```java
// Test class naming
public class UserServiceTest { }

// Test method naming: should_expectedBehavior_whenCondition
@Test
public void should_throwUserNotFoundException_whenUsernameNotFound() { }

// Use descriptive assertions
assertThat(result)
    .as("Login should fail with invalid credentials")
    .isFalse();
```

### 4. Testing Requirements

- **Unit Tests**: Test business logic in isolation
- **Integration Tests**: Test service-to-service communication
- **Repository Tests**: Test database queries with TestContainers
- **Controller Tests**: Test HTTP endpoints

**Coverage Targets**:
- Minimum: 80% overall
- Critical business logic: 90%+
- Controllers: 70%+

### 5. Documentation

Update documentation when:

- Adding a new service or feature
- Changing API contracts
- Adding new configurations
- Creating new ADRs (for architectural decisions)

Documentation files:

```
/docs/
  /decisions/          # ADRs
  /architecture/       # System design docs
  /api/               # API specifications
  /guides/            # How-to guides
README.md             # Project overview
```

### 6. ADR Compliance

Before implementing features:

1. Check if relevant ADR exists in `/docs/decisions/`
2. If yes, implement following the ADR exactly
3. If no, propose a new ADR before implementation
4. Never silently override architectural decisions

**Example ADRs**:
- ADR-0001: Microservices Architecture
- ADR-0003: Database Per Service
- ADR-0005: JWT + RBAC Authentication
- ADR-0009: Observability (Logging & Monitoring)

## Pull Request Process

1. **PR Title**: Follow conventional commits (feat:, fix:, docs:, etc.)
2. **PR Description**: Explain WHAT changed and WHY
3. **Reference Issues**: Link related issues (#123)
4. **Checklist**:
   ```markdown
   - [ ] Code follows style guidelines
   - [ ] Tests added/updated
   - [ ] Test coverage maintained (80%+)
   - [ ] Documentation updated
   - [ ] No hardcoded secrets or credentials
   - [ ] ADRs reviewed and compliant
   - [ ] No breaking changes (or documented)
   ```

5. **Review**: Address feedback from maintainers
6. **Merge**: Maintainers merge after approval and checks pass

## Commit Message Guidelines

Use conventional commits for clarity:

```
feat: add user registration endpoint
fix: resolve JWT token expiration bug
docs: update API documentation
test: add integration tests for user service
refactor: restructure permission model
chore: update dependencies
ci: add GitHub Actions workflow
```

## Security Considerations

- **Never commit secrets**: Use environment variables
- **Validate input**: Always sanitize user input
- **Use parameterized queries**: Never string concatenation for SQL
- **Hash passwords**: Use BCrypt with cost factor 12+
- **Log carefully**: Never log passwords or tokens
- **Report vulnerabilities**: Email security@smartstock.dev (non-public)

See [SECURITY.md](SECURITY.md) for detailed guidelines.

## Questions or Need Help?

- Check [/docs/decisions/](docs/decisions/) for architectural decisions
- Review [README_IMPLEMENTATION.md](README_IMPLEMENTATION.md) for setup
- Open an issue with the question tag
- Join our community discussions

## License

By contributing to SmartStock AI, you agree that your contributions will be licensed under the MIT License.

## Recognition

Contributors will be recognized in:
- Project README
- Release notes
- Contributors page

Thank you for contributing to SmartStock AI! 🚀
