package com.smartstock.inventory.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockInRequest {

    @NotBlank(message = "productId is required")
    private String productId;

    @NotBlank(message = "warehouseId is required")
    private String warehouseId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.0", inclusive = true, message = "unitCost must be non-negative")
    private BigDecimal unitCost;

    private String referenceType;
    private String referenceId;
    private String supplierId;
    private LocalDate receiveDate;
    private String notes;
}
