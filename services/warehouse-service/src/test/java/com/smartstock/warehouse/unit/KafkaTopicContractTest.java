package com.smartstock.warehouse.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.warehouse.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: warehouse-service producer must publish to the canonical topic so any consumer
 * referencing {@link Topics#WAREHOUSE_EVENTS} receives the events it expects (C-4).
 */
@DisplayName("Warehouse-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.WAREHOUSE_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.WAREHOUSE_EVENTS, not a hardcoded literal")
                .isSameAs(Topics.WAREHOUSE_EVENTS);
    }

    @Test
    @DisplayName("Topics.WAREHOUSE_EVENTS has the expected wire name")
    void topicWireName() {
        assertThat(Topics.WAREHOUSE_EVENTS).isEqualTo("warehouse.events");
    }
}
