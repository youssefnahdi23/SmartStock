package com.smartstock.common.consumer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import javax.sql.DataSource;

/**
 * Shared Kafka consumer hardening (debt H-3), opt-in via {@code smartstock.consumer.enabled=true}.
 * Provides:
 * <ul>
 *   <li>a {@link DefaultErrorHandler} with exponential backoff that, after retries are
 *       exhausted, routes the poison record to a {@code <topic>.DLT} dead-letter topic instead of
 *       blocking the partition forever. Spring Boot applies this {@code CommonErrorHandler} bean to
 *       the auto-configured listener container factory automatically;</li>
 *   <li>an {@link IdempotencyService} so handlers can dedupe at-least-once redeliveries.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "smartstock.consumer", name = "enabled", havingValue = "true")
public class ConsumerAutoConfiguration {

    @Bean
    public IdempotencyService idempotencyService(DataSource dataSource) {
        return new IdempotencyService(new JdbcTemplate(dataSource));
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        // Publish exhausted records to "<original-topic>.DLT" (the recoverer's default).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        // 1s, 2s, 4s … capped, with a bounded number of attempts so a poison message cannot
        // wedge the partition indefinitely.
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10_000L);
        backOff.setMaxElapsedTime(30_000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
