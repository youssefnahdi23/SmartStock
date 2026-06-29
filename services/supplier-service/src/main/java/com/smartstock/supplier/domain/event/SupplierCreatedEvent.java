package com.smartstock.supplier.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SupplierCreatedEvent extends DomainEvent {

    private String supplierCode;
    private String supplierName;
    private String supplierType;
    private String createdBy;

    public SupplierCreatedEvent(String supplierId, String supplierCode, String supplierName,
                                 String supplierType, String createdBy) {
        super(supplierId, "Supplier", "supplier-service");
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.supplierType = supplierType;
        this.createdBy = createdBy;
    }
}
