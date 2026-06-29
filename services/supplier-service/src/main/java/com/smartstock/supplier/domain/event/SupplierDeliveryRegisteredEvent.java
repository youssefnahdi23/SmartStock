package com.smartstock.supplier.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SupplierDeliveryRegisteredEvent extends DomainEvent {

    private String deliveryNumber;
    private String purchaseOrderId;
    private String supplierCode;
    private LocalDate orderDate;
    private LocalDate promisedDeliveryDate;
    private Integer quantityOrdered;
    private String registeredBy;

    public SupplierDeliveryRegisteredEvent(String deliveryId, String supplierId, String deliveryNumber,
                                            String purchaseOrderId, String supplierCode,
                                            LocalDate orderDate, LocalDate promisedDeliveryDate,
                                            Integer quantityOrdered, String registeredBy) {
        super(supplierId, "Supplier", "supplier-service");
        this.deliveryNumber = deliveryNumber;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierCode = supplierCode;
        this.orderDate = orderDate;
        this.promisedDeliveryDate = promisedDeliveryDate;
        this.quantityOrdered = quantityOrdered;
        this.registeredBy = registeredBy;
    }
}
