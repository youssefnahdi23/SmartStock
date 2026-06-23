package com.smartstock.identity.presentation.controller;

import com.smartstock.common.api.ApiResponse;
import com.smartstock.identity.application.dto.AssignRolesRequest;
import com.smartstock.identity.application.dto.UserCreateRequest;
import com.smartstock.identity.application.dto.UserCreateResponse;
import com.smartstock.identity.application.dto.UserPageResponse;
import com.smartstock.identity.application.dto.UserUpdateRequest;
import com.smartstock.identity.application.service.UserService;
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
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('user:manage:global') or hasRole('SYSTEM_ADMIN')")
public class UserController {

    private final UserService userService;
    private final ApiResponseFactory apiResponseFactory;

    public UserController(UserService userService, ApiResponseFactory apiResponseFactory) {
        this.userService = userService;
        this.apiResponseFactory = apiResponseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserCreateResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponseFactory.success(HttpStatus.CREATED, userService.create(request), "User created successfully."));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserCreateResponse>> get(@PathVariable UUID userId) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, userService.get(userId), "User retrieved successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserPageResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, userService.list(page, size), "Users retrieved successfully."));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserCreateResponse>> update(@PathVariable UUID userId,
                                                                  @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, userService.update(userId, request), "User updated successfully."));
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<UserCreateResponse>> assignRoles(@PathVariable UUID userId,
                                                                       @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, userService.assignRoles(userId, request), "User roles assigned successfully."));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID userId) {
        userService.delete(userId);
        return ResponseEntity.ok(apiResponseFactory.success(HttpStatus.OK, null, "User deleted successfully."));
    }
}
