package com.smartstock.inventory.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_counts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "count_type", nullable = false, length = 50)
    private String countType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(name = "count_reason", length = 100)
    private String countReason;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "IN_PROGRESS";

    @Column(name = "expected_duration", length = 100)
    private String expectedDuration;

    @Column(name = "count_team", columnDefinition = "TEXT")
    private String countTeam;

    @Column(name = "total_items_counted")
    @Builder.Default
    private Integer totalItemsCounted = 0;

    @Column(name = "total_variances")
    @Builder.Default
    private Integer totalVariances = 0;

    @Column(name = "adjustments_created")
    @Builder.Default
    private Integer adjustmentsCreated = 0;

    @Column(name = "variance_rate", precision = 8, scale = 2)
    private java.math.BigDecimal varianceRate;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "completed_by", length = 36)
    private String completedBy;

    @Column(name = "approver_comments", columnDefinition = "TEXT")
    private String approverComments;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "count", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryCountItem> items = new ArrayList<>();
}
