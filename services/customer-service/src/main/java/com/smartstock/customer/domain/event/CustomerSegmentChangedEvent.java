package com.smartstock.customer.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CustomerSegmentChangedEvent extends DomainEvent {

    private String customerCode;
    private String customerName;
    private String previousSegment;
    private String newSegment;
    private String changedBy;

    public CustomerSegmentChangedEvent(String customerId, String customerCode, String customerName,
                                        String previousSegment, String newSegment, String changedBy) {
        super(customerId, "Customer", "CustomerService");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.previousSegment = previousSegment;
        this.newSegment = newSegment;
        this.changedBy = changedBy;
    }
}
