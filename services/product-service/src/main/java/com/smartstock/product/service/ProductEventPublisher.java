package com.smartstock.product.service;

import com.smartstock.product.config.KafkaConfig;
import com.smartstock.product.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishProductCreated(ProductCreatedEvent event) {
        publish(event.getProductId(), event);
    }

    @Async
    public void publishProductUpdated(ProductUpdatedEvent event) {
        publish(event.getProductId(), event);
    }

    @Async
    public void publishProductDeactivated(ProductDeletedEvent event) {
        publish(event.getProductId(), event);
    }

    @Async
    public void publishProductReactivated(ProductReactivatedEvent event) {
        publish(event.getProductId(), event);
    }

    @Async
    public void publishBarcodeGenerated(BarcodeGeneratedEvent event) {
        publish(event.getProductId(), event);
    }

    private void publish(String key, Object payload) {
        kafkaTemplate.send(KafkaConfig.PRODUCT_EVENTS_TOPIC, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for key={}: {}", key, ex.getMessage());
                    } else {
                        log.debug("Event published: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
