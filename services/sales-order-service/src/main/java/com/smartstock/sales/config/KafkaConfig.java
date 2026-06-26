package com.smartstock.sales.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String SALES_ORDER_EVENTS_TOPIC = "sales-order.events";

    @Bean
    public NewTopic salesOrderEventsTopic() {
        return TopicBuilder.name(SALES_ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
