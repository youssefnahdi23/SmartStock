package com.smartstock.supplier.exception;

import org.springframework.http.HttpStatus;

public class SupplierNotFoundException extends BusinessException {

    public SupplierNotFoundException(String supplierId) {
        super("SUPPLIER_NOT_FOUND", "Supplier not found: " + supplierId, HttpStatus.NOT_FOUND);
    }
}
