package com.smartstock.identity.controller;

import com.smartstock.identity.dto.UserCreateRequest;
import com.smartstock.identity.dto.UserResponse;
import com.smartstock.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Creating new user: {}", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Fetching user: {}", userId);
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/username/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        log.info("Fetching user by username: {}", username);
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users");
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateUser(
        @PathVariable String userId,
        @Valid @RequestBody UserCreateRequest request) {
        log.info("Updating user: {}", userId);
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.info("Deleting user: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> assignRole(
        @PathVariable String userId,
        @PathVariable String roleName) {
        log.info("Assigning role {} to user {}", roleName, userId);
        userService.assignRoleToUser(userId, roleName);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Void> removeRole(
        @PathVariable String userId,
        @PathVariable String roleName) {
        log.info("Removing role {} from user {}", roleName, userId);
        userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.noContent().build();
    }
}
