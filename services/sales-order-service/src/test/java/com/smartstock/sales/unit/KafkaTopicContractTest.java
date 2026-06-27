package com.smartstock.sales.unit;

import com.smartstock.common.event.Topics;
import com.smartstock.sales.config.KafkaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Producer side of the sales-order topic contract (debt C-4). The publisher must emit on
 * the same canonical topic the customer-service listener subscribes to; both reference
 * {@link Topics#SALES_ORDER_EVENTS}. This guards against re-introducing a hardcoded literal
 * that drifts from the shared registry.
 */
@DisplayName("Sales-order Kafka topic contract")
class KafkaTopicContractTest {

    @Test
    @DisplayName("producer publishes to the canonical sales-order topic")
    void producerTopicMatchesRegistry() {
        assertThat(KafkaConfig.SALES_ORDER_EVENTS_TOPIC).isEqualTo(Topics.SALES_ORDER_EVENTS);
        assertThat(Topics.SALES_ORDER_EVENTS).isEqualTo("sales-order.events");
    }
}
