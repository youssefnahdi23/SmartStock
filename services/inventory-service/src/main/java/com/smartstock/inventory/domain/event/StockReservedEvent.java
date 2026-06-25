package com.smartstock.inventory.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StockReservedEvent extends DomainEvent {

    private String reservationId;
    private String productId;
    private String warehouseId;
    private int quantity;
    private String orderId;
    private String reservationReason;
    private Instant expiryDate;
    private String reservedBy;
    private int availableAfterReservation;

    public StockReservedEvent(String reservationId, String productId, String warehouseId,
                               int quantity, String orderId, String reservationReason,
                               Instant expiryDate, String reservedBy, int availableAfterReservation) {
        super(reservationId, "InventoryHold", "inventory-service");
        this.reservationId = reservationId;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.orderId = orderId;
        this.reservationReason = reservationReason;
        this.expiryDate = expiryDate;
        this.reservedBy = reservedBy;
        this.availableAfterReservation = availableAfterReservation;
    }
}
