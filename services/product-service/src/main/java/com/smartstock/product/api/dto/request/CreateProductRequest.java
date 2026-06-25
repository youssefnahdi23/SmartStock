package com.smartstock.product.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 500, message = "Product name must not exceed 500 characters")
    private String name;

    @Size(max = 255, message = "SKU must not exceed 255 characters")
    private String sku;

    private String description;

    private String categoryId;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit price must have at most 2 decimal places")
    private BigDecimal unitPrice;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Unit cost must have at most 2 decimal places")
    private BigDecimal unitCost;

    @NotBlank(message = "Unit of measure is required")
    @Pattern(regexp = "PIECE|KG|L|M|BOX|UNIT", message = "Unit must be one of: PIECE, KG, L, M, BOX, UNIT")
    private String unit;

    private String manufacturer;
    private String brand;

    @DecimalMin(value = "0.0", message = "Weight must be non-negative")
    private BigDecimal weight;

    @Pattern(regexp = "KG|G|LB|OZ", message = "Weight unit must be one of: KG, G, LB, OZ")
    private String weightUnit;

    @Valid
    private DimensionsRequest dimensions;

    @Min(value = 0, message = "Reorder point must be non-negative")
    private Integer reorderPoint;

    @Min(value = 0, message = "Reorder quantity must be non-negative")
    private Integer reorderQuantity;

    @Min(value = 0, message = "Max stock must be non-negative")
    private Integer maxStock;

    private List<String> supplierIds;

    private Map<String, String> attributes;

    @Builder.Default
    private boolean active = true;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DimensionsRequest {
        @DecimalMin(value = "0.0") private BigDecimal length;
        @DecimalMin(value = "0.0") private BigDecimal width;
        @DecimalMin(value = "0.0") private BigDecimal height;
        @Pattern(regexp = "CM|M|IN|MM") private String unit;
    }
}
