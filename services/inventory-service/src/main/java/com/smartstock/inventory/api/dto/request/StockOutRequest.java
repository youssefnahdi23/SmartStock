package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StockOutRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotBlank(message = "warehouseId is required")
    private String warehouseId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private String referenceType;
    private String referenceId;
    private String customerId;
    private LocalDate shippingDate;
    private String notes;
}
