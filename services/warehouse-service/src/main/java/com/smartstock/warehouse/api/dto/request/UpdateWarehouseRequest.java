package com.smartstock.warehouse.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWarehouseRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String description;

    @Valid
    private CreateWarehouseRequest.ManagerRequest manager;

    @Valid
    private CreateWarehouseRequest.OperatingHoursRequest operatingHours;

    @Valid
    private CreateWarehouseRequest.CapacityRequest capacity;
}
