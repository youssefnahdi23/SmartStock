package com.smartstock.product.exception;

import org.springframework.http.HttpStatus;

public class SkuAlreadyExistsException extends BusinessException {

    public SkuAlreadyExistsException(String sku) {
        super("SKU_ALREADY_EXISTS", "SKU already in use: " + sku, HttpStatus.BAD_REQUEST);
    }
}
