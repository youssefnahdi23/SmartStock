package com.smartstock.identity.presentation.exception;

public class RefreshTokenException extends InvalidTokenException {

    public RefreshTokenException(String message) {
        super(message);
    }
}
