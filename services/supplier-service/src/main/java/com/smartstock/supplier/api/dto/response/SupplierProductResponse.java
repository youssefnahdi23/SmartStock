package com.smartstock.supplier.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SupplierProductResponse {

    private String id;
    private String supplierId;
    private String productId;
    private String supplierProductCode;
    private BigDecimal unitPrice;
    private Integer minimumOrderQuantity;
    private Integer leadTimeDays;
    private BigDecimal qualityRating;
    private Boolean isActive;
    private Instant lastOrderedAt;
    private Integer totalQuantityOrdered;
    private BigDecimal totalSpent;
    private Instant createdAt;
    private Instant updatedAt;
}
