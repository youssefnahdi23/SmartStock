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
public class SupplierResumedEvent extends DomainEvent {

    private String supplierCode;
    private String supplierName;
    private String resumedBy;

    public SupplierResumedEvent(String supplierId, String supplierCode, String supplierName, String resumedBy) {
        super(supplierId, "Supplier", "supplier-service");
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.resumedBy = resumedBy;
    }
}
