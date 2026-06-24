package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }
}
