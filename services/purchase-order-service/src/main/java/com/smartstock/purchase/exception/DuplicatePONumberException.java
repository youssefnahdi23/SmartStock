package com.smartstock.purchase.exception;

import org.springframework.http.HttpStatus;

public class DuplicatePONumberException extends BusinessException {

    public DuplicatePONumberException(String poNumber) {
        super("DUPLICATE_PO_NUMBER",
                "A purchase order with number '" + poNumber + "' already exists",
                HttpStatus.CONFLICT);
    }
}
