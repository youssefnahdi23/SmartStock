package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TransferResponse {

    private String transferId;
    private String productId;
    private String productName;
    private String fromWarehouseId;
    private String fromWarehouseName;
    private String toWarehouseId;
    private String toWarehouseName;
    private Integer quantity;
    private Integer fromStockBefore;
    private Integer fromStockAfter;
    private Integer toStockBefore;
    private Integer toStockAfter;
    private String status;
    private String reason;
    private String userId;
    private Instant createdAt;
    private String notes;
}
