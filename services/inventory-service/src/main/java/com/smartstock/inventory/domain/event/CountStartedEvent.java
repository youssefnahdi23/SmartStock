package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CountStartedEvent extends DomainEvent {

    private String countId;
    private String warehouseId;
    private String countType;
    private String name;
    private String startedBy;

    public CountStartedEvent(String countId, String warehouseId,
                              String countType, String name, String startedBy) {
        super(countId, "InventoryCount", "inventory-service");
        this.countId = countId;
        this.warehouseId = warehouseId;
        this.countType = countType;
        this.name = name;
        this.startedBy = startedBy;
    }
}
