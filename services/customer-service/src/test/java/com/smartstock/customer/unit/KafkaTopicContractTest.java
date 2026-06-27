package com.smartstock.customer.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.customer.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract: customer-service producer must publish to the canonical topic (C-4).
 * This catches a regression to the legacy {@code events.customer} wire name that was fixed
 * during stabilization — the correct form is {@code customer.events}.
 */
@DisplayName("Customer-service Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer topic constant delegates to the shared Topics registry")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.CUSTOMER_EVENTS_TOPIC)
                .as("KafkaConfig must reference Topics.CUSTOMER_EVENTS, not the legacy 'events.customer' literal")
                .isSameAs(Topics.CUSTOMER_EVENTS);
    }

    @Test
    @DisplayName("Topics.CUSTOMER_EVENTS has the normalised wire name (not legacy events.customer)")
    void topicWireNameIsNormalised() {
        assertThat(Topics.CUSTOMER_EVENTS)
                .isEqualTo("customer.events")
                .doesNotContain("events.customer");
    }
}
