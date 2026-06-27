package com.smartstock.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configures the transactional outbox (debt C-2) for any service that opts in with
 * {@code smartstock.outbox.enabled=true}. Off by default so consumers/skeletons that have no
 * outbox table never run the relay. Gated on Kafka being on the classpath.
 *
 * <p>The relay publishes via its own {@code acks=all} + idempotent {@code String} producer,
 * independent of the service's application-level {@code KafkaTemplate}, so reliability settings
 * live in one place and the stored JSON payload goes out verbatim.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "smartstock.outbox", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    public OutboxRepository outboxRepository(DataSource dataSource) {
        return new OutboxRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    public OutboxService outboxService(OutboxRepository repository, ObjectMapper objectMapper) {
        return new OutboxService(repository, objectMapper);
    }

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Exactly-once-ish producer guarantees: full ISR ack + idempotence prevents duplicates
        // from producer retries and never reports success on a lost write.
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(ProducerFactory<String, String> outboxProducerFactory) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxRepository repository,
                                   KafkaTemplate<String, String> outboxKafkaTemplate,
                                   @org.springframework.beans.factory.annotation.Value("${smartstock.outbox.batch-size:100}") int batchSize) {
        return new OutboxRelay(repository, outboxKafkaTemplate, batchSize);
    }
}
