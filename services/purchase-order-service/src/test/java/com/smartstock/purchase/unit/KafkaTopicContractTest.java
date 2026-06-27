package com.smartstock.purchase.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.purchase.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: purchase-order-service producer must publish to the canonical topic so any consumer
 * referencing {@link Topics#PURCHASE_ORDER_EVENTS} receives the events it expects (C-4).
 */
@DisplayName("Purchase-order-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.PURCHASE_ORDER_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.PURCHASE_ORDER_EVENTS, not a hardcoded literal")
                .isSameAs(Topics.PURCHASE_ORDER_EVENTS);
    }

    @Test
    @DisplayName("Topics.PURCHASE_ORDER_EVENTS has the expected wire name")
    void topicWireName() {
        assertThat(Topics.PURCHASE_ORDER_EVENTS).isEqualTo("purchase-order.events");
    }
}
