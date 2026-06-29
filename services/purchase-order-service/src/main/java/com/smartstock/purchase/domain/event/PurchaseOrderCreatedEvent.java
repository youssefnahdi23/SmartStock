package com.smartstock.purchase.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PurchaseOrderCreatedEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String supplierName;
    private String deliveryWarehouseId;
    private LocalDate expectedDeliveryDate;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String createdBy;

    public PurchaseOrderCreatedEvent(String poId, String poNumber, String supplierId, String supplierName,
                                      String deliveryWarehouseId, LocalDate expectedDeliveryDate,
                                      BigDecimal totalAmount, Integer totalQuantity, String createdBy) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.deliveryWarehouseId = deliveryWarehouseId;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.createdBy = createdBy;
    }
}
