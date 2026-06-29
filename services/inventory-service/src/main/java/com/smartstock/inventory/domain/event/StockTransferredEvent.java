package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockTransferredEvent extends DomainEvent {

    private String transferId;
    private String productId;
    private String fromWarehouseId;
    private String toWarehouseId;
    private int quantity;
    private int fromStockBefore;
    private int fromStockAfter;
    private int toStockBefore;
    private int toStockAfter;
    private String reason;
    private String movedBy;

    public StockTransferredEvent(String transferId, String productId,
                                  String fromWarehouseId, String toWarehouseId,
                                  int quantity, int fromStockBefore, int fromStockAfter,
                                  int toStockBefore, int toStockAfter,
                                  String reason, String movedBy) {
        super(transferId, "StockTransfer", "inventory-service");
        this.transferId = transferId;
        this.productId = productId;
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        this.quantity = quantity;
        this.fromStockBefore = fromStockBefore;
        this.fromStockAfter = fromStockAfter;
        this.toStockBefore = toStockBefore;
        this.toStockAfter = toStockAfter;
        this.reason = reason;
        this.movedBy = movedBy;
    }
}
