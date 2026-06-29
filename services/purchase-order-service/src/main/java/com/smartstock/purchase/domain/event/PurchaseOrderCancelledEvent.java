package com.smartstock.purchase.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PurchaseOrderCancelledEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String reason;
    private Instant cancelledAt;
    private String cancelledBy;

    public PurchaseOrderCancelledEvent(String poId, String poNumber, String supplierId,
                                        String reason, Instant cancelledAt, String cancelledBy) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }
}
