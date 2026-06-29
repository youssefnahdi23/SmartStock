package com.smartstock.notificationservice.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Tolerant reader for any SmartStock domain event. Only the minimum fields needed to
 * route and log notifications are extracted; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DomainEventPayload(
        String eventId,
        String eventType,
        String aggregateId,

        // Inventory: LowStockThresholdReachedEvent
        String productId,
        String warehouseId,
        Integer currentQuantity,

        // Sales order: SalesOrderConfirmedEvent / DeliveryCompletedEvent
        String soNumber,
        String customerId,
        BigDecimal totalAmount,

        // Purchase order: PurchaseOrderCreatedEvent / DeliveryRegisteredEvent
        String poNumber,
        String supplierId,

        // Identity: UserCreatedEvent
        String username,
        String email
) {}
