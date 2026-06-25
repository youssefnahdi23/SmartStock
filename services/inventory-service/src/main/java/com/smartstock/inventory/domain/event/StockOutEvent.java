package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockOutEvent extends DomainEvent {

    private String transactionId;
    private String productId;
    private String warehouseId;
    private int quantity;
    private String referenceType;
    private String referenceId;
    private String customerId;
    private int previousBalance;
    private int newBalance;
    private String releasedBy;

    public StockOutEvent(String transactionId, String productId, String warehouseId,
                         int quantity, String referenceType, String referenceId,
                         String customerId, int previousBalance, int newBalance, String releasedBy) {
        super(transactionId, "StockMovement", "inventory-service");
        this.transactionId = transactionId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.customerId = customerId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.releasedBy = releasedBy;
    }
}
