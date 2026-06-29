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
public class CustomerUpdatedEvent extends DomainEvent {

    private String customerCode;
    private String customerName;
    private String updatedBy;

    public CustomerUpdatedEvent(String customerId, String customerCode, String customerName, String updatedBy) {
        super(customerId, "Customer", "CustomerService");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.updatedBy = updatedBy;
    }
}
