package com.smartstock.sales.domain.event;

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
public class SalesOrderConfirmedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String pickingWarehouseId;
    private Instant confirmationDate;
    private String confirmedBy;

    public SalesOrderConfirmedEvent(String soId, String soNumber, String customerId,
                                     String pickingWarehouseId, Instant confirmationDate, String confirmedBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.pickingWarehouseId = pickingWarehouseId;
        this.confirmationDate = confirmationDate;
        this.confirmedBy = confirmedBy;
    }
}
