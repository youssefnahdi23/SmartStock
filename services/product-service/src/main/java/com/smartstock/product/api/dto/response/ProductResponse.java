package com.smartstock.product.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductResponse {

    private String id;
    private String name;
    private String sku;
    private String description;
    private String manufacturer;
    private String brand;
    private String categoryId;
    private String categoryName;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private String unit;
    private BigDecimal weight;
    private String weightUnit;
    private DimensionsResponse dimensions;
    private int reorderPoint;
    private int reorderQuantity;
    private int maxStock;
    private List<String> supplierIds;
    private String barcode;
    private String barcodeFormat;
    private String qrCode;
    private Map<String, String> attributes;
    private boolean active;
    private String lifecycleStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DimensionsResponse {
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal height;
        private String unit;
    }
}
