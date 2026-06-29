package com.smartstock.warehouse.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WarehouseUpdatedEvent extends DomainEvent {

    private String warehouseId;
    private String updatedBy;
    private Map<String, Object> changes;
    private Map<String, Object> previousValues;

    public WarehouseUpdatedEvent(String warehouseId, String updatedBy,
                                  Map<String, Object> changes, Map<String, Object> previousValues) {
        super(warehouseId, "Warehouse", "warehouse-service");
        this.warehouseId = warehouseId;
        this.updatedBy = updatedBy;
        this.changes = changes;
        this.previousValues = previousValues;
    }
}
