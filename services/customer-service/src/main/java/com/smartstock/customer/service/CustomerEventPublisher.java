package com.smartstock.customer.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.customer.config.KafkaConfig;
import com.smartstock.customer.domain.event.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publishes customer domain events through the transactional outbox (debt C-2): written in the
 * caller's transaction, then relayed to Kafka by the {@code OutboxRelay}. Replaces the previous
 * fire-and-forget {@code @Async} send.
 */
@Service
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private final OutboxService outbox;

    public void publishCustomerCreated(CustomerCreatedEvent event) { publish(event); }

    public void publishCustomerUpdated(CustomerUpdatedEvent event) { publish(event); }

    public void publishCustomerSuspended(CustomerSuspendedEvent event) { publish(event); }

    public void publishCustomerResumed(CustomerResumedEvent event) { publish(event); }

    public void publishCustomerSegmentChanged(CustomerSegmentChangedEvent event) { publish(event); }

    private void publish(Object event) {
        outbox.append(KafkaConfig.CUSTOMER_EVENTS_TOPIC, event);
    }
}
