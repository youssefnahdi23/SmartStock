package com.smartstock.identity.presentation.exception;

public class RateLimitExceededException extends IdentityException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
