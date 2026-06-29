package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable ledger of every inventory movement. No UPDATE or DELETE permitted after insert.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "movement_total", precision = 15, scale = 2)
    private BigDecimal movementTotal;

    @Column(name = "previous_balance")
    private Integer previousBalance;

    @Column(name = "new_balance")
    private Integer newBalance;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "movement_reason", length = 200)
    private String movementReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "actor_id", nullable = false, length = 36)
    private String actorId;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
