package com.smartstock.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 255, message = "Role name must not exceed 255 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Role name must be UPPERCASE with underscores only")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private List<String> permissionNames;
}
