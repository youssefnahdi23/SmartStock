package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ShelfCreatedEvent extends DomainEvent {

    private String shelfId;
    private String zoneId;
    private String warehouseId;
    private String code;
    private String name;
    private String createdBy;

    public ShelfCreatedEvent(String shelfId, String zoneId, String warehouseId,
                              String code, String name, String createdBy) {
        super(shelfId, "WarehouseShelf", "warehouse-service");
        this.shelfId = shelfId;
        this.zoneId = zoneId;
        this.warehouseId = warehouseId;
        this.code = code;
        this.name = name;
        this.createdBy = createdBy;
    }
}
