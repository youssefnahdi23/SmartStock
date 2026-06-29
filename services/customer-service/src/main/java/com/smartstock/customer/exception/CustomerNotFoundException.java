package com.smartstock.customer.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends BusinessException {

    public CustomerNotFoundException(String customerId) {
        super("CUSTOMER_NOT_FOUND", "Customer not found: " + customerId, HttpStatus.NOT_FOUND);
    }
}
