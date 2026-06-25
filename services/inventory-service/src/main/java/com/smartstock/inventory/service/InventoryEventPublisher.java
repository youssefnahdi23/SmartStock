package com.smartstock.inventory.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.inventory.config.KafkaConfig;
import com.smartstock.inventory.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("inventoryEventExecutor")
    public void publishStockIn(StockInEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishStockOut(StockOutEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishStockTransferred(StockTransferredEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishStockAdjusted(StockAdjustedEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishLowStockThreshold(LowStockThresholdReachedEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishCountStarted(CountStartedEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishCountCompleted(CountCompletedEvent event) { publish(event); }

    @Async("inventoryEventExecutor")
    public void publishStockReserved(StockReservedEvent event) { publish(event); }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.INVENTORY_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
