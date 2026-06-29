package com.smartstock.inventory.service;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.inventory.api.dto.request.StockInRequest;
import com.smartstock.inventory.event.payload.PurchaseOrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumes {@code purchase-order.events} and reacts to {@code DeliveryRegisteredEvent}:
 * for each received item in the delivery, calls {@link InventoryService#receiveStock} so
 * physical stock is updated atomically with the idempotency claim.
 *
 * <p>Idempotent: duplicate DeliveryRegistered messages are deduplicated by the
 * {@code processed_events} ledger — a redelivery skips the stock-in rather than
 * double-counting it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderEventListener {

    static final String DELIVERY_REGISTERED = "DeliveryRegisteredEvent";
    static final String CONSUMER_NAME = "inventory-po-listener";

    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "#{T(com.smartstock.common.event.Topics).PURCHASE_ORDER_EVENTS}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPurchaseOrderEvent(PurchaseOrderEventPayload payload) {
        if (payload == null || !DELIVERY_REGISTERED.equals(payload.eventType())) {
            return;
        }
        List<PurchaseOrderEventPayload.ReceivedItem> items = payload.receivedItems();
        if (items == null || items.isEmpty()) {
            log.warn("DeliveryRegisteredEvent has no receivedItems, skipping stock-in (eventId={})", payload.eventId());
            return;
        }
        // Dedupe per delivery × item: use eventId + productId as the compound key so each
        // product in a redelivered batch is independently idempotent.
        for (PurchaseOrderEventPayload.ReceivedItem item : items) {
            String claimKey = payload.eventId() + ":" + item.productId();
            if (!idempotencyService.claim(CONSUMER_NAME, claimKey)) {
                log.debug("Skipping already-processed stock-in: delivery={} product={}",
                        payload.deliveryId(), item.productId());
                continue;
            }
            if (item.receivedQuantity() <= 0) {
                continue;
            }
            StockInRequest req = new StockInRequest();
            req.setProductId(item.productId());
            req.setWarehouseId(payload.deliveryWarehouseId());
            req.setQuantity(item.receivedQuantity());
            req.setUnitCost(item.unitCost());
            req.setSupplierId(payload.supplierId());
            req.setReferenceType("PURCHASE_ORDER");
            req.setReferenceId(payload.aggregateId());
            req.setNotes("Auto stock-in from delivery " + payload.deliveryId());

            inventoryService.receiveStockInternal(req, payload.receivedBy() != null ? payload.receivedBy() : "system");
            log.info("Stock-in from PO delivery: product={} warehouse={} qty={} delivery={}",
                    item.productId(), payload.deliveryWarehouseId(), item.receivedQuantity(), payload.deliveryId());
        }
    }

    @DltHandler
    public void onDeadLetter(PurchaseOrderEventPayload payload,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("PurchaseOrderEvent routed to DLT after retries exhausted: topic={} eventId={} eventType={}",
                topic, payload != null ? payload.eventId() : "null",
                payload != null ? payload.eventType() : "null");
    }
}
