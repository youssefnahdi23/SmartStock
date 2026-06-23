package com.smartstock.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;

public record UserUpdateRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        boolean active,
        Set<UUID> roleIds,
        Set<String> roleNames,
        Set<String> warehouseIds
) {
}
