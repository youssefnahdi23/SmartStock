package com.smartstock.purchase.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelPurchaseOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    private String notes;
}
