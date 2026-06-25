package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class DeliveryResponse {

    private String id;
    private String supplierId;
    private String purchaseOrderId;
    private String deliveryNumber;
    private LocalDate orderDate;
    private LocalDate promisedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private Integer quantityRejected;
    private String deliveryStatus;
    private Boolean onTime;
    private Integer onTimeDaysVariance;
    private String qualityInspectionStatus;
    private Integer qualityIssuesFound;
    private BigDecimal qualityRating;
    private String carrierName;
    private String trackingNumber;
    private BigDecimal totalValue;
    private String notes;
    private String receivedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
