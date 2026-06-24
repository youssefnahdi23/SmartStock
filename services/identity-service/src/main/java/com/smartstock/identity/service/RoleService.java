package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.CreateRoleRequest;
import com.smartstock.identity.api.dto.response.RoleResponse;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.repository.PermissionRepository;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.exception.BusinessException;
import com.smartstock.identity.exception.RoleNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public Page<RoleResponse> findAll(Pageable pageable) {
        return roleRepository.findAllByActiveTrue(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RoleResponse findById(String id) {
        Role role = roleRepository.findByIdAndActive(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return toResponse(role);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("ROLE_ALREADY_EXISTS",
                    "Role with name '" + request.getName() + "' already exists",
                    HttpStatus.BAD_REQUEST);
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .systemRole(false)
                .active(true)
                .build();

        if (request.getPermissionNames() != null && !request.getPermissionNames().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permName : request.getPermissionNames()) {
                permissionRepository.findByNameAndActive(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);
        log.info("Role created: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Role findEntityById(String id) {
        return roleRepository.findByIdAndActive(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }

    private RoleResponse toResponse(Role role) {
        List<String> perms = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(role.isSystemRole())
                .active(role.isActive())
                .permissions(perms)
                .createdAt(role.getCreatedAt())
                .build();
    }
}
