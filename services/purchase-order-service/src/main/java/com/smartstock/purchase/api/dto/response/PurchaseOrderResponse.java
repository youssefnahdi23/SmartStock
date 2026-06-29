package com.smartstock.purchase.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseOrderResponse {
    private String poId;
    private String poNumber;
    private String supplierId;
    private String supplierName;
    private String status;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private LocalDate expectedDeliveryDate;
    private String deliveryWarehouseId;
    private String shippingMethod;
    private String paymentTerms;
    private Integer totalQuantity;
    private Integer deliveredQuantity;
    private BigDecimal totalLineAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String deliveryStatus;
    private String paymentStatus;
    private Instant confirmationDate;
    private String confirmationNumber;
    private Instant cancelledAt;
    private String cancellationReason;
    private String notes;
    private List<POLineItemResponse> items;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
