package com.smartstock.supplier.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.supplier.config.KafkaConfig;
import com.smartstock.supplier.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes supplier domain events through the transactional outbox (debt C-2): written in the
 * caller's transaction, then relayed to Kafka by the {@code OutboxRelay}. Replaces the previous
 * fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class SupplierEventPublisher {

    private final OutboxService outbox;

    public void publishSupplierCreated(SupplierCreatedEvent event) { publish(event); }

    public void publishSupplierUpdated(SupplierUpdatedEvent event) { publish(event); }

    public void publishSupplierSuspended(SupplierSuspendedEvent event) { publish(event); }

    public void publishSupplierResumed(SupplierResumedEvent event) { publish(event); }

    public void publishPerformanceUpdated(SupplierPerformanceUpdatedEvent event) { publish(event); }

    public void publishDeliveryRegistered(SupplierDeliveryRegisteredEvent event) { publish(event); }

    public void publishSupplierQualityIssue(SupplierQualityIssueEvent event) { publish(event); }

    private void publish(Object event) {
        outbox.append(KafkaConfig.SUPPLIER_EVENTS_TOPIC, event);
    }
}
