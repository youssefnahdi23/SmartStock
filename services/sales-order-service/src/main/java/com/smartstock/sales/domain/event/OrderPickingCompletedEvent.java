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
public class OrderPickingCompletedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String warehouseId;
    private Integer pickedQuantity;
    private Instant completedAt;
    private String pickedBy;

    public OrderPickingCompletedEvent(String soId, String soNumber, String customerId,
                                       String warehouseId, Integer pickedQuantity,
                                       Instant completedAt, String pickedBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.warehouseId = warehouseId;
        this.pickedQuantity = pickedQuantity;
        this.completedAt = completedAt;
        this.pickedBy = pickedBy;
    }
}
