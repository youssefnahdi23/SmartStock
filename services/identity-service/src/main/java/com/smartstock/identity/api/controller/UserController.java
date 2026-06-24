package com.smartstock.identity.api.controller;

import com.smartstock.identity.api.dto.request.*;
import com.smartstock.identity.api.dto.response.PagedResponse;
import com.smartstock.identity.api.dto.response.UserResponse;
import com.smartstock.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Users", description = "User management and profile operations")
@RestController
@RequestMapping("/identity/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(wrapData(user));
    }

    @Operation(summary = "Get user profile by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId,
                                                        @AuthenticationPrincipal UserDetails principal) {
        String requestingId = principal.getUsername();
        // Users can view their own profile; admins can view any
        if (!requestingId.equals(userId)) {
            assertHasAuthority(principal, "user:admin:read");
        }
        UserResponse user = userService.findById(userId);
        return ResponseEntity.ok(wrapData(user));
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String userId,
                                                           @Valid @RequestBody UpdateUserRequest request,
                                                           @AuthenticationPrincipal UserDetails principal) {
        String requestingId = principal.getUsername();
        if (!requestingId.equals(userId)) {
            assertHasAuthority(principal, "user:admin:write");
        }
        UserResponse user = userService.update(userId, request);
        return ResponseEntity.ok(wrapData(user));
    }

    @Operation(summary = "Change user password")
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@PathVariable String userId,
                                                               @Valid @RequestBody ChangePasswordRequest request,
                                                               @AuthenticationPrincipal UserDetails principal) {
        userService.changePassword(userId, request, principal.getUsername());
        return ResponseEntity.ok(wrapData(Map.of("message", "Password changed successfully")));
    }

    @Operation(summary = "List all users (admin only)")
    @GetMapping
    @PreAuthorize("hasAuthority('user:admin:read')")
    public ResponseEntity<PagedResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        size = Math.min(size, 100);
        String[] sortParts = sort.split(",");
        Sort.Direction dir = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortParts[0]));

        Page<UserResponse> result = userService.findAll(role, active, search, pageable);
        return ResponseEntity.ok(PagedResponse.of(result, result.getContent()));
    }

    @Operation(summary = "Deactivate user account (admin only)")
    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAuthority('user:admin:write')")
    public ResponseEntity<Map<String, Object>> deactivate(@PathVariable String userId) {
        UserResponse user = userService.deactivate(userId);
        return ResponseEntity.ok(wrapData(user));
    }

    @Operation(summary = "Reactivate user account (admin only)")
    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasAuthority('user:admin:write')")
    public ResponseEntity<Map<String, Object>> reactivate(@PathVariable String userId) {
        UserResponse user = userService.reactivate(userId);
        return ResponseEntity.ok(wrapData(user));
    }

    @Operation(summary = "Assign role to user (admin only)")
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('role:assign')")
    public ResponseEntity<Map<String, Object>> assignRole(@PathVariable String userId,
                                                           @Valid @RequestBody AssignRoleRequest request) {
        userService.assignRole(userId, request);
        return ResponseEntity.ok(wrapData(Map.of(
                "userId", userId, "roleId", request.getRoleId(),
                "assignedAt", Instant.now().toString())));
    }

    @Operation(summary = "Remove role from user (admin only)")
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('role:revoke')")
    public ResponseEntity<Map<String, Object>> removeRole(@PathVariable String userId,
                                                           @PathVariable String roleId) {
        userService.removeRole(userId, roleId);
        return ResponseEntity.ok(wrapData(Map.of(
                "message", "Role removed successfully",
                "userId", userId,
                "roleId", roleId)));
    }

    // --------------------------------------------------------

    private Map<String, Object> wrapData(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of("timestamp", Instant.now().toString()));
    }

    private void assertHasAuthority(UserDetails principal, String authority) {
        boolean has = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
        if (!has) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Insufficient permissions");
        }
    }
}
