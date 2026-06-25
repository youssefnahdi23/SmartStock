package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BinCreatedEvent extends DomainEvent {

    private String binId;
    private String shelfId;
    private String zoneId;
    private String warehouseId;
    private String code;
    private String type;
    private int capacityUnits;

    public BinCreatedEvent(String binId, String shelfId, String zoneId, String warehouseId,
                            String code, String type, int capacityUnits) {
        super(binId, "WarehouseBin", "warehouse-service");
        this.binId = binId;
        this.shelfId = shelfId;
        this.zoneId = zoneId;
        this.warehouseId = warehouseId;
        this.code = code;
        this.type = type;
        this.capacityUnits = capacityUnits;
    }
}
