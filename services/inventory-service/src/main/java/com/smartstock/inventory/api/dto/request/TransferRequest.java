package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransferRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotBlank(message = "fromWarehouseId is required")
    private String fromWarehouseId;

    @NotBlank(message = "toWarehouseId is required")
    private String toWarehouseId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private LocalDate transferDate;
    private String reason;
    private String notes;
}
