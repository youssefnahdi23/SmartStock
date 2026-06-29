package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "stock_transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "stock_movement_id", nullable = false, length = 36)
    private String stockMovementId;

    @Column(name = "from_warehouse_id", nullable = false, length = 36)
    private String fromWarehouseId;

    @Column(name = "to_warehouse_id", nullable = false, length = 36)
    private String toWarehouseId;

    @Column(name = "transfer_status", length = 50)
    @Builder.Default
    private String transferStatus = "PENDING";

    @Column(name = "transfer_reason", length = 100)
    private String transferReason;

    @Column(name = "from_stock_before")
    private Integer fromStockBefore;

    @Column(name = "from_stock_after")
    private Integer fromStockAfter;

    @Column(name = "to_stock_before")
    private Integer toStockBefore;

    @Column(name = "to_stock_after")
    private Integer toStockAfter;

    @Column(name = "shipped_by", length = 36)
    private String shippedBy;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "received_by", length = 36)
    private String receivedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
