# Build stage
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /build

# Copy Maven files
COPY pom.xml .
COPY parent-pom.xml ../pom.xml 2>/dev/null || true

# Copy source code
COPY src ./src

# Build application
RUN chmod +x ./mvnw 2>/dev/null || apk add --no-cache maven \
    && mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=build/target/*.jar

WORKDIR /app

# Copy jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health/status || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
