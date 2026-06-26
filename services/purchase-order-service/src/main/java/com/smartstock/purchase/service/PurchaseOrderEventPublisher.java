package com.smartstock.purchase.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.purchase.config.KafkaConfig;
import com.smartstock.purchase.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("purchaseOrderEventExecutor")
    public void publishPurchaseOrderCreated(PurchaseOrderCreatedEvent event) { publish(event); }

    @Async("purchaseOrderEventExecutor")
    public void publishPurchaseOrderConfirmed(PurchaseOrderConfirmedEvent event) { publish(event); }

    @Async("purchaseOrderEventExecutor")
    public void publishDeliveryRegistered(DeliveryRegisteredEvent event) { publish(event); }

    @Async("purchaseOrderEventExecutor")
    public void publishPurchaseOrderCancelled(PurchaseOrderCancelledEvent event) { publish(event); }

    @Async("purchaseOrderEventExecutor")
    public void publishQualityIssueReported(QualityIssueReportedEvent event) { publish(event); }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.PURCHASE_ORDER_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
