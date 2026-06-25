package com.smartstock.supplier.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.supplier.config.KafkaConfig;
import com.smartstock.supplier.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("supplierEventExecutor")
    public void publishSupplierCreated(SupplierCreatedEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishSupplierUpdated(SupplierUpdatedEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishSupplierSuspended(SupplierSuspendedEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishSupplierResumed(SupplierResumedEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishPerformanceUpdated(SupplierPerformanceUpdatedEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishDeliveryRegistered(SupplierDeliveryRegisteredEvent event) { publish(event); }

    @Async("supplierEventExecutor")
    public void publishSupplierQualityIssue(SupplierQualityIssueEvent event) { publish(event); }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.SUPPLIER_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
