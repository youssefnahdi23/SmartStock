package com.smartstock.sales.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SOLineItemResponse {
    private String lineId;
    private String productId;
    private String productName;
    private Integer quantity;
    private Integer pickedQuantity;
    private Integer shippedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal lineTotal;
    private String status;
    private String binLocation;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
