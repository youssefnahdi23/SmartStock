package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 128)
    private String permissionKey;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 64)
    private String resource;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 128)
    private String scope;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Permission(String permissionKey, String description, String resource, String action, String scope, boolean systemManaged) {
        this.permissionKey = permissionKey.trim().toLowerCase();
        this.description = description;
        this.resource = resource.trim().toLowerCase();
        this.action = action.trim().toLowerCase();
        this.scope = scope.trim().toLowerCase();
        this.active = true;
        this.systemManaged = systemManaged;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String permissionKey, String description, String resource, String action, String scope, boolean active) {
        this.permissionKey = permissionKey.trim().toLowerCase();
        this.description = description;
        this.resource = resource.trim().toLowerCase();
        this.action = action.trim().toLowerCase();
        this.scope = scope.trim().toLowerCase();
        this.active = active;
    }

    public void deactivate() {
        this.active = false;
    }
}
