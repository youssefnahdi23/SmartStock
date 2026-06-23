package com.smartstock.identity.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        int hierarchyRank,
        boolean active,
        boolean systemManaged,
        Instant createdAt,
        Instant updatedAt,
        List<PermissionDto> permissions
) {
}
