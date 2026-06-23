package com.smartstock.identity.presentation.controller;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.application.dto.PagedPermissionResponse;
import com.smartstock.identity.application.dto.PermissionDto;
import com.smartstock.identity.application.dto.PermissionMutationRequest;
import com.smartstock.identity.application.service.PermissionService;
import com.smartstock.identity.presentation.support.ApiResponseFactory;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
@PreAuthorize("hasAuthority('permission:manage:global') or hasRole('SYSTEM_ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;
    private final ApiResponseFactory apiResponseFactory;

    public PermissionController(PermissionService permissionService, ApiResponseFactory apiResponseFactory) {
        this.permissionService = permissionService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionDto>> create(@Valid @RequestBody PermissionMutationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponseFactory.success(HttpStatus.CREATED, permissionService.create(request), "Permission created successfully."));
    }

    @GetMapping("/{permissionId}")
    public ResponseEntity<ApiResponse<PermissionDto>> get(@PathVariable UUID permissionId) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, permissionService.get(permissionId), "Permission retrieved successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedPermissionResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, permissionService.list(page, size), "Permissions retrieved successfully."));
    }

    @PutMapping("/{permissionId}")
    public ResponseEntity<ApiResponse<PermissionDto>> update(@PathVariable UUID permissionId,
                                                             @Valid @RequestBody PermissionMutationRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, permissionService.update(permissionId, request), "Permission updated successfully."));
    }

    @DeleteMapping("/{permissionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID permissionId) {
        permissionService.delete(permissionId);
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, null, "Permission deleted successfully."));
    }
}
