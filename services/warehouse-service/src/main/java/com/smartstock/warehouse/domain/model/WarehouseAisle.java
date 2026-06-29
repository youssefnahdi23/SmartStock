package com.smartstock.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "warehouse_aisles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseAisle {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_zone_id", nullable = false)
    private WarehouseZone zone;

    @Column(name = "aisle_number", nullable = false)
    private Integer aisleNumber;

    @Column(name = "aisle_length_m", precision = 8, scale = 2)
    private BigDecimal aisleLengthM;

    @Column(name = "shelf_count", nullable = false)
    @Builder.Default
    private int shelfCount = 0;

    @Column(name = "total_bins", nullable = false)
    @Builder.Default
    private int totalBins = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
