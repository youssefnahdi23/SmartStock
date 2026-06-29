package com.smartstock.identity.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("smartstock_identity_test")
                    .withUsername("smartstock")
                    .withPassword("smartstock")
                    // jsonb columns (e.g. audit_logs.old_values/new_values) are mapped from String;
                    // let PostgreSQL infer the type instead of rejecting the varchar binding.
                    .withUrlParam("stringtype", "unspecified");

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url",          POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user",         POSTGRES::getUsername);
        registry.add("spring.flyway.password",     POSTGRES::getPassword);
        // Keep Kafka auto-configuration so KafkaProperties (required by the transactional outbox)
        // is available, but never contact a broker in tests: producers are lazy, and listener
        // containers are kept from starting.
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }
}
