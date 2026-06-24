package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String field, String value) {
        super("USER_ALREADY_EXISTS",
              "User with " + field + " '" + value + "' already exists",
              HttpStatus.BAD_REQUEST);
    }
}
