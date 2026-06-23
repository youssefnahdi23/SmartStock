package com.smartstock.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionDto {
    
    private String id;
    
    private String resource;
    
    private String action;
    
    private String scope;
    
    private String description;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
