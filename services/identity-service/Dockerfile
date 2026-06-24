# Multi-stage build for Identity Service
# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build

# Cache Maven dependencies
COPY services/pom.xml ./pom.xml
COPY services/identity-service/pom.xml ./identity-service/pom.xml
COPY services/common/pom.xml ./common/pom.xml

# Copy source and build (skip other modules for faster builds)
COPY services/common/src ./common/src
COPY services/identity-service/src ./identity-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl common,identity-service -am package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine AS runtime
LABEL maintainer="SmartStock Team <dev@smartstock.local>"
LABEL service="identity-service"
LABEL version="1.0.0"

# Create non-root user
RUN addgroup -S smartstock && adduser -S identity -G smartstock
USER identity

WORKDIR /app

# Copy built JAR
COPY --from=builder /build/identity-service/target/smartstock-identity-service-*.jar app.jar

# Create log directory
RUN mkdir -p /app/logs

EXPOSE 8001

# Health check via actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8001/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILE:-prod}", \
    "-jar", "app.jar"]
