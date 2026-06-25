package com.smartstock.supplier.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String SUPPLIER_EVENTS_TOPIC = "supplier.events";

    @Bean
    public NewTopic supplierEventsTopic() {
        return TopicBuilder.name(SUPPLIER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
