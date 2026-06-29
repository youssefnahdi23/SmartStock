package com.smartstock.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "warehouse_bins",
       uniqueConstraints = @UniqueConstraint(name = "uq_bin_code", columnNames = {"bin_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseBin {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private WarehouseShelf shelf;

    @Column(name = "bin_code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "bin_name")
    private String name;

    @Column(name = "bin_number")
    private Integer number;

    @Column(name = "bin_position", length = 50)
    private String position;

    @Column(name = "bin_type", length = 50)
    private String type;

    @Column(name = "capacity_units")
    private Integer capacityUnits;

    @Column(name = "current_units")
    private Integer currentUnits;

    @Column(name = "max_weight_kg", precision = 12, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "current_weight_kg", precision = 12, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_full")
    private boolean full;

    // Reference to product stored here (cross-service UUID, no FK)
    @Column(name = "current_product_id", length = 36)
    private String currentProductId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (currentUnits == null) currentUnits = 0;
        if (currentWeightKg == null) currentWeightKg = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        this.full = (capacityUnits != null && currentUnits != null && currentUnits >= capacityUnits);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public int getAvailableCapacity() {
        if (capacityUnits == null) return 0;
        return capacityUnits - (currentUnits == null ? 0 : currentUnits);
    }

    public boolean hasAvailableSpace(int quantity) {
        return getAvailableCapacity() >= quantity;
    }
}
