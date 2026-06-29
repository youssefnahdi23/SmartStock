package com.smartstock.supplier.exception;

import org.springframework.http.HttpStatus;

public class SupplierSuspendedException extends BusinessException {

    public SupplierSuspendedException(String supplierId) {
        super("SUPPLIER_SUSPENDED", "Supplier is suspended: " + supplierId, HttpStatus.BAD_REQUEST);
    }
}
