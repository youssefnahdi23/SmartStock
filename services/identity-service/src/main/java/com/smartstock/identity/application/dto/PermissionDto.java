package com.smartstock.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PermissionDto(
        UUID id,
        String permissionKey,
        String description,
        String resource,
        String action,
        String scope,
        boolean active,
        boolean systemManaged,
        Instant createdAt,
        Instant updatedAt
) {
}
