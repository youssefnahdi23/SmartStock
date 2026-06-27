package com.smartstock.warehouse.config;

import com.smartstock.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String WAREHOUSE_EVENTS_TOPIC = Topics.WAREHOUSE_EVENTS;

    @Bean
    public NewTopic warehouseEventsTopic() {
        return TopicBuilder.name(WAREHOUSE_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
