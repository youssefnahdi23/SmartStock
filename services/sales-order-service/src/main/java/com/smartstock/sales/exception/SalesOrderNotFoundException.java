package com.smartstock.sales.exception;

public class SalesOrderNotFoundException extends BusinessException {
    public SalesOrderNotFoundException(String soId) {
        super("SO_NOT_FOUND", "Sales order not found: " + soId);
    }
}
