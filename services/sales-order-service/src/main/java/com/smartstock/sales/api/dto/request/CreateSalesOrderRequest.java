package com.smartstock.sales.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSalesOrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "SO number is required")
    @Size(max = 100)
    private String soNumber;

    private LocalDate orderDate;

    private LocalDate dueDate;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<CreateLineItemRequest> items;

    private String shippingAddress;

    private String shippingMethod;

    private String paymentTerms;

    private String notes;

    @Data
    public static class CreateLineItemRequest {

        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull @Min(1)
        private Integer quantity;

        @NotNull @DecimalMin("0.01")
        private BigDecimal unitPrice;

        @DecimalMin("0.0") @DecimalMax("100.0")
        private BigDecimal discount = BigDecimal.ZERO;

        private String notes;
    }
}
