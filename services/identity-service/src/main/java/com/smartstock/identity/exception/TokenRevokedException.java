package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class TokenRevokedException extends BusinessException {

    public TokenRevokedException() {
        super("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked", HttpStatus.FORBIDDEN);
    }
}
