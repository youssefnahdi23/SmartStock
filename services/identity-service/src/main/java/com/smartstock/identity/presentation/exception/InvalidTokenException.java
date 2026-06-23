package com.smartstock.identity.presentation.exception;

public class InvalidTokenException extends UnauthorizedException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
