FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="SmartStock Team <dev@smartstock.local>"
LABEL service="data-export-service"
LABEL version="1.0.0"

RUN addgroup -S smartstock && adduser -S dataexport -G smartstock
USER dataexport

WORKDIR /app

COPY services/data-export-service/target/smartstock-data-export-service-*.jar app.jar

EXPOSE 8013

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8013/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILE:-prod}", \
    "-jar", "app.jar"]
