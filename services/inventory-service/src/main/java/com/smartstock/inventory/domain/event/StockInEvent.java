package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockInEvent extends DomainEvent {

    private String transactionId;
    private String productId;
    private String warehouseId;
    private String zoneId;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String referenceType;
    private String referenceId;
    private String supplierId;
    private int previousBalance;
    private int newBalance;
    private String receivedBy;

    public StockInEvent(String transactionId, String productId, String warehouseId,
                        String zoneId, int quantity, BigDecimal unitCost, String referenceType,
                        String referenceId, String supplierId,
                        int previousBalance, int newBalance, String receivedBy) {
        super(transactionId, "StockMovement", "inventory-service");
        this.transactionId = transactionId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.totalCost = unitCost != null ? unitCost.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.supplierId = supplierId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.receivedBy = receivedBy;
    }
}
