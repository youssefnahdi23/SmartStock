package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdjustmentRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotBlank(message = "warehouseId is required")
    private String warehouseId;

    @NotNull(message = "adjustmentQuantity is required")
    private Integer adjustmentQuantity;

    @NotBlank(message = "reason is required")
    private String reason;

    private String adjustmentType;
    private String notes;
    private String approverComments;
}
