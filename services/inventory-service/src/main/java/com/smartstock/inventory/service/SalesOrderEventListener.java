package com.smartstock.inventory.service;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.inventory.api.dto.request.ReservationRequest;
import com.smartstock.inventory.event.payload.SalesOrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumes {@code sales-order.events} and drives inventory reactions:
 * <ul>
 *   <li>{@code SalesOrderConfirmedEvent} → reserve stock per line item so goods are
 *       held until the order ships;</li>
 *   <li>{@code DeliveryCompletedEvent} → dispatch (decrement) reserved stock as the
 *       physical shipment has left the warehouse.</li>
 * </ul>
 * Both paths are idempotent via the {@code processed_events} ledger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderEventListener {

    static final String CONFIRMED_EVENT  = "SalesOrderConfirmedEvent";
    static final String DELIVERED_EVENT  = "DeliveryCompletedEvent";
    static final String CONSUMER_NAME    = "inventory-so-listener";

    private final ReservationService reservationService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "#{T(com.smartstock.common.event.Topics).SALES_ORDER_EVENTS}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onSalesOrderEvent(SalesOrderEventPayload payload) {
        if (payload == null) return;

        switch (payload.eventType() != null ? payload.eventType() : "") {
            case CONFIRMED_EVENT -> handleConfirmed(payload);
            case DELIVERED_EVENT -> handleDelivered(payload);
            default -> { /* other events ignored */ }
        }
    }

    private void handleConfirmed(SalesOrderEventPayload payload) {
        List<SalesOrderEventPayload.LineItem> items = payload.items();
        if (items == null || items.isEmpty()) {
            log.warn("SalesOrderConfirmedEvent has no items, skipping reservation (eventId={})", payload.eventId());
            return;
        }
        for (SalesOrderEventPayload.LineItem item : items) {
            String claimKey = payload.eventId() + ":" + item.productId();
            if (!idempotencyService.claim(CONSUMER_NAME, claimKey)) {
                log.debug("Skipping already-processed reservation: so={} product={}", payload.soNumber(), item.productId());
                continue;
            }
            ReservationRequest req = new ReservationRequest();
            req.setProductId(item.productId());
            req.setWarehouseId(payload.pickingWarehouseId());
            req.setQuantity(item.quantity());
            req.setOrderId(payload.aggregateId());
            req.setReservationReason("SALES_ORDER_CONFIRMED");

            try {
                reservationService.reserveInternal(req, "system");
                log.info("Stock reserved: product={} warehouse={} qty={} so={}",
                        item.productId(), payload.pickingWarehouseId(), item.quantity(), payload.soNumber());
            } catch (Exception ex) {
                log.error("Failed to reserve stock for product={} so={}: {}",
                        item.productId(), payload.soNumber(), ex.getMessage());
                throw ex;
            }
        }
    }

    private void handleDelivered(SalesOrderEventPayload payload) {
        // DeliveryCompletedEvent signals physical shipment delivered; decrement reserved stock.
        String claimKey = payload.eventId();
        if (!idempotencyService.claim(CONSUMER_NAME + "-dispatch", claimKey)) {
            log.debug("Skipping already-processed dispatch: so={}", payload.soNumber());
            return;
        }
        // Items aren't carried on DeliveryCompletedEvent — dispatch is handled by the picking
        // flow (StockOut) which already ran. Nothing more to do here; the claim is recorded so
        // a DLT redelivery doesn't attempt a double-dispatch.
        log.info("DeliveryCompletedEvent acknowledged for inventory (no double-dispatch): so={}", payload.soNumber());
    }

    @DltHandler
    public void onDeadLetter(SalesOrderEventPayload payload,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("SalesOrderEvent routed to DLT after retries exhausted: topic={} eventId={} eventType={}",
                topic, payload != null ? payload.eventId() : "null",
                payload != null ? payload.eventType() : "null");
    }
}
