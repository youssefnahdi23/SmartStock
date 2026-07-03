FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="SmartStock Team <dev@smartstock.local>"
LABEL service="analytics-service"
LABEL version="1.0.0"

RUN addgroup -S smartstock && adduser -S analytics -G smartstock
USER analytics

WORKDIR /app

COPY services/analytics-service/target/smartstock-analytics-service-*.jar app.jar

EXPOSE 8012

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8012/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

# Exec-form ENTRYPOINT does not expand shell vars; a -Dspring.profiles.active
# flag here would also override SPRING_PROFILES_ACTIVE from compose/k8s.
# Default the profile via ENV instead (overridable at runtime).
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
