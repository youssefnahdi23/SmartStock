package com.smartstock.common.test;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;

/**
 * Base class providing a shared Kafka container for integration tests.
 * Subclasses must add {@code @SpringBootTest}, {@code @Testcontainers},
 * and a {@code @DynamicPropertySource} for the database they need.
 *
 * Lifecycle is managed manually via {@code @BeforeAll} so that subclasses
 * do not need the {@code junit-jupiter} TestContainers extension on this class.
 */
public abstract class AbstractKafkaIntegrationTest {

    protected static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.0");

    @BeforeAll
    static void startKafka() {
        if (!KAFKA.isRunning()) {
            KAFKA.start();
        }
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id",
                () -> "test-group-" + System.currentTimeMillis());
    }
}
