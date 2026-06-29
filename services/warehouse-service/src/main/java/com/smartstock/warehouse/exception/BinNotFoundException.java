package com.smartstock.warehouse.exception;

import org.springframework.http.HttpStatus;

public class BinNotFoundException extends BusinessException {
    public BinNotFoundException(String id) {
        super("Bin not found: " + id, HttpStatus.NOT_FOUND, "BIN_NOT_FOUND");
    }
}
