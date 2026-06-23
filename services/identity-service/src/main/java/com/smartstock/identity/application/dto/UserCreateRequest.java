package com.smartstock.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record UserCreateRequest(
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        @NotBlank(message = "Password is required")
        String password,
        Set<UUID> roleIds,
        Set<String> roleNames,
        @NotEmpty(message = "At least one assigned warehouse identifier is required")
        Set<String> warehouseIds
) {
}
