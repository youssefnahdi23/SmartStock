package com.smartstock.sales.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ShipmentResponse {
    private String shipmentId;
    private String soId;
    private String shipmentNumber;
    private String carrierName;
    private String trackingNumber;
    private String shippingMethod;
    private Integer shippedQuantity;
    private LocalDate shipDate;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String status;
    private String signedBy;
    private String deliveryNotes;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant createdAt;
}
