package com.smartstock.identity.service.impl;

import com.smartstock.identity.dto.UserCreateRequest;
import com.smartstock.identity.dto.UserResponse;
import com.smartstock.identity.entity.Role;
import com.smartstock.identity.entity.User;
import com.smartstock.identity.exception.UserNotFoundException;
import com.smartstock.identity.mapper.UserMapper;
import com.smartstock.identity.repository.RoleRepository;
import com.smartstock.identity.repository.UserRepository;
import com.smartstock.identity.service.AuditService;
import com.smartstock.identity.service.PasswordService;
import com.smartstock.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    
    @Override
    public UserResponse createUser(UserCreateRequest request) {
        log.debug("Creating new user: {}", request.getUsername());
        
        // Validate password
        passwordService.validatePassword(request.getPassword());
        
        // Check if user already exists
        if (userRepository.findByUsernameAndDeletedAtIsNull(request.getUsername()).isPresent()) {
            auditService.logEvent("", "USER_CREATION_FAILED", "User", "", "Username already exists: " + request.getUsername());
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (userRepository.findByEmailAndDeletedAtIsNull(request.getEmail()).isPresent()) {
            auditService.logEvent("", "USER_CREATION_FAILED", "User", "", "Email already exists: " + request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .isActive(true)
            .isLocked(false)
            .passwordExpiresAt(LocalDateTime.now().plusDays(90))
            .roles(new HashSet<>())
            .build();
        
        // Assign roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoleNames()) {
            Role role = roleRepository.findByNameAndDeletedAtIsNull(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            roles.add(role);
        }
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        // Store password in history
        passwordService.addPasswordToHistory(savedUser.getId(), request.getPassword());
        
        auditService.logEvent(savedUser.getId().toString(), "USER_CREATED", "User", savedUser.getId().toString(), "User created successfully");
        
        return userMapper.toDto(savedUser);
    }
    
    @Override
    public UserResponse getUserById(String userId) {
        User user = getUserEntityById(userId);
        return userMapper.toDto(user);
    }
    
    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndDeletedAtIsNull(username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return userMapper.toDto(user);
    }
    
    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .filter(u -> u.getDeletedAt() == null)
            .map(userMapper::toDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public UserResponse updateUser(String userId, UserCreateRequest request) {
        log.debug("Updating user: {}", userId);
        
        User user = getUserEntityById(userId);
        
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            passwordService.validatePassword(request.getPassword());
            passwordService.validatePasswordNotReused(user.getId(), request.getPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setPasswordExpiresAt(LocalDateTime.now().plusDays(90));
            passwordService.addPasswordToHistory(user.getId(), request.getPassword());
        }
        
        User updatedUser = userRepository.save(user);
        auditService.logEvent(userId, "USER_UPDATED", "User", userId, "User updated");
        
        return userMapper.toDto(updatedUser);
    }
    
    @Override
    public void deleteUser(String userId) {
        log.debug("Deleting user: {}", userId);
        
        User user = getUserEntityById(userId);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        auditService.logEvent(userId, "USER_DELETED", "User", userId, "User deleted (soft delete)");
    }
    
    @Override
    public void assignRoleToUser(String userId, String roleName) {
        log.debug("Assigning role {} to user: {}", roleName, userId);
        
        User user = getUserEntityById(userId);
        Role role = roleRepository.findByNameAndDeletedAtIsNull(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        user.getRoles().add(role);
        userRepository.save(user);
        
        auditService.logEvent(userId, "ROLE_ASSIGNED", "User", userId, "Role assigned: " + roleName);
    }
    
    @Override
    public void removeRoleFromUser(String userId, String roleName) {
        log.debug("Removing role {} from user: {}", roleName, userId);
        
        User user = getUserEntityById(userId);
        Role role = roleRepository.findByNameAndDeletedAtIsNull(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        user.getRoles().remove(role);
        userRepository.save(user);
        
        auditService.logEvent(userId, "ROLE_REMOVED", "User", userId, "Role removed: " + roleName);
    }
    
    @Override
    public User getUserEntityById(String userId) {
        return userRepository.findById(java.util.UUID.fromString(userId))
            .filter(u -> u.getDeletedAt() == null)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
