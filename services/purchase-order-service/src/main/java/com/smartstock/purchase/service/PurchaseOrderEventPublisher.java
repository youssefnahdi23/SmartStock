package com.smartstock.purchase.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.purchase.config.KafkaConfig;
import com.smartstock.purchase.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes purchase-order domain events through the transactional outbox (debt C-2): written in
 * the caller's transaction, then relayed to Kafka by the {@code OutboxRelay}. Replaces the
 * previous fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderEventPublisher {

    private final OutboxService outbox;

    public void publishPurchaseOrderCreated(PurchaseOrderCreatedEvent event) { publish(event); }

    public void publishPurchaseOrderConfirmed(PurchaseOrderConfirmedEvent event) { publish(event); }

    public void publishDeliveryRegistered(DeliveryRegisteredEvent event) { publish(event); }

    public void publishPurchaseOrderCancelled(PurchaseOrderCancelledEvent event) { publish(event); }

    public void publishQualityIssueReported(QualityIssueReportedEvent event) { publish(event); }

    private void publish(Object event) {
        outbox.append(KafkaConfig.PURCHASE_ORDER_EVENTS_TOPIC, event);
    }
}
