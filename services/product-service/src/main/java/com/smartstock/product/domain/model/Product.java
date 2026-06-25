package com.smartstock.product.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_sku",        columnList = "sku",            unique = true),
        @Index(name = "idx_products_name",       columnList = "product_name"),
        @Index(name = "idx_products_active",     columnList = "is_active"),
        @Index(name = "idx_products_lifecycle",  columnList = "lifecycle_status"),
        @Index(name = "idx_products_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    private String id;

    @Column(name = "sku", nullable = false, unique = true, length = 255)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 500)
    private String name;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    @Builder.Default
    private String unitOfMeasure = "PIECE";

    @Column(name = "standard_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal standardCost = BigDecimal.ZERO;

    @Column(name = "standard_retail_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal standardRetailPrice = BigDecimal.ZERO;

    @Column(name = "weight", precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(name = "weight_unit", length = 10)
    private String weightUnit;

    @Column(name = "length", precision = 10, scale = 3)
    private BigDecimal length;

    @Column(name = "width", precision = 10, scale = 3)
    private BigDecimal width;

    @Column(name = "height", precision = 10, scale = 3)
    private BigDecimal height;

    @Column(name = "dimension_unit", length = 10)
    private String dimensionUnit;

    @Column(name = "reorder_level")
    @Builder.Default
    private int reorderLevel = 10;

    @Column(name = "reorder_quantity")
    @Builder.Default
    private int reorderQuantity = 50;

    @Column(name = "max_stock")
    @Builder.Default
    private int maxStock = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_discontinued")
    @Builder.Default
    private boolean discontinued = false;

    @Column(name = "discontinued_at")
    private LocalDateTime discontinuedAt;

    @Column(name = "lifecycle_status", length = 50)
    @Builder.Default
    private String lifecycleStatus = "ACTIVE";

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductCategory> productCategories = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductBarcode> barcodes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductSku> skus = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 36)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void deactivate(String userId) {
        this.active = false;
        this.discontinued = true;
        this.lifecycleStatus = "DISCONTINUED";
        this.discontinuedAt = LocalDateTime.now();
        this.updatedBy = userId;
    }

    public void reactivate(String userId) {
        this.active = true;
        this.discontinued = false;
        this.lifecycleStatus = "ACTIVE";
        this.discontinuedAt = null;
        this.updatedBy = userId;
    }

    public Category getPrimaryCategory() {
        return productCategories.stream()
                .filter(ProductCategory::isPrimary)
                .map(ProductCategory::getCategory)
                .findFirst()
                .orElse(null);
    }

    public ProductBarcode getPrimaryBarcode() {
        return barcodes.stream()
                .filter(ProductBarcode::isPrimary)
                .findFirst()
                .orElse(null);
    }
}
