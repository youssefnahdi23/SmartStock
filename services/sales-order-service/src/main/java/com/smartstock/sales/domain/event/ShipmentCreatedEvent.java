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
public class ShipmentCreatedEvent extends DomainEvent {

    private String soNumber;
    private String customerId;
    private String shipmentId;
    private String shipmentNumber;
    private String carrierName;
    private String trackingNumber;
    private LocalDate estimatedDeliveryDate;
    private Integer shippedQuantity;
    private Instant shippedAt;
    private String shippedBy;

    public ShipmentCreatedEvent(String soId, String soNumber, String customerId,
                                 String shipmentId, String shipmentNumber,
                                 String carrierName, String trackingNumber,
                                 LocalDate estimatedDeliveryDate, Integer shippedQuantity,
                                 Instant shippedAt, String shippedBy) {
        super(soId, "SalesOrder", "sales-order-service");
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.shipmentId = shipmentId;
        this.shipmentNumber = shipmentNumber;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.shippedQuantity = shippedQuantity;
        this.shippedAt = shippedAt;
        this.shippedBy = shippedBy;
    }
}
