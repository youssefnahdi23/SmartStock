package com.smartstock.product.exception;

import org.springframework.http.HttpStatus;

public class CategoryNameExistsException extends BusinessException {

    public CategoryNameExistsException(String name) {
        super("CATEGORY_NAME_EXISTS", "Category name already exists: " + name, HttpStatus.BAD_REQUEST);
    }
}
