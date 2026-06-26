FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="SmartStock Team <dev@smartstock.local>"
LABEL service="order-service"
LABEL version="1.0.0"

RUN addgroup -S smartstock && adduser -S order -G smartstock
USER order

WORKDIR /app

COPY services/order-service/target/smartstock-order-service-*.jar app.jar

EXPOSE 8014

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8014/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILE:-prod}", \
    "-jar", "app.jar"]
