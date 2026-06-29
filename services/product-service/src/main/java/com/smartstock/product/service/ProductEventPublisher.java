package com.smartstock.product.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.product.config.KafkaConfig;
import com.smartstock.product.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes product domain events through the transactional outbox (debt C-2): written in the
 * caller's transaction, then relayed to Kafka by the {@code OutboxRelay}. Replaces the previous
 * fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final OutboxService outbox;

    public void publishProductCreated(ProductCreatedEvent event) {
        publish(event.getProductId(), event);
    }

    public void publishProductUpdated(ProductUpdatedEvent event) {
        publish(event.getProductId(), event);
    }

    public void publishProductDeactivated(ProductDeletedEvent event) {
        publish(event.getProductId(), event);
    }

    public void publishProductReactivated(ProductReactivatedEvent event) {
        publish(event.getProductId(), event);
    }

    public void publishBarcodeGenerated(BarcodeGeneratedEvent event) {
        publish(event.getProductId(), event);
    }

    private void publish(String key, Object payload) {
        outbox.append(KafkaConfig.PRODUCT_EVENTS_TOPIC, key, payload);
    }
}
