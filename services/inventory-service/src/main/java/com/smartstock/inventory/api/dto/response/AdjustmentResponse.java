package com.smartstock.inventory.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdjustmentResponse {

    private String adjustmentId;
    private String productId;
    private String productName;
    private String warehouseId;
    private String warehouseName;
    private Integer adjustmentQuantity;
    private String reason;
    private String adjustmentType;
    private Integer previousStockLevel;
    private Integer newStockLevel;
    private String status;
    private String createdBy;
    private String approvedBy;
    private Instant createdAt;
    private Instant approvedAt;
    private String notes;
    private String approverComments;
}
