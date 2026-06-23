package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "password_expires_at", nullable = false)
    private Instant passwordExpiresAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_warehouse_access", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "warehouse_id", nullable = false, length = 100)
    private Set<String> warehouseIds = new LinkedHashSet<>();

    public User(String username,
                String email,
                String firstName,
                String lastName,
                String passwordHash,
                Instant passwordChangedAt,
                Instant passwordExpiresAt,
                Collection<Role> roles,
                Collection<String> warehouseIds) {
        this.username = normalizeUsername(username);
        this.email = normalizeEmail(email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.active = true;
        this.emailVerified = false;
        this.passwordChangedAt = passwordChangedAt;
        this.passwordExpiresAt = passwordExpiresAt;
        replaceRoles(roles);
        replaceWarehouseIds(warehouseIds);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (passwordChangedAt == null) {
            this.passwordChangedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String email, String firstName, String lastName, boolean active) {
        this.email = normalizeEmail(email);
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public void replaceRoles(Collection<Role> assignedRoles) {
        this.roles.clear();
        if (assignedRoles != null) {
            this.roles.addAll(assignedRoles);
        }
    }

    public void replaceWarehouseIds(Collection<String> warehouseIds) {
        this.warehouseIds.clear();
        if (warehouseIds != null) {
            warehouseIds.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(String::toUpperCase)
                    .forEach(this.warehouseIds::add);
        }
    }

    public void updatePassword(String passwordHash, Instant changedAt, Instant expiresAt) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = changedAt;
        this.passwordExpiresAt = expiresAt;
    }

    public void recordSuccessfulLogin(Instant at) {
        this.lastLoginAt = at;
        this.lockedUntil = null;
    }

    public void lockUntil(Instant lockExpiresAt) {
        this.lockedUntil = lockExpiresAt;
    }

    public void unlock() {
        this.lockedUntil = null;
    }

    public boolean isLocked(Clock clock) {
        return lockedUntil != null && lockedUntil.isAfter(clock.instant());
    }

    public boolean isPasswordExpired(Clock clock) {
        return passwordExpiresAt != null && !passwordExpiresAt.isAfter(clock.instant());
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
        this.active = false;
    }

    private String normalizeUsername(String value) {
        return Objects.requireNonNull(value, "username").trim().toLowerCase();
    }

    private String normalizeEmail(String value) {
        return Objects.requireNonNull(value, "email").trim().toLowerCase();
    }
}
