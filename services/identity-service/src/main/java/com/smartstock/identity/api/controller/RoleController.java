package com.smartstock.identity.api.controller;

import com.smartstock.identity.api.dto.request.CreateRoleRequest;
import com.smartstock.identity.api.dto.response.PagedResponse;
import com.smartstock.identity.api.dto.response.RoleResponse;
import com.smartstock.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Roles", description = "Role management for RBAC")
@RestController
@RequestMapping("/identity/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "List all active roles (paginated)")
    @GetMapping
    public ResponseEntity<PagedResponse<RoleResponse>> listRoles(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoleResponse> result = roleService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.of(result, result.getContent()));
    }

    @Operation(summary = "Get role by ID")
    @GetMapping("/{roleId}")
    public ResponseEntity<Map<String, Object>> getRole(@PathVariable String roleId) {
        RoleResponse role = roleService.findById(roleId);
        return ResponseEntity.ok(wrapData(role));
    }

    @Operation(summary = "Create a new role (admin only)")
    @PostMapping
    @PreAuthorize("hasAuthority('role:admin:create')")
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wrapData(role));
    }

    // --------------------------------------------------------

    private Map<String, Object> wrapData(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString()));
    }
}
