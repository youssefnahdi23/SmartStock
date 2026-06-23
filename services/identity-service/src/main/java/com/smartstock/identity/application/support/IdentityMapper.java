package com.smartstock.identity.application.support;

import com.smartstock.identity.application.dto.PagedPermissionResponse;
import com.smartstock.identity.application.dto.PagedRoleResponse;
import com.smartstock.identity.application.dto.PermissionDto;
import com.smartstock.identity.application.dto.RoleDto;
import com.smartstock.identity.application.dto.UserCreateResponse;
import com.smartstock.identity.application.dto.UserPageResponse;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.model.User;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class IdentityMapper {

    public PermissionDto toPermissionDto(Permission permission) {
        return new PermissionDto(
                permission.getId(),
                permission.getPermissionKey(),
                permission.getDescription(),
                permission.getResource(),
                permission.getAction(),
                permission.getScope(),
                permission.isActive(),
                permission.isSystemManaged(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }

    public RoleDto toRoleDto(Role role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getHierarchyRank(),
                role.isActive(),
                role.isSystemManaged(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getPermissionKey))
                        .map(this::toPermissionDto)
                        .toList()
        );
    }

    public UserCreateResponse toUserResponse(User user) {
        return new UserCreateResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                user.isEmailVerified(),
                user.getPasswordExpiresAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getWarehouseIds().stream().sorted().collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)),
                user.getRoles().stream()
                        .sorted(Comparator.comparing(Role::getHierarchyRank).thenComparing(Role::getName))
                        .map(this::toRoleDto)
                        .toList()
        );
    }

    public UserPageResponse toUserPage(Page<User> page) {
        return new UserPageResponse(page.getContent().stream().map(this::toUserResponse).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    public PagedRoleResponse toRolePage(Page<Role> page) {
        return new PagedRoleResponse(page.getContent().stream().map(this::toRoleDto).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    public PagedPermissionResponse toPermissionPage(Page<Permission> page) {
        return new PagedPermissionResponse(page.getContent().stream().map(this::toPermissionDto).toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    public List<PermissionDto> toPermissionDtos(User user) {
        return user.getRoles().stream()
                .map(Role::getPermissions)
                .flatMap(java.util.Set::stream)
                .distinct()
                .sorted(Comparator.comparing(Permission::getPermissionKey))
                .map(this::toPermissionDto)
                .toList();
    }

    public List<RoleDto> toRoleDtos(User user) {
        return user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getHierarchyRank).thenComparing(Role::getName))
                .map(this::toRoleDto)
                .toList();
    }
}
