package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String identifier) {
        super("USER_NOT_FOUND", "User not found: " + identifier, HttpStatus.NOT_FOUND);
    }
}
