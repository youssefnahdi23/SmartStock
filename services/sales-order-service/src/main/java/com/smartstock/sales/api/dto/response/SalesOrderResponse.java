package com.smartstock.sales.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SalesOrderResponse {
    private String soId;
    private String soNumber;
    private String customerId;
    private String customerName;
    private String status;
    private String fulfillmentStatus;
    private String paymentStatus;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private String pickingWarehouseId;
    private String shippingAddress;
    private String shippingMethod;
    private String paymentTerms;
    private Integer totalQuantity;
    private Integer pickedQuantity;
    private Integer shippedQuantity;
    private BigDecimal totalLineAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private Instant confirmationDate;
    private Instant cancelledAt;
    private String cancellationReason;
    private String notes;
    private List<SOLineItemResponse> items;
    private List<ShipmentResponse> shipments;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
