package com.smartstock.identity.application.service;

import com.smartstock.identity.application.dto.PagedPermissionResponse;
import com.smartstock.identity.application.dto.PermissionDto;
import com.smartstock.identity.application.dto.PermissionMutationRequest;
import com.smartstock.identity.application.support.IdentityMapper;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.repository.PermissionRepository;
import com.smartstock.identity.presentation.exception.ConflictException;
import com.smartstock.identity.presentation.exception.ResourceNotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final IdentityMapper identityMapper;

    public PermissionService(PermissionRepository permissionRepository, IdentityMapper identityMapper) {
        this.permissionRepository = permissionRepository;
        this.identityMapper = identityMapper;
    }

    @Transactional
    public PermissionDto create(PermissionMutationRequest request) {
        String permissionKey = request.permissionKey().trim().toLowerCase(Locale.ROOT);
        permissionRepository.findByPermissionKeyIgnoreCase(permissionKey)
                .ifPresent(existing -> {
                    throw new ConflictException("Permission already exists: " + permissionKey);
                });
        Permission permission = new Permission(
                permissionKey,
                request.description(),
                request.resource(),
                request.action(),
                request.scope(),
                false
        );
        permission.update(permissionKey, request.description(), request.resource(), request.action(), request.scope(), request.active());
        return identityMapper.toPermissionDto(permissionRepository.save(permission));
    }

    @Transactional(readOnly = true)
    public PermissionDto get(UUID permissionId) {
        return identityMapper.toPermissionDto(findEntity(permissionId));
    }

    @Transactional(readOnly = true)
    public PagedPermissionResponse list(int page, int size) {
        return identityMapper.toPermissionPage(permissionRepository.findAllByActiveTrue(PageRequest.of(page, size)));
    }

    @Transactional
    public PermissionDto update(UUID permissionId, PermissionMutationRequest request) {
        Permission permission = findEntity(permissionId);
        if (permission.isSystemManaged()) {
            throw new ConflictException("System-managed permissions cannot be modified.");
        }
        permission.update(request.permissionKey(), request.description(), request.resource(), request.action(), request.scope(), request.active());
        return identityMapper.toPermissionDto(permission);
    }

    @Transactional
    public void delete(UUID permissionId) {
        Permission permission = findEntity(permissionId);
        if (permission.isSystemManaged()) {
            throw new ConflictException("System-managed permissions cannot be deleted.");
        }
        permission.deactivate();
    }

    Permission findEntity(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
    }
}
