package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CapacityAlertEvent extends DomainEvent {

    private String warehouseId;
    private String warehouseName;
    private String alertLevel;
    private String alertType;
    private double utilizationPercentage;
    private String message;

    public CapacityAlertEvent(String warehouseId, String warehouseName,
                               String alertLevel, double utilizationPercentage) {
        super(warehouseId, "Warehouse", "warehouse-service");
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.alertLevel = alertLevel;
        this.alertType = "CAPACITY_THRESHOLD";
        this.utilizationPercentage = utilizationPercentage;
        this.message = String.format("Warehouse capacity at %.1f%% — %s threshold reached",
                utilizationPercentage, alertLevel);
    }
}
