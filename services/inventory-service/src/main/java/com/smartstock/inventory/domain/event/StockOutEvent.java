package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockOutEvent extends DomainEvent {

    private String transactionId;
    private String productId;
    private String warehouseId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String destinationType;
    private String referenceType;
    private String referenceId;
    private String customerId;
    private int previousBalance;
    private int newBalance;
    private String releasedBy;

    public StockOutEvent(String transactionId, String productId, String warehouseId,
                         int quantity, BigDecimal unitPrice, BigDecimal totalPrice,
                         String destinationType, String referenceType, String referenceId,
                         String customerId, int previousBalance, int newBalance, String releasedBy) {
        super(transactionId, "StockMovement", "inventory-service");
        this.transactionId = transactionId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.destinationType = destinationType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.customerId = customerId;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.releasedBy = releasedBy;
    }
}
