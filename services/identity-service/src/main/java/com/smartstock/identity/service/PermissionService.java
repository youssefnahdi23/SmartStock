package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.response.PermissionResponse;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAllByActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Permission findByName(String name) {
        return permissionRepository.findByNameAndActive(name)
                .orElseThrow(() -> new com.smartstock.identity.exception.BusinessException(
                        "PERMISSION_NOT_FOUND", "Permission not found: " + name,
                        org.springframework.http.HttpStatus.NOT_FOUND));
    }

    private PermissionResponse toResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .resource(p.getResource())
                .action(p.getAction())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
