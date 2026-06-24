package com.smartstock.identity.exception;

import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends BusinessException {

    public RoleNotFoundException(String identifier) {
        super("ROLE_NOT_FOUND", "Role not found: " + identifier, HttpStatus.NOT_FOUND);
    }
}
