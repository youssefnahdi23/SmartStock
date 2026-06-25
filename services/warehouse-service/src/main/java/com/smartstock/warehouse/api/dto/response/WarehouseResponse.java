package com.smartstock.warehouse.api.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private String type;

    private LocationData location;
    private CapacityData capacity;
    private ManagerData manager;
    private OperatingHoursData operatingHours;

    private boolean active;
    private String createdAt;
    private String updatedAt;
    private String deactivatedAt;

    private int zoneCount;
    private long totalBins;
    private long occupiedBins;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LocationData {
        private String address;
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CapacityData {
        private BigDecimal maxFloorSpace;
        private BigDecimal usedFloorSpace;
        private BigDecimal availableFloorSpace;
        private BigDecimal utilizationPercentage;
        private Integer maxPallets;
        private Integer usedPallets;
        private BigDecimal maxWeight;
        private BigDecimal usedWeight;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ManagerData {
        private String userId;
        private String email;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OperatingHoursData {
        private String mondayToFriday;
        private String saturday;
        private String sunday;
    }
}
