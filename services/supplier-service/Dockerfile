FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S smartstock && adduser -S supplier -G smartstock

USER supplier
WORKDIR /app

COPY services/supplier-service/target/smartstock-supplier-service-*.jar app.jar

EXPOSE 8005

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8005/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

# Exec-form ENTRYPOINT does not expand shell vars, and a -Dspring.profiles.active
# flag would override SPRING_PROFILES_ACTIVE from compose/k8s.
# Default the profile via ENV instead (overridable at runtime).
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
