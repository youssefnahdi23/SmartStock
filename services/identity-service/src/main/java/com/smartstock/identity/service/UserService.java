package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.AssignRoleRequest;
import com.smartstock.identity.api.dto.request.ChangePasswordRequest;
import com.smartstock.identity.api.dto.request.RegisterRequest;
import com.smartstock.identity.api.dto.request.UpdateUserRequest;
import com.smartstock.identity.api.dto.response.UserResponse;
import com.smartstock.identity.domain.model.Permission;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndNotDeleted(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        if (userRepository.existsByEmailAndNotDeleted(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        Set<Role> roles = resolveRoles(request.getRoleIds());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .active(true)
                .emailVerified(false)
                .passwordChangedAt(LocalDateTime.now())
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        User user = userRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public User findEntityById(String id) {
        return userRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(String role, Boolean active, String search, Pageable pageable) {
        return userRepository.findAllWithFilters(role, active, search, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public UserResponse update(String id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndNotDeleted(request.getEmail())) {
                throw new UserAlreadyExistsException("email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request, String requestingUserId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD",
                    "Current password is incorrect", org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    @Transactional
    public UserResponse deactivate(String id) {
        User user = userRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse reactivate(String id) {
        User user = userRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void assignRole(String userId, AssignRoleRequest request) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findByIdAndActive(request.getRoleId())
                .orElseThrow(() -> new RoleNotFoundException(request.getRoleId()));
        user.addRole(role);
        userRepository.save(user);
        log.info("Role {} assigned to user {}", role.getName(), userId);
    }

    @Transactional
    public void removeRole(String userId, String roleId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        user.removeRole(role);
        userRepository.save(user);
        log.info("Role {} removed from user {}", role.getName(), userId);
    }

    @Transactional
    public void resetPasswordWithToken(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // --------------------------------------------------------
    // Helpers
    // --------------------------------------------------------

    private Set<Role> resolveRoles(List<String> roleIds) {
        Set<Role> roles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (String roleId : roleIds) {
                roleRepository.findByIdAndActive(roleId).ifPresent(roles::add);
            }
        }
        return roles;
    }

    public UserResponse toResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
        List<String> permNames = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .roles(roleNames)
                .permissions(permNames)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
