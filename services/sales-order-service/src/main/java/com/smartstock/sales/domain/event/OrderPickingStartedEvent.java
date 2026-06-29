package com.smartstock.sales.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class OrderPickingStartedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String warehouseId;
    private String pickedBy;

    public OrderPickingStartedEvent(String soId, String soNumber, String customerId,
                                     String warehouseId, String pickedBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.warehouseId = warehouseId;
        this.pickedBy = pickedBy;
    }
}
