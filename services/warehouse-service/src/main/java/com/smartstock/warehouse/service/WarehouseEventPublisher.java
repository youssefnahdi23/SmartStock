package com.smartstock.warehouse.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.warehouse.config.KafkaConfig;
import com.smartstock.warehouse.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes warehouse domain events through the transactional outbox (debt C-2): written in the
 * caller's transaction, then relayed to Kafka by the {@code OutboxRelay}. Replaces the previous
 * fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class WarehouseEventPublisher {

    private final OutboxService outbox;

    public void publishWarehouseCreated(WarehouseCreatedEvent event) {
        publish(event);
    }

    public void publishWarehouseUpdated(WarehouseUpdatedEvent event) {
        publish(event);
    }

    public void publishWarehouseDeactivated(WarehouseDeactivatedEvent event) {
        publish(event);
    }

    public void publishZoneCreated(ZoneCreatedEvent event) {
        publish(event);
    }

    public void publishShelfCreated(ShelfCreatedEvent event) {
        publish(event);
    }

    public void publishBinCreated(BinCreatedEvent event) {
        publish(event);
    }

    public void publishCapacityUpdated(WarehouseCapacityUpdatedEvent event) {
        publish(event);
    }

    private void publish(Object event) {
        outbox.append(KafkaConfig.WAREHOUSE_EVENTS_TOPIC, event);
    }
}
