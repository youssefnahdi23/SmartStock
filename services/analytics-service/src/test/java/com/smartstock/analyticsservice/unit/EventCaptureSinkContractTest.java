package com.smartstock.analyticsservice.unit;

import com.smartstock.analyticsservice.infrastructure.messaging.EventCaptureSink;
import com.smartstock.common.event.Topics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that every topic the analytics capture sink subscribes to has a declared producer
 * in the Topics registry (C-4). Also verifies the SpEL expressions resolve to canonical
 * constants so there is no hidden string drift.
 *
 * <p>The "each consumed topic has a producer" invariant: the EventCaptureSink listens to
 * 8 topics; each is owned by a specific bounded context listed in the producer map below.
 * If a producer renames its topic, its own KafkaTopicContractTest fails; if a consumer
 * adds a new topic without a producer, this test fails.
 */
@DisplayName("EventCaptureSink — producer-consumer topic contract (C-4)")
class EventCaptureSinkContractTest {

    /**
     * Authoritative producer map: topic constant → producing service.
     * Extend this map when a new bounded context is added.
     */
    private static final Map<String, String> PRODUCER_BY_TOPIC = Map.of(
            Topics.INVENTORY_EVENTS,       "inventory-service",
            Topics.PRODUCT_EVENTS,         "product-service",
            Topics.WAREHOUSE_EVENTS,       "warehouse-service",
            Topics.SUPPLIER_EVENTS,        "supplier-service",
            Topics.CUSTOMER_EVENTS,        "customer-service",
            Topics.IDENTITY_EVENTS,        "identity-service",
            Topics.PURCHASE_ORDER_EVENTS,  "purchase-order-service",
            Topics.SALES_ORDER_EVENTS,     "sales-order-service"
    );

    @Test
    @DisplayName("capture() is annotated with @KafkaListener")
    void captureMethodHasKafkaListenerAnnotation() throws NoSuchMethodException {
        Method capture = EventCaptureSink.class.getMethod(
                "capture", org.apache.kafka.clients.consumer.ConsumerRecord.class);
        assertThat(capture.getAnnotation(KafkaListener.class))
                .as("capture() must carry @KafkaListener")
                .isNotNull();
    }

    @Test
    @DisplayName("sink subscribes to exactly the 8 canonical topics via SpEL")
    void sinkSubscribesToExactlyEightTopics() throws NoSuchMethodException {
        KafkaListener annotation = getKafkaListenerAnnotation();
        assertThat(annotation.topics())
                .as("EventCaptureSink must subscribe to 8 topics — one per bounded context")
                .hasSize(8);
    }

    @Test
    @DisplayName("every subscribed topic SpEL references the shared Topics class")
    void allSubscribedTopicsReferenceTopicsClass() throws NoSuchMethodException {
        KafkaListener annotation = getKafkaListenerAnnotation();
        List<String> notUsingTopicsClass = Arrays.stream(annotation.topics())
                .filter(t -> !t.contains("com.smartstock.common.event.Topics"))
                .collect(Collectors.toList());
        assertThat(notUsingTopicsClass)
                .as("Every @KafkaListener topic expression must reference the Topics registry constant")
                .isEmpty();
    }

    @Test
    @DisplayName("each subscribed topic has a declared producer in the producer map")
    void eachConsumedTopicHasDeclaredProducer() throws NoSuchMethodException {
        KafkaListener annotation = getKafkaListenerAnnotation();
        Set<String> canonicalTopics = PRODUCER_BY_TOPIC.keySet();

        // Build the set of Topics field names referenced in the SpEL expressions
        // e.g. "#{T(com.smartstock.common.event.Topics).INVENTORY_EVENTS}" -> "INVENTORY_EVENTS"
        List<String> resolvedTopics = Arrays.stream(annotation.topics())
                .map(spel -> {
                    int dot = spel.lastIndexOf('.');
                    int end = spel.indexOf('}', dot);
                    return spel.substring(dot + 1, end);
                })
                .map(fieldName -> {
                    try {
                        return (String) Topics.class.getField(fieldName).get(null);
                    } catch (Exception e) {
                        return "__UNRESOLVABLE__:" + fieldName;
                    }
                })
                .collect(Collectors.toList());

        assertThat(resolvedTopics)
                .as("Every consumed topic must resolve to a valid Topics constant")
                .doesNotContainAnyElementsOf(
                        List.of("__UNRESOLVABLE__"));

        for (String topic : resolvedTopics) {
            assertThat(canonicalTopics)
                    .as("Topic '%s' is consumed by analytics-service but has no declared producer. "
                            + "Add it to PRODUCER_BY_TOPIC in this test.", topic)
                    .contains(topic);
        }
    }

    @Test
    @DisplayName("every topic in the producer map is consumed by the sink")
    void noProducerTopicIsOrphaned() throws NoSuchMethodException {
        KafkaListener annotation = getKafkaListenerAnnotation();

        // Resolve SpEL field names to wire values
        Set<String> consumedWireNames = Arrays.stream(annotation.topics())
                .map(spel -> {
                    int dot = spel.lastIndexOf('.');
                    int end = spel.indexOf('}', dot);
                    return spel.substring(dot + 1, end);
                })
                .map(fieldName -> {
                    try {
                        return (String) Topics.class.getField(fieldName).get(null);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());

        for (String producerTopic : PRODUCER_BY_TOPIC.keySet()) {
            assertThat(consumedWireNames)
                    .as("Producer topic '%s' (owner: %s) is not consumed by EventCaptureSink. "
                            + "Either add it to the @KafkaListener or remove it from PRODUCER_BY_TOPIC.",
                            producerTopic, PRODUCER_BY_TOPIC.get(producerTopic))
                    .contains(producerTopic);
        }
    }

    private KafkaListener getKafkaListenerAnnotation() throws NoSuchMethodException {
        Method capture = EventCaptureSink.class.getMethod(
                "capture", org.apache.kafka.clients.consumer.ConsumerRecord.class);
        return capture.getAnnotation(KafkaListener.class);
    }
}
