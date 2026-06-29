package com.smartstock.product.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateProductRequest {

    @Size(max = 500, message = "Product name must not exceed 500 characters")
    private String name;

    private String description;

    private String categoryId;

    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price must have at most 2 decimal places")
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit cost must have at most 2 decimal places")
    private BigDecimal unitCost;

    private String manufacturer;
    private String brand;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    private BigDecimal weight;

    private String weightUnit;

    @Valid
    private CreateProductRequest.DimensionsRequest dimensions;

    @Min(value = 0, message = "Reorder point must be non-negative")
    private Integer reorderPoint;

    @Min(value = 0, message = "Reorder quantity must be non-negative")
    private Integer reorderQuantity;

    @Min(value = 0, message = "Max stock must be non-negative")
    private Integer maxStock;

    private Map<String, String> attributes;
}
