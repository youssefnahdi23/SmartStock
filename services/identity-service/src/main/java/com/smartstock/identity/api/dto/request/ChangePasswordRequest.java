package com.smartstock.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_.#^()\\-])[A-Za-z\\d@$!%*?&_.#^()\\-]{12,}$",
        message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    private String newPassword;
}
