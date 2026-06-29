package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ZoneCreatedEvent extends DomainEvent {

    private String zoneId;
    private String warehouseId;
    private String code;
    private String name;
    private String type;
    private String createdBy;

    public ZoneCreatedEvent(String zoneId, String warehouseId, String code,
                             String name, String type, String createdBy) {
        super(zoneId, "WarehouseZone", "warehouse-service");
        this.zoneId = zoneId;
        this.warehouseId = warehouseId;
        this.code = code;
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
    }
}
