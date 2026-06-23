package com.smartstock.identity.infrastructure.security;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String username,
        String email,
        List<String> roles,
        List<String> permissions,
        List<String> warehouseIds
) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
