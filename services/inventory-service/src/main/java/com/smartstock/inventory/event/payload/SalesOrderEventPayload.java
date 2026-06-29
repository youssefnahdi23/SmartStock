package com.smartstock.inventory.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tolerant reader for {@code sales-order.events} messages consumed by inventory-service.
 * Covers both {@code SalesOrderConfirmedEvent} (reserve) and {@code DeliveryCompletedEvent} (dispatch).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesOrderEventPayload(
        String eventId,
        String eventType,
        int    eventVersion,
        String aggregateId,

        // SalesOrderConfirmedEvent fields
        String soNumber,
        String customerId,
        String pickingWarehouseId,
        List<LineItem> items,

        // DeliveryCompletedEvent fields
        BigDecimal totalAmount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LineItem(String productId, int quantity) {}
}
