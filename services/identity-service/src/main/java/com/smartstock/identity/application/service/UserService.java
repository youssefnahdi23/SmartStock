package com.smartstock.identity.application.service;

import com.smartstock.identity.application.dto.AssignRolesRequest;
import com.smartstock.identity.application.dto.UserCreateRequest;
import com.smartstock.identity.application.dto.UserCreateResponse;
import com.smartstock.identity.application.dto.UserPageResponse;
import com.smartstock.identity.application.dto.UserUpdateRequest;
import com.smartstock.identity.application.support.IdentityMapper;
import com.smartstock.identity.domain.event.UserCreatedEvent;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.presentation.exception.ConflictException;
import com.smartstock.identity.presentation.exception.UserNotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordService passwordService;
    private final IdentityMapper identityMapper;
    private final AuditService auditService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    public UserService(UserRepository userRepository,
                       RoleService roleService,
                       PasswordService passwordService,
                       IdentityMapper identityMapper,
                       AuditService auditService,
                       ApplicationEventPublisher applicationEventPublisher,
                       Clock clock) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordService = passwordService;
        this.identityMapper = identityMapper;
        this.auditService = auditService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public UserCreateResponse create(UserCreateRequest request) {
        ensureUnique(request.username(), request.email());
        PasswordService.EncodedPassword encodedPassword = passwordService.preparePassword(null, request.password());
        List<Role> roles = roleService.resolveRoles(request.roleIds(), request.roleNames());
        User user = new User(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                encodedPassword.hash(),
                encodedPassword.changedAt(),
                encodedPassword.expiresAt(),
                roles,
                request.warehouseIds()
        );
        User savedUser = userRepository.save(user);
        passwordService.recordPasswordHistory(savedUser, encodedPassword.hash());
        auditService.log("USER_CREATED", "CREATE", "user", savedUser.getId().toString(), "SUCCESS",
                Map.of("username", savedUser.getUsername(), "roles", roles.stream().map(Role::getName).toList()),
                savedUser.getId(), savedUser.getUsername());
        applicationEventPublisher.publishEvent(new UserCreatedEvent(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                "identity-service"
        ));
        return identityMapper.toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserCreateResponse get(UUID userId) {
        return identityMapper.toUserResponse(findEntity(userId));
    }

    @Transactional(readOnly = true)
    public UserPageResponse list(int page, int size) {
        return identityMapper.toUserPage(userRepository.findAllByDeletedAtIsNull(PageRequest.of(page, size)));
    }

    @Transactional
    public UserCreateResponse update(UUID userId, UserUpdateRequest request) {
        User user = findEntity(userId);
        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(request.email())) {
            throw new ConflictException("Email is already in use.");
        }
        user.updateProfile(request.email(), request.firstName(), request.lastName(), request.active());
        if ((request.roleIds() != null && !request.roleIds().isEmpty()) || (request.roleNames() != null && !request.roleNames().isEmpty())) {
            user.replaceRoles(roleService.resolveRoles(request.roleIds(), request.roleNames()));
        }
        if (request.warehouseIds() != null && !request.warehouseIds().isEmpty()) {
            user.replaceWarehouseIds(request.warehouseIds());
        }
        auditService.log("USER_UPDATED", "UPDATE", "user", user.getId().toString(), "SUCCESS",
                Map.of("username", user.getUsername(), "active", user.isActive()),
                user.getId(), user.getUsername());
        return identityMapper.toUserResponse(user);
    }

    @Transactional
    public UserCreateResponse assignRoles(UUID userId, AssignRolesRequest request) {
        User user = findEntity(userId);
        user.replaceRoles(roleService.resolveRoles(request.roleIds(), null));
        user.replaceWarehouseIds(request.warehouseIds());
        auditService.log("USER_ROLES_ASSIGNED", "ASSIGN_ROLES", "user", user.getId().toString(), "SUCCESS",
                Map.of("roles", user.getRoles().stream().map(Role::getName).toList(), "warehouseIds", user.getWarehouseIds()),
                user.getId(), user.getUsername());
        return identityMapper.toUserResponse(user);
    }

    @Transactional
    public void delete(UUID userId) {
        User user = findEntity(userId);
        user.softDelete(clock.instant());
        auditService.log("USER_DELETED", "DELETE", "user", user.getId().toString(), "SUCCESS",
                Map.of("username", user.getUsername()),
                user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public User findEntity(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private void ensureUnique(String username, String email) {
        if (userRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username)) {
            throw new ConflictException("Username is already in use.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new ConflictException("Email is already in use.");
        }
    }
}
