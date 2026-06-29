package com.smartstock.product.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.product.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: product-service producer must publish to the canonical topic so any consumer
 * referencing {@link Topics#PRODUCT_EVENTS} receives the events it expects (C-4).
 */
@DisplayName("Product-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.PRODUCT_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.PRODUCT_EVENTS, not a hardcoded literal")
                .isSameAs(Topics.PRODUCT_EVENTS);
    }

    @Test
    @DisplayName("Topics.PRODUCT_EVENTS has the expected wire name")
    void topicWireName() {
        assertThat(Topics.PRODUCT_EVENTS).isEqualTo("product.events");
    }
}
