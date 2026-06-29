package com.smartstock.product.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ProductCreatedEvent extends DomainEvent {

    private String productId;
    private String sku;
    private String name;
    private String description;
    private String categoryId;
    private String categoryName;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private String unitOfMeasure;
    private BigDecimal weight;
    private DimensionsData dimensions;
    private String barcode;
    private String qrCode;
    private String createdBy;
    private String status;

    public ProductCreatedEvent(String productId, String sku, String name, String description,
                                String categoryId, String categoryName, BigDecimal unitPrice,
                                BigDecimal unitCost, String unitOfMeasure, BigDecimal weight,
                                String barcode, String createdBy) {
        super(productId, "Product", "product-service");
        this.productId = productId;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unitPrice = unitPrice;
        this.unitCost = unitCost;
        this.unitOfMeasure = unitOfMeasure;
        this.weight = weight;
        this.barcode = barcode;
        this.createdBy = createdBy;
        this.status = "ACTIVE";
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DimensionsData {
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal height;
        private String unit;
    }
}
