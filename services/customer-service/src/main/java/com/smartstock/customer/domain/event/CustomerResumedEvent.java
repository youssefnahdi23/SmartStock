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
public class CustomerResumedEvent extends DomainEvent {

    private String customerCode;
    private String customerName;
    private String resumedBy;

    public CustomerResumedEvent(String customerId, String customerCode, String customerName, String resumedBy) {
        super(customerId, "Customer", "CustomerService");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.resumedBy = resumedBy;
    }
}
