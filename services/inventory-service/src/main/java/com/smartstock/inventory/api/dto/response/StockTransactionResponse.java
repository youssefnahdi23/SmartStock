package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class StockTransactionResponse {

    private String transactionId;
    private String productId;
    private String productName;
    private String warehouseId;
    private String warehouseName;
    private Integer quantity;
    private BigDecimal unitCost;
    private BigDecimal transactionValue;
    private Integer previousStockLevel;
    private Integer newStockLevel;
    private String transactionType;
    private String referenceType;
    private String referenceId;
    private String supplierId;
    private String customerId;
    private String userId;
    private Instant timestamp;
    private String notes;
}
