package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockAdjustedEvent extends DomainEvent {

    private String adjustmentId;
    private String productId;
    private String warehouseId;
    private int quantityBefore;
    private int quantityAfter;
    private int adjustment;
    private String reason;
    private String adjustedBy;
    private String notes;

    public StockAdjustedEvent(String adjustmentId, String productId, String warehouseId,
                               int quantityBefore, int quantityAfter,
                               String reason, String adjustedBy, String notes) {
        super(adjustmentId, "StockAdjustment", "inventory-service");
        this.adjustmentId = adjustmentId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.adjustment = quantityAfter - quantityBefore;
        this.reason = reason;
        this.adjustedBy = adjustedBy;
        this.notes = notes;
    }
}
