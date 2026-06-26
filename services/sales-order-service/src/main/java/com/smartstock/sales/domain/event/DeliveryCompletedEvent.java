package com.smartstock.sales.domain.event;

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
public class DeliveryCompletedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String shipmentId;
    private LocalDate deliveryDate;
    private String signedBy;
    private Instant deliveredAt;

    public DeliveryCompletedEvent(String soId, String soNumber, String customerId,
                                   String shipmentId, LocalDate deliveryDate,
                                   String signedBy, Instant deliveredAt) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.shipmentId = shipmentId;
        this.deliveryDate = deliveryDate;
        this.signedBy = signedBy;
        this.deliveredAt = deliveredAt;
    }
}
