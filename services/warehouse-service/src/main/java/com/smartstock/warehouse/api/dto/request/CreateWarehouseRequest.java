package com.smartstock.warehouse.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWarehouseRequest {

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 100, message = "Code must not exceed 100 characters")
    private String code;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    private String type;

    @Valid
    private LocationRequest location;

    @Valid
    private CapacityRequest capacity;

    @Valid
    private ManagerRequest manager;

    @Valid
    private OperatingHoursRequest operatingHours;

    private boolean active = true;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationRequest {
        @NotBlank(message = "Address is required")
        private String address;
        @NotBlank(message = "City is required")
        private String city;
        private String state;
        private String country;
        private String zipCode;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CapacityRequest {
        private Double maxFloorSpace;
        private String floorSpaceUnit;
        private Integer maxPallets;
        private Double maxWeight;
        private String weightUnit;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManagerRequest {
        private String userId;
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OperatingHoursRequest {
        private String mondayToFriday;
        private String saturday;
        private String sunday;
    }
}
