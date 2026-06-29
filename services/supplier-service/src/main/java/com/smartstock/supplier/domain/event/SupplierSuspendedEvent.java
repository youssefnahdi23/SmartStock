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
public class SupplierSuspendedEvent extends DomainEvent {

    private String supplierCode;
    private String supplierName;
    private String reason;
    private LocalDate resumeDate;
    private String suspendedBy;

    public SupplierSuspendedEvent(String supplierId, String supplierCode, String supplierName,
                                   String reason, LocalDate resumeDate, String suspendedBy) {
        super(supplierId, "Supplier", "supplier-service");
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.reason = reason;
        this.resumeDate = resumeDate;
        this.suspendedBy = suspendedBy;
    }
}
