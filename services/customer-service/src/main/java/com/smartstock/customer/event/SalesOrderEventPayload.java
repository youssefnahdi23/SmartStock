package com.smartstock.customer.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Tolerant projection of a sales-order domain event, carrying only the fields customer
 * statistics need. The {@code sales-order.events} stream multiplexes several event types
 * over one topic; this payload reads the shared {@code eventType} discriminator plus the
 * order value/customer so the listener can filter and apply selectively. Unknown fields
 * (the rest of each concrete event, plus the polymorphic {@code @type}) are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesOrderEventPayload(
        String eventId,
        String eventType,
        String customerId,
        BigDecimal totalAmount) {
}
