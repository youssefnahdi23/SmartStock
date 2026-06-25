package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CountCompletedEvent extends DomainEvent {

    private String countId;
    private String warehouseId;
    private int totalItemsCounted;
    private int totalVariances;
    private double varianceRate;
    private int adjustmentsCreated;
    private String completedBy;

    public CountCompletedEvent(String countId, String warehouseId,
                                int totalItemsCounted, int totalVariances,
                                double varianceRate, int adjustmentsCreated, String completedBy) {
        super(countId, "InventoryCount", "inventory-service");
        this.countId = countId;
        this.warehouseId = warehouseId;
        this.totalItemsCounted = totalItemsCounted;
        this.totalVariances = totalVariances;
        this.varianceRate = varianceRate;
        this.adjustmentsCreated = adjustmentsCreated;
        this.completedBy = completedBy;
    }
}
