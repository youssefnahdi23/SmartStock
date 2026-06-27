package com.smartstock.customer.config;

import com.smartstock.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CUSTOMER_EVENTS_TOPIC = Topics.CUSTOMER_EVENTS;

    @Bean
    public NewTopic customerEventsTopic() {
        return TopicBuilder.name(CUSTOMER_EVENTS_TOPIC)
                .partitions(10)
                .replicas(3)
                .build();
    }
}
