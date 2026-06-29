package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WarehouseCapacityUpdatedEvent extends DomainEvent {

    private String warehouseId;
    private String warehouseName;
    private BigDecimal totalCapacityVolume;
    private BigDecimal usedCapacityVolume;
    private BigDecimal percentageUsed;
    private String triggeredBy;

    public WarehouseCapacityUpdatedEvent(String warehouseId, String warehouseName,
                                          BigDecimal totalCapacityVolume, BigDecimal usedCapacityVolume,
                                          BigDecimal percentageUsed, String triggeredBy) {
        super(warehouseId, "Warehouse", "warehouse-service");
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.totalCapacityVolume = totalCapacityVolume;
        this.usedCapacityVolume = usedCapacityVolume;
        this.percentageUsed = percentageUsed;
        this.triggeredBy = triggeredBy;
    }
}
