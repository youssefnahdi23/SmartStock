package com.smartstock.purchase.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String PURCHASE_ORDER_EVENTS_TOPIC = "purchase-order.events";

    @Bean
    public NewTopic purchaseOrderEventsTopic() {
        return TopicBuilder.name(PURCHASE_ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
