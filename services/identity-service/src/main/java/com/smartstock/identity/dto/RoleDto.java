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
public class RoleDto {
    
    private String id;
    
    private String name;
    
    private String description;
    
    @JsonProperty("hierarchy_level")
    private Integer hierarchyLevel;
    
    private List<PermissionDto> permissions;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
