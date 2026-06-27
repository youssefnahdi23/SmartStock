package com.smartstock.sales.config;

import com.smartstock.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /** Canonical topic from the shared registry — keeps producer and consumers in lockstep (C-4). */
    public static final String SALES_ORDER_EVENTS_TOPIC = Topics.SALES_ORDER_EVENTS;

    @Bean
    public NewTopic salesOrderEventsTopic() {
        return TopicBuilder.name(SALES_ORDER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
