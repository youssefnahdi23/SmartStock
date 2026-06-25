package com.smartstock.supplier.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RegisterDeliveryRequest {

    @NotBlank(message = "Delivery number is required")
    @Size(max = 100)
    private String deliveryNumber;

    private String purchaseOrderId;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @NotNull(message = "Promised delivery date is required")
    private LocalDate promisedDeliveryDate;

    @NotNull(message = "Quantity ordered is required")
    @Min(value = 1, message = "Quantity ordered must be at least 1")
    private Integer quantityOrdered;

    @DecimalMin(value = "0.0")
    private BigDecimal totalValue;

    private String carrierName;
    private String trackingNumber;
    private String notes;
}
