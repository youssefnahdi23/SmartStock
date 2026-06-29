package com.smartstock.purchase.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class QualityIssueReportedEvent extends DomainEvent {

    private String poNumber;
    private String supplierId;
    private String issueId;
    private String lineItemId;
    private String issueType;
    private Integer quantity;
    private String severity;
    private String proposedResolution;
    private String reportedBy;

    public QualityIssueReportedEvent(String poId, String poNumber, String supplierId,
                                      String issueId, String lineItemId,
                                      String issueType, Integer quantity, String severity,
                                      String proposedResolution, String reportedBy) {
        super(poId, "PurchaseOrder", "purchase-order-service");
        this.poNumber = poNumber;
        this.supplierId = supplierId;
        this.issueId = issueId;
        this.lineItemId = lineItemId;
        this.issueType = issueType;
        this.quantity = quantity;
        this.severity = severity;
        this.proposedResolution = proposedResolution;
        this.reportedBy = reportedBy;
    }
}
