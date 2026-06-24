package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String tokenType) {
        super("INVALID_" + tokenType.toUpperCase() + "_TOKEN",
              tokenType + " token is invalid or expired",
              HttpStatus.UNAUTHORIZED);
    }
}
