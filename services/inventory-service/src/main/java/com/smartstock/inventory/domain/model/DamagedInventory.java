package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "damaged_inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamagedInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "stock_movement_id", length = 36)
    private String stockMovementId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "damage_date", nullable = false)
    private Instant damageDate;

    @Column(name = "damage_reason", nullable = false, length = 200)
    private String damageReason;

    @Column(name = "reported_by", nullable = false, length = 36)
    private String reportedBy;

    @Column(name = "damage_severity", nullable = false, length = 50)
    private String damageSeverity;

    @Column(name = "salvage_value", precision = 12, scale = 2)
    private BigDecimal salvageValue;

    @Column(name = "is_resaleable")
    @Builder.Default
    private Boolean isResaleable = false;

    @Column(name = "action_taken", length = 200)
    private String actionTaken;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
