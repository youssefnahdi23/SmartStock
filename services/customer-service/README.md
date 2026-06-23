# Customer Service

## Overview

Microservice responsible for customer service.

## Architecture

### Clean Architecture Layers

```
presentation/
  └── controller/          # REST API endpoints
application/
  └── service/             # Application logic & orchestration
domain/
  └── event/               # Domain events
infrastructure/
  ├── config/              # Spring configurations
└── messaging/             # Kafka event producers/consumers
```

## Configuration

Environment variables:
- `SERVER_PORT`: Service port (default: 8080)
- `DB_USER`: Database user (default: postgres)
- `DB_PASSWORD`: Database password
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses

## Building

```bash
mvn clean package
```

## Running

```bash
java -jar target/*.jar
```

## Docker

```bash
docker build -t smartstock-customerservice .
docker run -p 8080:8080 smartstock-customerservice
```

## Health Check

```bash
curl http://localhost:8080/api/v1/health/status
```

## API Documentation

OpenAPI documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn verify
```

## Database Migrations

Flyway migrations are in `src/main/resources/db/migration/`

## Event-Driven Architecture

This service emits domain events for event-driven communication with other microservices.

See events documentation in `docs/events/` for published event schemas.
