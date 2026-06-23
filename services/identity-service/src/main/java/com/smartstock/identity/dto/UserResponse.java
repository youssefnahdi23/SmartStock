package com.smartstock.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private String id;
    
    private String username;
    
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("is_locked")
    private Boolean isLocked;
    
    @JsonProperty("last_login")
    private LocalDateTime lastLogin;
    
    @JsonProperty("password_expires_at")
    private LocalDateTime passwordExpiresAt;
    
    private List<RoleDto> roles;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
