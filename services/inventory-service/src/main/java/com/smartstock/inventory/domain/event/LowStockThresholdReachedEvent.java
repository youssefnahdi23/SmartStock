package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LowStockThresholdReachedEvent extends DomainEvent {

    private String productId;
    private String warehouseId;
    private int currentQuantity;
    private int minimumThreshold;
    private int reorderQuantity;
    private int reorderPoint;

    public LowStockThresholdReachedEvent(String productId, String warehouseId,
                                          int currentQuantity, int minimumThreshold,
                                          int reorderQuantity, int reorderPoint) {
        super(productId + "-" + warehouseId, "InventoryLevel", "inventory-service");
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.currentQuantity = currentQuantity;
        this.minimumThreshold = minimumThreshold;
        this.reorderQuantity = reorderQuantity;
        this.reorderPoint = reorderPoint;
    }
}
