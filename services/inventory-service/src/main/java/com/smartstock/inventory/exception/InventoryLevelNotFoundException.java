package com.smartstock.inventory.exception;

import org.springframework.http.HttpStatus;

public class InventoryLevelNotFoundException extends BusinessException {

    public InventoryLevelNotFoundException(String productId, String warehouseId) {
        super("INVENTORY_LEVEL_NOT_FOUND",
                String.format("No inventory record for product %s in warehouse %s", productId, warehouseId),
                HttpStatus.NOT_FOUND);
    }
}
