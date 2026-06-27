package com.smartstock.supplier.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.supplier.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: supplier-service producer must publish to the canonical topic so any consumer
 * referencing {@link Topics#SUPPLIER_EVENTS} receives the events it expects (C-4).
 */
@DisplayName("Supplier-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.SUPPLIER_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.SUPPLIER_EVENTS, not a hardcoded literal")
                .isSameAs(Topics.SUPPLIER_EVENTS);
    }

    @Test
    @DisplayName("Topics.SUPPLIER_EVENTS has the expected wire name")
    void topicWireName() {
        assertThat(Topics.SUPPLIER_EVENTS).isEqualTo("supplier.events");
    }
}
