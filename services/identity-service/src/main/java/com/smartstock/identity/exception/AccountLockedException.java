package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BusinessException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED",
              "Account is locked due to too many failed login attempts. Try again later.",
              HttpStatus.UNAUTHORIZED);
    }
}
