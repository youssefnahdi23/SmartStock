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
public class CustomerCreatedEvent extends DomainEvent {

    private String customerCode;
    private String customerName;
    private String customerType;
    private String segment;
    private String emailAddress;
    private String phoneNumber;
    private AddressData address;
    private String createdBy;

    public CustomerCreatedEvent(String customerId, String customerCode, String customerName,
                                String customerType, String segment, String emailAddress,
                                String phoneNumber, AddressData address, String createdBy) {
        super(customerId, "Customer", "CustomerService");
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.customerType = customerType;
        this.segment = segment;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.createdBy = createdBy;
    }

    public record AddressData(String street, String city, String state, String postalCode, String country) {}
}
