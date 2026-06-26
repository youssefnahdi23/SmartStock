package com.smartstock.sales.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelSalesOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;

    private String notes;
}
