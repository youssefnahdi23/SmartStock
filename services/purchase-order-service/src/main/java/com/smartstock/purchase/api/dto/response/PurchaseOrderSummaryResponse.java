package com.smartstock.purchase.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class PurchaseOrderSummaryResponse {
    private String poId;
    private String poNumber;
    private String supplierId;
    private String supplierName;
    private String status;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private String deliveryStatus;
    private String paymentStatus;
    private Instant createdAt;
}
