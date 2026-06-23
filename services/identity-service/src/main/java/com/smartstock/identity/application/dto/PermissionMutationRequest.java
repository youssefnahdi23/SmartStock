package com.smartstock.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionMutationRequest(
        @NotBlank(message = "Permission key is required")
        String permissionKey,
        @NotBlank(message = "Description is required")
        String description,
        @NotBlank(message = "Resource is required")
        String resource,
        @NotBlank(message = "Action is required")
        String action,
        @NotBlank(message = "Scope is required")
        String scope,
        boolean active
) {
}
