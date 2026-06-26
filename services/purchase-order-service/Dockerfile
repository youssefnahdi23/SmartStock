FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S smartstock && adduser -S purchase -G smartstock

USER purchase
WORKDIR /app

COPY services/purchase-order-service/target/smartstock-purchase-order-service-*.jar app.jar

EXPOSE 8007

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8007/api/v1/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
