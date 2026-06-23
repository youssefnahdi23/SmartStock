package com.smartstock.identity.presentation.exception;

public class AccountLockedException extends UnauthorizedException {

    public AccountLockedException(String message) {
        super(message);
    }
}
