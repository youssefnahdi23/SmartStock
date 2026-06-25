package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TransactionResponse {

    private String transactionId;
    private String productId;
    private String productName;
    private String warehouseId;
    private String warehouseName;
    private Integer quantity;
    private Integer previousBalance;
    private Integer newBalance;
    private String transactionType;
    private String referenceType;
    private String referenceId;
    private String userId;
    private String username;
    private Instant timestamp;
    private String notes;
}
