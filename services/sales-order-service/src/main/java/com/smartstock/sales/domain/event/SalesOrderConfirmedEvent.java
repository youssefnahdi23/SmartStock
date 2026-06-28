package com.smartstock.sales.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

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
    /** Per-product reservation detail; inventory-service uses this to reserve stock (v2). */
    private List<LineItem> items;

    public SalesOrderConfirmedEvent(String soId, String soNumber, String customerId,
                                     String pickingWarehouseId, Instant confirmationDate,
                                     String confirmedBy, List<LineItem> items) {
        super(soId, "SalesOrder", "sales-order-service");
        setEventVersion(2);
        this.soNumber = soNumber;
        this.customerId = customerId;
        this.pickingWarehouseId = pickingWarehouseId;
        this.confirmationDate = confirmationDate;
        this.confirmedBy = confirmedBy;
        this.items = items;
    }

    /** Line item to reserve in inventory on order confirmation. */
    public record LineItem(String productId, int quantity) {}
}
