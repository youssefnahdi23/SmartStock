package com.smartstock.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouse_zones",
       uniqueConstraints = @UniqueConstraint(name = "uq_zone_code", columnNames = {"warehouse_id", "zone_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseZone {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "zone_code", nullable = false, length = 100)
    private String code;

    @Column(name = "zone_name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "zone_type", nullable = false, length = 50)
    private String type;

    @Column(name = "area_sqm", precision = 12, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "capacity_units")
    private Integer capacityUnits;

    @Column(name = "current_utilization")
    private Integer currentUtilization;

    @Column(name = "temperature_controlled")
    private boolean temperatureControlled;

    @Column(name = "min_temperature", precision = 5, scale = 2)
    private BigDecimal minTemperature;

    @Column(name = "max_temperature", precision = 5, scale = 2)
    private BigDecimal maxTemperature;

    @Column(name = "temperature_unit", length = 10)
    private String temperatureUnit;

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

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseShelf> shelves = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (currentUtilization == null) currentUtilization = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public int getShelfCount() {
        return shelves == null ? 0 : (int) shelves.stream().filter(s -> !s.isDeleted()).count();
    }

    public int getTotalBinCount() {
        if (shelves == null) return 0;
        return shelves.stream()
                .filter(s -> !s.isDeleted())
                .mapToInt(WarehouseShelf::getBinCount)
                .sum();
    }

    public double getOccupancyPercentage() {
        if (capacityUnits == null || capacityUnits == 0) return 0.0;
        return (currentUtilization == null ? 0 : currentUtilization) * 100.0 / capacityUnits;
    }
}
