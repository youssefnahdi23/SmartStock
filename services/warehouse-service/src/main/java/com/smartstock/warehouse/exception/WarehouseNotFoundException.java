package com.smartstock.warehouse.exception;

import org.springframework.http.HttpStatus;

public class WarehouseNotFoundException extends BusinessException {
    public WarehouseNotFoundException(String id) {
        super("Warehouse not found: " + id, HttpStatus.NOT_FOUND, "WAREHOUSE_NOT_FOUND");
    }
}
