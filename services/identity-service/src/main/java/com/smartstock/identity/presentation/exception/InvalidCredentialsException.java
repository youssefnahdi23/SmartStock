package com.smartstock.identity.presentation.exception;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
