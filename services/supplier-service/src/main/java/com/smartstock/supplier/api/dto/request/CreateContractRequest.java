package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContractRequest {

    @NotBlank(message = "Contract number is required")
    @Size(max = 100)
    private String contractNumber;

    @NotBlank(message = "Contract title is required")
    @Size(max = 255)
    private String contractTitle;

    @NotNull(message = "Contract type is required")
    @Pattern(regexp = "PURCHASE_AGREEMENT|MASTER_SUPPLY|FRAMEWORK|SPOT|BLANKET",
             message = "Type must be one of: PURCHASE_AGREEMENT, MASTER_SUPPLY, FRAMEWORK, SPOT, BLANKET")
    private String contractType;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private LocalDate renewalDate;

    @DecimalMin(value = "0.0")
    private BigDecimal contractValue;

    private String paymentTerms;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal discountPercentage;

    @Min(0)
    private Integer minimumVolume;
}
