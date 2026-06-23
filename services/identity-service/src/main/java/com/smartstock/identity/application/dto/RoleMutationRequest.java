package com.smartstock.identity.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record RoleMutationRequest(
        @NotBlank(message = "Role name is required")
        String name,
        @NotBlank(message = "Role description is required")
        String description,
        @Min(value = 0, message = "Hierarchy rank must be zero or greater")
        int hierarchyRank,
        boolean active,
        @NotEmpty(message = "At least one permission must be assigned")
        Set<UUID> permissionIds
) {
}
