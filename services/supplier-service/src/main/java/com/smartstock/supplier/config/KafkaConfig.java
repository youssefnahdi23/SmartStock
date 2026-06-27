package com.smartstock.supplier.config;

import com.smartstock.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String SUPPLIER_EVENTS_TOPIC = Topics.SUPPLIER_EVENTS;

    @Bean
    public NewTopic supplierEventsTopic() {
        return TopicBuilder.name(SUPPLIER_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
