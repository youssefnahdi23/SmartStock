package com.smartstock.product.exception;

import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(String categoryId) {
        super("CATEGORY_NOT_FOUND", "Category not found: " + categoryId, HttpStatus.NOT_FOUND);
    }
}
