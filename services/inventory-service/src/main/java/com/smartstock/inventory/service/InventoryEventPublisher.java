package com.smartstock.inventory.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.inventory.config.KafkaConfig;
import com.smartstock.inventory.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes inventory domain events through the transactional outbox (debt C-2). Each method
 * runs synchronously inside the caller's transaction, so the event row commits atomically with
 * the inventory state change; the {@code OutboxRelay} delivers it to Kafka afterwards. No more
 * fire-and-forget {@code @Async} send with a swallowed exception.
 */
@Service
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final OutboxService outbox;

    public void publishStockIn(StockInEvent event) { publish(event); }

    public void publishStockOut(StockOutEvent event) { publish(event); }

    public void publishStockTransferred(StockTransferredEvent event) { publish(event); }

    public void publishStockAdjusted(StockAdjustedEvent event) { publish(event); }

    public void publishLowStockThreshold(LowStockThresholdReachedEvent event) { publish(event); }

    public void publishCountStarted(CountStartedEvent event) { publish(event); }

    public void publishCountCompleted(CountCompletedEvent event) { publish(event); }

    public void publishStockReserved(StockReservedEvent event) { publish(event); }

    private void publish(Object event) {
        outbox.append(KafkaConfig.INVENTORY_EVENTS_TOPIC, event);
    }
}
