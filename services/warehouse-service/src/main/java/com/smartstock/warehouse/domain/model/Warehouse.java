package com.smartstock.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "warehouse_code", unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "warehouse_name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "warehouse_type", length = 50)
    private String type;

    // Location
    @Column(name = "location_address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_province", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 2)
    private String country;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // Capacity
    @Column(name = "total_area_sqm", precision = 12, scale = 2)
    private BigDecimal totalAreaSqm;

    @Column(name = "total_capacity_units")
    private Integer totalCapacityUnits;

    @Column(name = "current_utilization_percentage", precision = 5, scale = 2)
    private BigDecimal currentUtilizationPercentage;

    @Column(name = "max_weight_kg", precision = 12, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "used_weight_kg", precision = 12, scale = 2)
    private BigDecimal usedWeightKg;

    // Contact
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "manager_id", length = 36)
    private String managerId;

    @Column(name = "manager_email")
    private String managerEmail;

    // Operating hours (stored as simple strings)
    @Column(name = "hours_monday_friday", length = 20)
    private String hoursMondayFriday;

    @Column(name = "hours_saturday", length = 20)
    private String hoursSaturday;

    @Column(name = "hours_sunday", length = 20)
    private String hoursSunday;

    // Capabilities
    @Column(name = "temperature_controlled")
    private boolean temperatureControlled;

    @Column(name = "min_temperature", precision = 5, scale = 2)
    private BigDecimal minTemperature;

    @Column(name = "max_temperature", precision = 5, scale = 2)
    private BigDecimal maxTemperature;

    @Column(name = "hazmat_capable")
    private boolean hazmatCapable;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    // Audit
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

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WarehouseZone> zones = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (currentUtilizationPercentage == null) currentUtilizationPercentage = BigDecimal.ZERO;
        if (usedWeightKg == null) usedWeightKg = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void deactivate(String userId) {
        this.active = false;
        this.deactivatedAt = Instant.now();
        this.updatedBy = userId;
    }

    public void reactivate(String userId) {
        this.active = true;
        this.deactivatedAt = null;
        this.updatedBy = userId;
    }

    public int getZoneCount() {
        return zones == null ? 0 : (int) zones.stream().filter(z -> !z.isDeleted()).count();
    }
}
