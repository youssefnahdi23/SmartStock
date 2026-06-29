package com.smartstock.product.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_attributes", indexes = {
        @Index(name = "idx_attributes_product_id",   columnList = "product_id"),
        @Index(name = "idx_attributes_name",         columnList = "attribute_name"),
        @Index(name = "idx_attributes_searchable",   columnList = "is_searchable")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductAttribute {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "attribute_name", nullable = false, length = 255)
    private String name;

    @Column(name = "attribute_value", nullable = false, length = 500)
    private String value;

    @Column(name = "attribute_type", length = 50)
    @Builder.Default
    private String type = "TEXT";

    @Column(name = "sort_order")
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "is_searchable")
    @Builder.Default
    private boolean searchable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
}
