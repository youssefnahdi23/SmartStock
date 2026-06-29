package com.smartstock.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_shelves",
       uniqueConstraints = @UniqueConstraint(name = "uq_shelf_code", columnNames = {"zone_id", "shelf_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseShelf {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private WarehouseZone zone;

    @Column(name = "shelf_code", nullable = false, length = 100)
    private String code;

    @Column(name = "shelf_name", nullable = false)
    private String name;

    @Column(name = "shelf_level")
    private Integer level;

    @Column(name = "capacity_units")
    private Integer capacityUnits;

    @Column(name = "current_load_units")
    private Integer currentLoadUnits;

    @Column(name = "max_weight_kg", precision = 12, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "current_weight_kg", precision = 12, scale = 2)
    private BigDecimal currentWeightKg;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 36, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 36, nullable = false)
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "shelf", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseBin> bins = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (currentLoadUnits == null) currentLoadUnits = 0;
        if (currentWeightKg == null) currentWeightKg = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public int getBinCount() {
        return bins == null ? 0 : (int) bins.stream().filter(b -> !b.isDeleted()).count();
    }

    public int getOccupiedBinCount() {
        if (bins == null) return 0;
        return (int) bins.stream().filter(b -> !b.isDeleted() && b.getCurrentUnits() > 0).count();
    }
}
