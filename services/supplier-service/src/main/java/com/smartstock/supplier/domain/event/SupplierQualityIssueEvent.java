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
public class SupplierQualityIssueEvent extends DomainEvent {

    private String supplierCode;
    private String deliveryId;
    private String deliveryNumber;
    private Integer qualityIssuesFound;
    private Integer rejectedQuantity;
    private String reportedBy;

    public SupplierQualityIssueEvent(String supplierId, String supplierCode,
                                      String deliveryId, String deliveryNumber,
                                      Integer qualityIssuesFound, Integer rejectedQuantity,
                                      String reportedBy) {
        super(supplierId, "Supplier", "supplier-service");
        this.supplierCode = supplierCode;
        this.deliveryId = deliveryId;
        this.deliveryNumber = deliveryNumber;
        this.qualityIssuesFound = qualityIssuesFound;
        this.rejectedQuantity = rejectedQuantity;
        this.reportedBy = reportedBy;
    }
}
