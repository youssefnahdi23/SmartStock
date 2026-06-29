package com.smartstock.product.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(String productId) {
        super("PRODUCT_NOT_FOUND", "Product not found: " + productId, HttpStatus.NOT_FOUND);
    }
}
