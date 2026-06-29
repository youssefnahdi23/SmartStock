package com.smartstock.supplier.service;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.supplier.event.payload.PurchaseOrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes {@code purchase-order.events} and keeps the supplier record in sync:
 * <ul>
 *   <li>{@code DeliveryRegisteredEvent} → creates a {@code SupplierDelivery} row so the
 *       supplier's on-time-delivery metrics stay current without requiring a manual API call;</li>
 *   <li>{@code QualityIssueReportedEvent} → logged for audit; full quality tracking is done
 *       through the supplier delivery confirmation flow.</li>
 * </ul>
 * Both handlers are idempotent via the {@code processed_events} ledger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderEventListener {

    static final String DELIVERY_REGISTERED    = "DeliveryRegisteredEvent";
    static final String QUALITY_ISSUE_REPORTED = "QualityIssueReportedEvent";
    static final String CONSUMER_NAME          = "supplier-po-listener";

    private final SupplierDeliveryService deliveryService;
    private final IdempotencyService      idempotencyService;

    @KafkaListener(
            topics = "#{T(com.smartstock.common.event.Topics).PURCHASE_ORDER_EVENTS}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPurchaseOrderEvent(PurchaseOrderEventPayload payload) {
        if (payload == null) return;

        switch (payload.eventType() != null ? payload.eventType() : "") {
            case DELIVERY_REGISTERED    -> handleDeliveryRegistered(payload);
            case QUALITY_ISSUE_REPORTED -> handleQualityIssue(payload);
            default -> { /* other PO events ignored */ }
        }
    }

    private void handleDeliveryRegistered(PurchaseOrderEventPayload payload) {
        if (!idempotencyService.claim(CONSUMER_NAME, payload.eventId())) {
            log.debug("Skipping already-processed DeliveryRegisteredEvent (eventId={})", payload.eventId());
            return;
        }
        if (payload.supplierId() == null) {
            log.warn("DeliveryRegisteredEvent missing supplierId, skipping (eventId={})", payload.eventId());
            return;
        }
        deliveryService.registerDeliveryFromPurchaseOrderEvent(
                payload.supplierId(),
                payload.aggregateId(),
                payload.deliveryId(),
                payload.deliveryDate(),
                payload.totalReceivedQuantity() != null ? payload.totalReceivedQuantity() : 0,
                payload.receivedBy());
    }

    private void handleQualityIssue(PurchaseOrderEventPayload payload) {
        if (!idempotencyService.claim(CONSUMER_NAME + "-qi", payload.eventId())) {
            log.debug("Skipping already-processed QualityIssueReportedEvent (eventId={})", payload.eventId());
            return;
        }
        log.warn("Quality issue reported via PO event: supplierId={} issueId={} severity={} qty={}",
                payload.supplierId(), payload.issueId(), payload.severity(), payload.quantity());
    }

    @DltHandler
    public void onDeadLetter(PurchaseOrderEventPayload payload,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("PurchaseOrderEvent routed to DLT: topic={} eventId={} eventType={}",
                topic, payload != null ? payload.eventId() : "null",
                payload != null ? payload.eventType() : "null");
    }
}
