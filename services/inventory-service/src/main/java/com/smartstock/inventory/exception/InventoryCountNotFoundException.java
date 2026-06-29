package com.smartstock.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryCountNotFoundException extends BusinessException {

    public InventoryCountNotFoundException(String countId) {
        super("INVENTORY_COUNT_NOT_FOUND",
                "Inventory count not found: " + countId,
                HttpStatus.NOT_FOUND);
    }
}
