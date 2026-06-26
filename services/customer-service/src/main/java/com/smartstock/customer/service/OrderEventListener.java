package com.smartstock.customer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartstock.customer.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Consumes order events from events.order to keep customer purchase statistics
 * (totalOrders, totalSpent, averageOrderValue) current.
 * Wired to Sales Order Service events — activated when that service is live (M3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventListener {

    private final CustomerRepository customerRepository;

    @KafkaListener(topics = "events.order", groupId = "${spring.kafka.consumer.group-id}",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onOrderEvent(OrderCompletedPayload payload) {
        if (!"ORDER_COMPLETED".equals(payload.eventType())) {
            return;
        }
        if (payload.customerId() == null || payload.totalAmount() == null) {
            log.warn("Ignoring ORDER_COMPLETED with missing customerId or totalAmount");
            return;
        }
        customerRepository.findById(payload.customerId()).ifPresentOrElse(customer -> {
            customer.recordOrder(payload.totalAmount());
            customerRepository.save(customer);
            log.debug("Recorded order for customer {}: amount={}", payload.customerId(), payload.totalAmount());
        }, () -> log.warn("ORDER_COMPLETED received for unknown customerId={}", payload.customerId()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCompletedPayload(String eventType, String customerId, BigDecimal totalAmount) {}
}
