package com.smartstock.identity.application.service;

import com.smartstock.identity.application.dto.PagedRoleResponse;
import com.smartstock.identity.application.dto.RoleDto;
import com.smartstock.identity.application.dto.RoleMutationRequest;
import com.smartstock.identity.application.support.IdentityMapper;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.repository.PermissionRepository;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.presentation.exception.ConflictException;
import com.smartstock.identity.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final IdentityMapper identityMapper;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       IdentityMapper identityMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.identityMapper = identityMapper;
    }

    @Transactional
    public RoleDto create(RoleMutationRequest request) {
        String normalizedName = request.name().trim().toUpperCase(Locale.ROOT);
        roleRepository.findByNameIgnoreCase(normalizedName).ifPresent(existing -> {
            throw new ConflictException("Role already exists: " + normalizedName);
        });
        Role role = new Role(normalizedName, request.description(), request.hierarchyRank(), false, resolvePermissions(request.permissionIds()));
        role.update(normalizedName, request.description(), request.hierarchyRank(), request.active(), resolvePermissions(request.permissionIds()));
        return identityMapper.toRoleDto(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public RoleDto get(UUID roleId) {
        return identityMapper.toRoleDto(findEntity(roleId));
    }

    @Transactional(readOnly = true)
    public PagedRoleResponse list(int page, int size) {
        return identityMapper.toRolePage(roleRepository.findAllByActiveTrue(PageRequest.of(page, size)));
    }

    @Transactional
    public RoleDto update(UUID roleId, RoleMutationRequest request) {
        Role role = findEntity(roleId);
        if (role.isSystemManaged()) {
            throw new ConflictException("System-managed roles cannot be modified.");
        }
        role.update(request.name(), request.description(), request.hierarchyRank(), request.active(), resolvePermissions(request.permissionIds()));
        return identityMapper.toRoleDto(role);
    }

    @Transactional
    public void delete(UUID roleId) {
        Role role = findEntity(roleId);
        if (role.isSystemManaged()) {
            throw new ConflictException("System-managed roles cannot be deleted.");
        }
        role.deactivate();
    }

    @Transactional(readOnly = true)
    public List<Role> resolveRoles(Set<UUID> roleIds, Set<String> roleNames) {
        if (roleIds != null && !roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findByIdIn(roleIds);
            if (roles.size() != roleIds.size()) {
                throw new ResourceNotFoundException("One or more roles could not be found.");
            }
            return roles;
        }
        if (roleNames != null && !roleNames.isEmpty()) {
            Set<String> normalized = roleNames.stream().map(name -> name.trim().toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            List<Role> roles = roleRepository.findByNames(normalized);
            if (roles.size() != normalized.size()) {
                throw new ResourceNotFoundException("One or more roles could not be found.");
            }
            return roles;
        }
        throw new ResourceNotFoundException("At least one role identifier or role name must be provided.");
    }

    Role findEntity(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
    }

    private List<Permission> resolvePermissions(Set<UUID> permissionIds) {
        List<Permission> permissions = permissionRepository.findByIdIn(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new ResourceNotFoundException("One or more permissions could not be found.");
        }
        return permissions;
    }
}
