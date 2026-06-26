package com.smartstock.customer.service;

import com.smartstock.common.event.DomainEvent;
import com.smartstock.customer.config.KafkaConfig;
import com.smartstock.customer.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("customerEventExecutor")
    public void publishCustomerCreated(CustomerCreatedEvent event) { publish(event); }

    @Async("customerEventExecutor")
    public void publishCustomerUpdated(CustomerUpdatedEvent event) { publish(event); }

    @Async("customerEventExecutor")
    public void publishCustomerSuspended(CustomerSuspendedEvent event) { publish(event); }

    @Async("customerEventExecutor")
    public void publishCustomerResumed(CustomerResumedEvent event) { publish(event); }

    @Async("customerEventExecutor")
    public void publishCustomerSegmentChanged(CustomerSegmentChangedEvent event) { publish(event); }

    private void publish(Object event) {
        try {
            String aggregateId = (event instanceof DomainEvent de) ? de.getAggregateId() : "unknown";
            kafkaTemplate.send(KafkaConfig.CUSTOMER_EVENTS_TOPIC, aggregateId, event);
            log.debug("Published event {} for aggregate {}", event.getClass().getSimpleName(), aggregateId);
        } catch (Exception ex) {
            log.error("Failed to publish event {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
