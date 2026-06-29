package com.smartstock.customer.exception;

import org.springframework.http.HttpStatus;

public class CustomerSuspendedException extends BusinessException {

    public CustomerSuspendedException(String customerId) {
        super("CUSTOMER_SUSPENDED", "Customer is suspended: " + customerId, HttpStatus.BAD_REQUEST);
    }
}
