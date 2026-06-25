package com.smartstock.supplier.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "supplier_products",
        uniqueConstraints = @UniqueConstraint(name = "uq_supplier_product", columnNames = {"supplier_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "supplier_id", nullable = false, length = 36)
    private String supplierId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "supplier_product_code", length = 255)
    private String supplierProductCode;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "minimum_order_quantity")
    @Builder.Default
    private Integer minimumOrderQuantity = 1;

    @Column(name = "lead_time_days")
    @Builder.Default
    private Integer leadTimeDays = 7;

    @Column(name = "quality_rating", precision = 3, scale = 2)
    private BigDecimal qualityRating;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_ordered_at")
    private Instant lastOrderedAt;

    @Column(name = "total_quantity_ordered")
    @Builder.Default
    private Integer totalQuantityOrdered = 0;

    @Column(name = "total_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
