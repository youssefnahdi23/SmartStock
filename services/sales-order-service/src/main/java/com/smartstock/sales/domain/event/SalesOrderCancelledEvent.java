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
public class SalesOrderCancelledEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String reason;
    private Instant cancelledAt;
    private String cancelledBy;

    public SalesOrderCancelledEvent(String soId, String soNumber, String customerId,
                                     String reason, Instant cancelledAt, String cancelledBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.reason = reason;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
    }
}
