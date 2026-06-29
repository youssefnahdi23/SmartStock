package com.smartstock.sales.exception;

public class DuplicateSONumberException extends BusinessException {
    public DuplicateSONumberException(String soNumber) {
        super("SO_DUPLICATE_NUMBER", "Sales order number already exists: " + soNumber);
    }
}
