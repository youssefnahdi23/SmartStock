package com.smartstock.supplier.event.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

/**
 * Tolerant reader for {@code purchase-order.events} consumed by supplier-service.
 * Covers DeliveryRegisteredEvent and QualityIssueReportedEvent; unknown fields ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PurchaseOrderEventPayload(
        String eventId,
        String eventType,
        String aggregateId,

        // DeliveryRegisteredEvent
        String poNumber,
        String supplierId,
        String deliveryId,
        String deliveryWarehouseId,
        LocalDate deliveryDate,
        Integer totalReceivedQuantity,
        Integer damageCount,
        String receivedBy,

        // QualityIssueReportedEvent
        String issueId,
        String issueType,
        Integer quantity,
        String severity,
        String proposedResolution,
        String reportedBy
) {}
