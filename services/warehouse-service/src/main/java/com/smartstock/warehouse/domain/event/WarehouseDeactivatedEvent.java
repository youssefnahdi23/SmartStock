package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WarehouseDeactivatedEvent extends DomainEvent {

    private String warehouseId;
    private String code;
    private String name;
    private String deactivatedBy;

    public WarehouseDeactivatedEvent(String warehouseId, String code, String name, String deactivatedBy) {
        super(warehouseId, "Warehouse", "warehouse-service");
        this.warehouseId = warehouseId;
        this.code = code;
        this.name = name;
        this.deactivatedBy = deactivatedBy;
    }
}
