package com.smartstock.purchase.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DeliveryRegisteredEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String deliveryId;
    private String deliveryWarehouseId;
    private LocalDate deliveryDate;
    private Integer totalReceivedQuantity;
    private Integer damageCount;
    private String newPoStatus;
    private Instant receivedAt;
    private String receivedBy;

    public DeliveryRegisteredEvent(String poId, String poNumber, String supplierId,
                                    String deliveryId, String deliveryWarehouseId,
                                    LocalDate deliveryDate, Integer totalReceivedQuantity,
                                    Integer damageCount, String newPoStatus,
                                    Instant receivedAt, String receivedBy) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.deliveryId = deliveryId;
        this.deliveryWarehouseId = deliveryWarehouseId;
        this.deliveryDate = deliveryDate;
        this.totalReceivedQuantity = totalReceivedQuantity;
        this.damageCount = damageCount;
        this.newPoStatus = newPoStatus;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
    }
}
