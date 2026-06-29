package com.smartstock.warehouse.exception;

import org.springframework.http.HttpStatus;

public class ShelfNotFoundException extends BusinessException {
    public ShelfNotFoundException(String id) {
        super("Shelf not found: " + id, HttpStatus.NOT_FOUND, "SHELF_NOT_FOUND");
    }
}
