package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConfirmDeliveryRequest {

    @NotNull(message = "Actual delivery date is required")
    private LocalDate actualDeliveryDate;

    @NotNull(message = "Quantity received is required")
    @Min(value = 0, message = "Quantity received cannot be negative")
    private Integer quantityReceived;

    @Min(value = 0, message = "Quantity rejected cannot be negative")
    private Integer quantityRejected = 0;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "10.0")
    private BigDecimal qualityRating;

    private Integer qualityIssuesFound = 0;

    @Pattern(regexp = "PASSED|FAILED|PARTIAL",
             message = "Quality inspection status must be one of: PASSED, FAILED, PARTIAL")
    private String qualityInspectionStatus = "PASSED";

    private String notes;
}
