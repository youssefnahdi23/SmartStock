package com.smartstock.notificationservice.infrastructure.messaging;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.notificationservice.domain.model.NotificationLog;
import com.smartstock.notificationservice.domain.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Omnibus notification consumer: subscribes to all SmartStock topics and logs a
 * notification row for actionable events. The {@code channel = 'LOG'} entry is the
 * durable record; real email/push channels attach here when wired up.
 *
 * <p>Idempotent via {@code processed_events}: a redelivered event produces at most one
 * notification row, preventing duplicate alerts (H-3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    static final String CONSUMER_NAME = "notification-listener";

    private final NotificationLogRepository notificationLogRepository;
    private final IdempotencyService        idempotencyService;

    @KafkaListener(
            topics = {
                "#{T(com.smartstock.common.event.Topics).INVENTORY_EVENTS}",
                "#{T(com.smartstock.common.event.Topics).SALES_ORDER_EVENTS}",
                "#{T(com.smartstock.common.event.Topics).PURCHASE_ORDER_EVENTS}",
                "#{T(com.smartstock.common.event.Topics).IDENTITY_EVENTS}"
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onEvent(DomainEventPayload payload) {
        if (payload == null || payload.eventId() == null) return;

        if (!idempotencyService.claim(CONSUMER_NAME, payload.eventId())) {
            log.debug("Skipping already-notified event (eventId={})", payload.eventId());
            return;
        }

        String subject = buildSubject(payload);
        if (subject == null) return; // event not actionable for notifications

        NotificationLog entry = NotificationLog.builder()
                .eventId(payload.eventId())
                .eventType(payload.eventType())
                .subject(subject)
                .body(buildBody(payload))
                .build();

        notificationLogRepository.save(entry);
        log.info("Notification logged: eventType={} subject={}", payload.eventType(), subject);
    }

    @DltHandler
    public void onDeadLetter(DomainEventPayload payload,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Notification event routed to DLT: topic={} eventId={} eventType={}",
                topic, payload != null ? payload.eventId() : "null",
                payload != null ? payload.eventType() : "null");
    }

    private String buildSubject(DomainEventPayload p) {
        return switch (p.eventType() != null ? p.eventType() : "") {
            case "LowStockThresholdReachedEvent" ->
                    "LOW STOCK ALERT: product " + p.productId() + " in warehouse " + p.warehouseId()
                    + " (qty=" + p.currentQuantity() + ")";
            case "SalesOrderConfirmedEvent" ->
                    "Sales order confirmed: " + p.soNumber();
            case "DeliveryCompletedEvent" ->
                    "Delivery completed for sales order: " + p.soNumber();
            case "PurchaseOrderCreatedEvent" ->
                    "New purchase order created: " + p.poNumber();
            case "DeliveryRegisteredEvent" ->
                    "Purchase order delivery received: " + p.poNumber();
            case "UserCreatedEvent" ->
                    "New user registered: " + p.username();
            default -> null; // not actionable
        };
    }

    private String buildBody(DomainEventPayload p) {
        return switch (p.eventType() != null ? p.eventType() : "") {
            case "LowStockThresholdReachedEvent" ->
                    "Product " + p.productId() + " has fallen below its reorder point "
                    + "(current qty=" + p.currentQuantity() + "). Please replenish.";
            case "DeliveryCompletedEvent" ->
                    "Order " + p.soNumber() + " for customer " + p.customerId()
                    + " has been delivered. Total: " + p.totalAmount();
            default -> "Event " + p.eventType() + " for aggregate " + p.aggregateId();
        };
    }
}
