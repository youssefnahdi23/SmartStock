package com.smartstock.identity.application.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record AssignRolesRequest(
        Set<UUID> roleIds,
        @NotEmpty(message = "At least one warehouse identifier is required")
        Set<String> warehouseIds
) {
}
