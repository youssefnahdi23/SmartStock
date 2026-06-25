package com.smartstock.warehouse.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.warehouse.config.KafkaConfig;
import com.smartstock.warehouse.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("warehouseEventExecutor")
    public void publishWarehouseCreated(WarehouseCreatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishWarehouseUpdated(WarehouseUpdatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishWarehouseDeactivated(WarehouseDeactivatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishZoneCreated(ZoneCreatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishShelfCreated(ShelfCreatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishBinCreated(BinCreatedEvent event) {
        publish(event);
    }

    @Async("warehouseEventExecutor")
    public void publishCapacityAlert(CapacityAlertEvent event) {
        publish(event);
    }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.WAREHOUSE_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
