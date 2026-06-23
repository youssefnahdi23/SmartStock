package com.smartstock.identity.controller;

import com.smartstock.identity.dto.RoleDto;
import com.smartstock.identity.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final RoleService roleService;
    
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        log.info("Fetching all roles");
        List<RoleDto> response = roleService.getAllRoles();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{roleId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable String roleId) {
        log.info("Fetching role: {}", roleId);
        RoleDto response = roleService.getRoleById(roleId);
        return ResponseEntity.ok(response);
    }
}
