package com.smartstock.identity.presentation.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
