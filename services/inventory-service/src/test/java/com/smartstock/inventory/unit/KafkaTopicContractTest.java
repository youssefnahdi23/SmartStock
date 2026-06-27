package com.smartstock.inventory.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.inventory.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: inventory-service producer must publish to the canonical topic so any consumer
 * referencing {@link Topics#INVENTORY_EVENTS} receives the events it expects (C-4).
 */
@DisplayName("Inventory-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.INVENTORY_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.INVENTORY_EVENTS, not a hardcoded literal")
                .isSameAs(Topics.INVENTORY_EVENTS);
    }

    @Test
    @DisplayName("Topics.INVENTORY_EVENTS has the expected wire name")
    void topicWireName() {
        assertThat(Topics.INVENTORY_EVENTS).isEqualTo("inventory.events");
    }
}
