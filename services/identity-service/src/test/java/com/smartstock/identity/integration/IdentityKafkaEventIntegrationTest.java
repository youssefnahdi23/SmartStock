package com.smartstock.identity.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that identity events (user.created, user.authenticated) are published
 * to Kafka after the corresponding REST calls succeed.
 */
class IdentityKafkaEventIntegrationTest extends AbstractIntegrationTest {

    @Container
    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    @BeforeAll
    static void startKafka() {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // Re-enable Kafka (AbstractIntegrationTest disables it by default)
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @LocalServerPort
    private int port;

    private KafkaConsumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";

        Properties props = new Properties();
        props.put("bootstrap.servers", KAFKA.getBootstrapServers());
        props.put("group.id", "identity-kafka-test-" + System.currentTimeMillis());
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("identity.events"));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void login_shouldPublishUserAuthenticatedEvent() throws InterruptedException {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"username": "system.admin", "password": "Admin@SmartStock2026!"}
                """)
        .when()
            .post("/identity/auth/login")
        .then()
            .statusCode(200);

        // Poll for the event with Awaitility-style loop
        String eventPayload = pollForEvent("USER_AUTHENTICATED", 10_000);
        assertThat(eventPayload)
                .as("Expected USER_AUTHENTICATED event on identity.events")
                .isNotNull()
                .contains("USER_AUTHENTICATED")
                .contains("system.admin");
    }

    @Test
    void register_shouldPublishUserCreatedEvent() throws InterruptedException {
        String username = "kafka.test.user." + System.currentTimeMillis();
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "username": "%s",
                    "email": "%s@example.com",
                    "password": "Secure@2026!",
                    "firstName": "Kafka",
                    "lastName": "Test"
                }
                """, username, username))
        .when()
            .post("/identity/users/register")
        .then()
            .statusCode(201);

        String eventPayload = pollForEvent("USER_CREATED", 10_000);
        assertThat(eventPayload)
                .as("Expected USER_CREATED event on identity.events")
                .isNotNull()
                .contains("USER_CREATED")
                .contains(username);
    }

    private String pollForEvent(String eventType, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value() != null && record.value().contains(eventType)) {
                    return record.value();
                }
            }
        }
        return null;
    }
}
