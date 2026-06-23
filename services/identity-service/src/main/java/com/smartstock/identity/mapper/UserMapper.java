package com.smartstock.identity.mapper;

import com.smartstock.identity.dto.PermissionDto;
import com.smartstock.identity.dto.RoleDto;
import com.smartstock.identity.dto.UserResponse;
import com.smartstock.identity.entity.Permission;
import com.smartstock.identity.entity.Role;
import com.smartstock.identity.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {
    
    public UserResponse toDto(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponse.builder()
            .id(user.getId().toString())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .isActive(user.getIsActive())
            .isLocked(user.getIsLocked())
            .lastLogin(user.getLastLogin())
            .passwordExpiresAt(user.getPasswordExpiresAt())
            .roles(user.getRoles().stream()
                .map(this::roleToDtoLight)
                .collect(Collectors.toList()))
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
    
    private RoleDto roleToDtoLight(Role role) {
        if (role == null) {
            return null;
        }
        
        return RoleDto.builder()
            .id(role.getId().toString())
            .name(role.getName())
            .description(role.getDescription())
            .hierarchyLevel(role.getHierarchyLevel())
            .permissions(role.getPermissions().stream()
                .map(this::permissionToDto)
                .collect(Collectors.toList()))
            .createdAt(role.getCreatedAt())
            .build();
    }
    
    private PermissionDto permissionToDto(Permission permission) {
        if (permission == null) {
            return null;
        }
        
        return PermissionDto.builder()
            .id(permission.getId().toString())
            .resource(permission.getResource())
            .action(permission.getAction())
            .scope(permission.getScope())
            .description(permission.getDescription())
            .createdAt(permission.getCreatedAt())
            .build();
    }
}
