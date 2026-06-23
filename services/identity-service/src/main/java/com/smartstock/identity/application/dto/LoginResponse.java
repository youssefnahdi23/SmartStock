package com.smartstock.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean passwordChangeRequired,
        Instant passwordExpiresAt,
        List<RoleDto> roles,
        List<PermissionDto> permissions,
        TokenDto token
) {
}
