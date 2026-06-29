package com.smartstock.sales.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.sales.config.KafkaConfig;
import com.smartstock.sales.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes sales-order domain events through the transactional outbox (debt C-2): the event
 * row commits in the same transaction as the order state change, then the {@code OutboxRelay}
 * delivers it to Kafka. Replaces the previous fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class SalesOrderEventPublisher {

    private final OutboxService outbox;

    public void publishSalesOrderCreated(SalesOrderCreatedEvent event) { publish(event); }

    public void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event) { publish(event); }

    public void publishSalesOrderCancelled(SalesOrderCancelledEvent event) { publish(event); }

    public void publishOrderPickingStarted(OrderPickingStartedEvent event) { publish(event); }

    public void publishOrderPickingCompleted(OrderPickingCompletedEvent event) { publish(event); }

    public void publishShipmentCreated(ShipmentCreatedEvent event) { publish(event); }

    public void publishDeliveryCompleted(DeliveryCompletedEvent event) { publish(event); }

    private void publish(Object event) {
        outbox.append(KafkaConfig.SALES_ORDER_EVENTS_TOPIC, event);
    }
}
