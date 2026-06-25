package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "inventory_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "quantity_held", nullable = false)
    private Integer quantityHeld;

    @Column(name = "hold_reason", nullable = false, length = 100)
    private String holdReason;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "held_at", nullable = false)
    private Instant heldAt;

    @Column(name = "held_by", nullable = false, length = 36)
    private String heldBy;

    @Column(name = "release_date")
    private Instant releaseDate;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by", length = 36)
    private String releasedBy;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
