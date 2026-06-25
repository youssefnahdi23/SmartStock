package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_levels",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_warehouse",
                columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "quantity_on_hand", nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "quantity_damaged", nullable = false)
    @Builder.Default
    private Integer quantityDamaged = 0;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "reorder_point")
    @Builder.Default
    private Integer reorderPoint = 0;

    @Column(name = "reorder_quantity")
    @Builder.Default
    private Integer reorderQuantity = 0;

    @Column(name = "max_stock")
    private Integer maxStock;

    @Column(name = "last_moved_at")
    private Instant lastMovedAt;

    @Column(name = "last_counted_at")
    private Instant lastCountedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public int getQuantityAvailable() {
        return Math.max(0, quantityOnHand - quantityReserved);
    }

    public BigDecimal getInventoryValue() {
        return unitCost.multiply(BigDecimal.valueOf(quantityOnHand));
    }

    public boolean isLowStock() {
        return reorderPoint != null && reorderPoint > 0 && quantityOnHand <= reorderPoint;
    }

    public void receiveStock(int quantity, BigDecimal cost) {
        this.quantityOnHand += quantity;
        if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
            this.unitCost = cost;
        }
        this.lastMovedAt = Instant.now();
    }

    public void dispatchStock(int quantity) {
        if (quantity > getQuantityAvailable()) {
            throw new IllegalStateException("Insufficient available stock");
        }
        this.quantityOnHand -= quantity;
        this.lastMovedAt = Instant.now();
    }

    public void reserveStock(int quantity) {
        if (quantity > getQuantityAvailable()) {
            throw new IllegalStateException("Insufficient available stock to reserve");
        }
        this.quantityReserved += quantity;
    }

    public void releaseReservation(int quantity) {
        this.quantityReserved = Math.max(0, this.quantityReserved - quantity);
    }

    public void applyAdjustment(int adjustmentQty) {
        this.quantityOnHand = Math.max(0, this.quantityOnHand + adjustmentQty);
        this.lastMovedAt = Instant.now();
    }
}
