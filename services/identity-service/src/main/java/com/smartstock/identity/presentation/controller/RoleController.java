package com.smartstock.identity.presentation.controller;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.application.dto.PagedRoleResponse;
import com.smartstock.identity.application.dto.RoleDto;
import com.smartstock.identity.application.dto.RoleMutationRequest;
import com.smartstock.identity.application.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('role:manage:global') or hasRole('SYSTEM_ADMIN')")
public class RoleController {

    private final RoleService roleService;
    private final ApiResponseFactory apiResponseFactory;

    public RoleController(RoleService roleService, ApiResponseFactory apiResponseFactory) {
        this.roleService = roleService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> create(@Valid @RequestBody RoleMutationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponseFactory.success(HttpStatus.CREATED, roleService.create(request), "Role created successfully."));
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleDto>> get(@PathVariable UUID roleId) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, roleService.get(roleId), "Role retrieved successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedRoleResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, roleService.list(page, size), "Roles retrieved successfully."));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<RoleDto>> update(@PathVariable UUID roleId,
                                                       @Valid @RequestBody RoleMutationRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, roleService.update(roleId, request), "Role updated successfully."));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID roleId) {
        roleService.delete(roleId);
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, null, "Role deleted successfully."));
    }
}
