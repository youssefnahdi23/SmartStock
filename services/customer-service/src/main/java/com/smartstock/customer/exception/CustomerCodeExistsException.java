package com.smartstock.customer.exception;

import org.springframework.http.HttpStatus;

public class CustomerCodeExistsException extends BusinessException {

    public CustomerCodeExistsException(String code) {
        super("CUSTOMER_CODE_EXISTS", "Customer code already in use: " + code, HttpStatus.BAD_REQUEST);
    }
}
