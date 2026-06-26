package com.smartstock.purchase.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseOrderRequest {

    @NotBlank(message = "Supplier ID is required")
    private String supplierId;

    @NotBlank(message = "PO number is required")
    @Size(max = 100, message = "PO number must not exceed 100 characters")
    private String poNumber;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Expected delivery date is required")
    private LocalDate expectedDeliveryDate;

    @NotBlank(message = "Delivery warehouse ID is required")
    private String deliveryWarehouseId;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<CreateLineItemRequest> items;

    private String shippingMethod;

    @Size(max = 100, message = "Payment terms must not exceed 100 characters")
    private String paymentTerms;

    private String notes;

    @Data
    public static class CreateLineItemRequest {

        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        private java.math.BigDecimal unitPrice;

        @DecimalMin(value = "0.0", message = "Discount must be non-negative")
        @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
        private java.math.BigDecimal discount;

        private String notes;
    }
}
