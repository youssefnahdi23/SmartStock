package com.smartstock.identity.service.impl;

import com.smartstock.identity.dto.RoleDto;
import com.smartstock.identity.entity.Role;
import com.smartstock.identity.exception.UserNotFoundException;
import com.smartstock.identity.mapper.UserMapper;
import com.smartstock.identity.repository.RoleRepository;
import com.smartstock.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    
    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
            .filter(r -> r.getDeletedAt() == null)
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public RoleDto getRoleById(String roleId) {
        Role role = roleRepository.findById(java.util.UUID.fromString(roleId))
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new UserNotFoundException("Role not found: " + roleId));
        return convertToDto(role);
    }
    
    @Override
    public RoleDto getRoleByName(String name) {
        Role role = roleRepository.findByNameAndDeletedAtIsNull(name)
            .orElseThrow(() -> new UserNotFoundException("Role not found: " + name));
        return convertToDto(role);
    }
    
    private RoleDto convertToDto(Role role) {
        return RoleDto.builder()
            .id(role.getId().toString())
            .name(role.getName())
            .description(role.getDescription())
            .hierarchyLevel(role.getHierarchyLevel())
            .permissions(role.getPermissions().stream()
                .map(p -> com.smartstock.identity.dto.PermissionDto.builder()
                    .id(p.getId().toString())
                    .resource(p.getResource())
                    .action(p.getAction())
                    .scope(p.getScope())
                    .description(p.getDescription())
                    .createdAt(p.getCreatedAt())
                    .build())
                .collect(Collectors.toList()))
            .createdAt(role.getCreatedAt())
            .build();
    }
}
