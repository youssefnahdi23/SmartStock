package com.smartstock.identity.exception;

public class PasswordExpiredException extends RuntimeException {
    public PasswordExpiredException(String message) {
        super(message);
    }
    
    public PasswordExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
