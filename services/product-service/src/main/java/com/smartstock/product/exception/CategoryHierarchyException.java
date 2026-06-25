package com.smartstock.product.exception;

import org.springframework.http.HttpStatus;

public class CategoryHierarchyException extends BusinessException {

    public CategoryHierarchyException() {
        super("CATEGORY_HIERARCHY_EXCEEDED", "Category hierarchy is limited to 5 levels", HttpStatus.BAD_REQUEST);
    }
}
