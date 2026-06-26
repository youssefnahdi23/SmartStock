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
public class PurchaseOrderConfirmedEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String deliveryWarehouseId;
    private LocalDate expectedDeliveryDate;
    private String confirmationNumber;
    private Instant confirmationDate;
    private String confirmedBy;

    public PurchaseOrderConfirmedEvent(String poId, String poNumber, String supplierId,
                                        String deliveryWarehouseId, LocalDate expectedDeliveryDate,
                                        String confirmationNumber, Instant confirmationDate, String confirmedBy) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.deliveryWarehouseId = deliveryWarehouseId;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.confirmationNumber = confirmationNumber;
        this.confirmationDate = confirmationDate;
        this.confirmedBy = confirmedBy;
    }
}
