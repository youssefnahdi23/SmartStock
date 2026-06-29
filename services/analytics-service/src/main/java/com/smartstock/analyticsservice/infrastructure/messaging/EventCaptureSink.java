package com.smartstock.analyticsservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.common.consumer.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Durable "capture everything" sink (debt H-4). Subscribes to every domain-event topic and
 * persists the raw JSON envelope verbatim, so a complete, schema-stable historical record accrues
 * now — before any analytics logic — because lost event history is unrecoverable and is the
 * foundation the AI roadmap (ADR-0006) depends on.
 *
 * <p>Captures the message as a raw {@code String} (the value deserializer is overridden to
 * {@code StringDeserializer}); the event id/type are parsed best-effort for indexing. Capture is
 * idempotent (shared H-3 ledger) so at-least-once redeliveries are stored once, and runs in one
 * transaction with the idempotency claim.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventCaptureSink {

    static final String CONSUMER_NAME = "analytics-capture";

    private final EventCaptureRepository repository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                    "#{T(com.smartstock.common.event.Topics).INVENTORY_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).PRODUCT_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).WAREHOUSE_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).SUPPLIER_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).CUSTOMER_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).IDENTITY_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).PURCHASE_ORDER_EVENTS}",
                    "#{T(com.smartstock.common.event.Topics).SALES_ORDER_EVENTS}"
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void capture(ConsumerRecord<String, String> record) {
        String payload = record.value();
        if (payload == null) {
            return;
        }
        String eventId = null;
        String eventType = null;
        try {
            JsonNode node = objectMapper.readTree(payload);
            eventId = text(node, "eventId");
            eventType = text(node, "eventType");
        } catch (Exception ex) {
            // Never drop an event over a parse hiccup — capture it with null metadata.
            log.warn("Could not parse envelope from {} (offset {}): {}",
                    record.topic(), record.offset(), ex.getMessage());
        }

        if (eventId != null && !idempotencyService.claim(CONSUMER_NAME, eventId)) {
            return; // already captured
        }
        repository.capture(record.topic(), record.key(), eventId, eventType, payload);
        log.debug("Captured {} from {} (key={})", eventType, record.topic(), record.key());
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
