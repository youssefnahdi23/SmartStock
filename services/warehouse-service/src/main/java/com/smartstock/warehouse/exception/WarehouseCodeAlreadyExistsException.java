package com.smartstock.warehouse.exception;

import org.springframework.http.HttpStatus;

public class WarehouseCodeAlreadyExistsException extends BusinessException {
    public WarehouseCodeAlreadyExistsException(String code) {
        super("Warehouse code already exists: " + code, HttpStatus.CONFLICT, "WAREHOUSE_CODE_EXISTS");
    }
}
