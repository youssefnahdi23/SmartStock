package com.smartstock.inventory.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tolerant reader for {@code purchase-order.events} messages consumed by inventory-service.
 * Unknown fields are silently ignored so the consumer survives producer-side additions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PurchaseOrderEventPayload(
        String eventId,
        String eventType,
        int    eventVersion,
        String aggregateId,

        // DeliveryRegisteredEvent fields
        String poNumber,
        String supplierId,
        String deliveryId,
        String deliveryWarehouseId,
        LocalDate deliveryDate,
        String receivedBy,
        List<ReceivedItem> receivedItems
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReceivedItem(String productId, int receivedQuantity, int damageCount, BigDecimal unitCost) {}
}
