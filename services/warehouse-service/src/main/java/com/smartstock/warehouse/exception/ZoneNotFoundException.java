package com.smartstock.warehouse.exception;

import org.springframework.http.HttpStatus;

public class ZoneNotFoundException extends BusinessException {
    public ZoneNotFoundException(String id) {
        super("Zone not found: " + id, HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND");
    }
}
