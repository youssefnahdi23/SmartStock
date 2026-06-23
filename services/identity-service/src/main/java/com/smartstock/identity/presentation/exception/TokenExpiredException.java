package com.smartstock.identity.presentation.exception;

public class TokenExpiredException extends InvalidTokenException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
