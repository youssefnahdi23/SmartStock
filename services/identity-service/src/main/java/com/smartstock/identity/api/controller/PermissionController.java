package com.smartstock.identity.api.controller;

import com.smartstock.identity.api.dto.response.PermissionResponse;
import com.smartstock.identity.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Tag(name = "Permissions", description = "Permission catalog")
@RestController
@RequestMapping("/identity/permissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "List all active permissions")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPermissions() {
        List<PermissionResponse> permissions = permissionService.findAll();
        return ResponseEntity.ok(Map.of(
                "data", permissions,
                "meta", Map.of("timestamp", Instant.now().toString())));
    }
}
