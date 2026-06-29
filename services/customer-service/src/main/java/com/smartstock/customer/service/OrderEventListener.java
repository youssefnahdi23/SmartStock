package com.smartstock.customer.service;

import com.smartstock.common.consumer.IdempotencyService;
import com.smartstock.customer.domain.repository.CustomerRepository;
import com.smartstock.customer.event.SalesOrderEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps customer purchase statistics (totalOrders, totalSpent, averageOrderValue) current
 * by consuming sales-order completion events.
 *
 * <p>Wiring corrected per debt C-4: the previous listener subscribed to {@code events.order}
 * for an {@code ORDER_COMPLETED} type that <em>no producer emits</em>, so the flow never
 * ran. It now subscribes to the canonical {@link com.smartstock.common.event.Topics#SALES_ORDER_EVENTS}
 * stream and records spend on {@code DeliveryCompletedEvent} — the order's terminal state —
 * which avoids the double-count hazard of recording on creation (a cancelled order never
 * delivers). The order value rides on the event itself ({@code totalAmount}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    /** Terminal sales-order event on which an order counts toward customer spend. */
    static final String ORDER_COMPLETED_EVENT = "DeliveryCompletedEvent";

    /** Consumer identity for the idempotency ledger. */
    static final String CONSUMER_NAME = "customer-order-listener";

    private final CustomerRepository customerRepository;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "#{T(com.smartstock.common.event.Topics).SALES_ORDER_EVENTS}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onSalesOrderEvent(SalesOrderEventPayload payload) {
        if (payload == null || !ORDER_COMPLETED_EVENT.equals(payload.eventType())) {
            return;
        }
        if (payload.customerId() == null || payload.totalAmount() == null) {
            log.warn("Ignoring {} with missing customerId or totalAmount (eventId={})",
                    ORDER_COMPLETED_EVENT, payload.eventId());
            return;
        }
        // Dedupe at-least-once redeliveries in the same transaction as the spend update, so a
        // redelivered DeliveryCompletedEvent cannot double-count (debt H-3).
        if (!idempotencyService.claim(CONSUMER_NAME, payload.eventId())) {
            log.debug("Skipping already-processed {} (eventId={})", ORDER_COMPLETED_EVENT, payload.eventId());
            return;
        }
        customerRepository.findById(payload.customerId()).ifPresentOrElse(customer -> {
            customer.recordOrder(payload.totalAmount());
            customerRepository.save(customer);
            log.debug("Recorded completed order for customer {}: amount={}",
                    payload.customerId(), payload.totalAmount());
        }, () -> log.warn("{} received for unknown customerId={}",
                ORDER_COMPLETED_EVENT, payload.customerId()));
    }
}
