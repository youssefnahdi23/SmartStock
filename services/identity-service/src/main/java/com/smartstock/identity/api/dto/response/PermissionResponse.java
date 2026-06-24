package com.smartstock.identity.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PermissionResponse {
    private String id;
    private String name;
    private String description;
    private String resource;
    private String action;
    private boolean active;
    private LocalDateTime createdAt;
}
