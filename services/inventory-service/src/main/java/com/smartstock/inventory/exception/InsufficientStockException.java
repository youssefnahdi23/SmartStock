package com.smartstock.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productId, String warehouseId, int requested, int available) {
        super("INSUFFICIENT_STOCK",
                String.format("Insufficient stock for product %s in warehouse %s: requested %d, available %d",
                        productId, warehouseId, requested, available),
                HttpStatus.BAD_REQUEST);
    }
}
