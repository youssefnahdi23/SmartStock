package com.smartstock.purchase.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class POLineItemResponse {
    private String lineId;
    private String productId;
    private String productName;
    private Integer quantity;
    private Integer receivedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercentage;
    private BigDecimal lineAmount;
    private String status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
