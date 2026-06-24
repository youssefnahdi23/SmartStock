package com.smartstock.identity.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleResponse {
    private String id;
    private String name;
    private String description;
    private boolean systemRole;
    private boolean active;
    private List<String> permissions;
    private LocalDateTime createdAt;
}
