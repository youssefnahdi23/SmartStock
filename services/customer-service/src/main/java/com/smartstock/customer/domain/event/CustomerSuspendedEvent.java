package com.smartstock.customer.domain.event;

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
public class CustomerSuspendedEvent extends DomainEvent {

    private String customerCode;
    private String customerName;
    private String reason;
    private LocalDate resumeDate;
    private String suspendedBy;

    public CustomerSuspendedEvent(String customerId, String customerCode, String customerName,
                                   String reason, LocalDate resumeDate, String suspendedBy) {
        super(customerId, "Customer", "CustomerService");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.reason = reason;
        this.resumeDate = resumeDate;
        this.suspendedBy = suspendedBy;
    }
}
