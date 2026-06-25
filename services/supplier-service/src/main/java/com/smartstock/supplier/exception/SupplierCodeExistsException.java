package com.smartstock.supplier.exception;

import org.springframework.http.HttpStatus;

public class SupplierCodeExistsException extends BusinessException {

    public SupplierCodeExistsException(String code) {
        super("SUPPLIER_CODE_EXISTS", "Supplier code already in use: " + code, HttpStatus.BAD_REQUEST);
    }
}
