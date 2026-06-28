package com.smartstock.purchase.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DeliveryRegisteredEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String deliveryId;
    private String deliveryWarehouseId;
    private LocalDate deliveryDate;
    private Integer totalReceivedQuantity;
    private Integer damageCount;
    private String newPoStatus;
    private Instant receivedAt;
    private String receivedBy;
    /** Per-product receipt detail; consumers use this for stock-in per SKU (v2). */
    private List<ReceivedItem> receivedItems;

    public DeliveryRegisteredEvent(String poId, String poNumber, String supplierId,
                                    String deliveryId, String deliveryWarehouseId,
                                    LocalDate deliveryDate, Integer totalReceivedQuantity,
                                    Integer damageCount, String newPoStatus,
                                    Instant receivedAt, String receivedBy,
                                    List<ReceivedItem> receivedItems) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        setEventVersion(2);
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.deliveryId = deliveryId;
        this.deliveryWarehouseId = deliveryWarehouseId;
        this.deliveryDate = deliveryDate;
        this.totalReceivedQuantity = totalReceivedQuantity;
        this.damageCount = damageCount;
        this.newPoStatus = newPoStatus;
        this.receivedAt = receivedAt;
        this.receivedBy = receivedBy;
        this.receivedItems = receivedItems;
    }

    /** Immutable line-item receipt detail carried in the event for downstream consumers. */
    public record ReceivedItem(String productId, int receivedQuantity, int damageCount, BigDecimal unitCost) {}
}
