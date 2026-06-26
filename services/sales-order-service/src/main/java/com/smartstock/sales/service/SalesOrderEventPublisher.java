package com.smartstock.sales.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.sales.config.KafkaConfig;
import com.smartstock.sales.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesOrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("salesOrderEventExecutor")
    public void publishSalesOrderCreated(SalesOrderCreatedEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishSalesOrderConfirmed(SalesOrderConfirmedEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishSalesOrderCancelled(SalesOrderCancelledEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishOrderPickingStarted(OrderPickingStartedEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishOrderPickingCompleted(OrderPickingCompletedEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishShipmentCreated(ShipmentCreatedEvent event) { publish(event); }

    @Async("salesOrderEventExecutor")
    public void publishDeliveryCompleted(DeliveryCompletedEvent event) { publish(event); }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.SALES_ORDER_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
