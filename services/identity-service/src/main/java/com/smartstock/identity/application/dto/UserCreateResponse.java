package com.smartstock.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserCreateResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean active,
        boolean emailVerified,
        Instant passwordExpiresAt,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        Set<String> warehouseIds,
        List<RoleDto> roles
) {
}
