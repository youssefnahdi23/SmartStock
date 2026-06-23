package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "hierarchy_rank", nullable = false)
    private int hierarchyRank;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "system_managed", nullable = false)
    private boolean systemManaged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new LinkedHashSet<>();

    public Role(String name, String description, int hierarchyRank, boolean systemManaged, Collection<Permission> permissions) {
        this.name = name.trim().toUpperCase();
        this.description = description;
        this.hierarchyRank = hierarchyRank;
        this.active = true;
        this.systemManaged = systemManaged;
        replacePermissions(permissions);
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

    public void update(String name, String description, int hierarchyRank, boolean active, Collection<Permission> permissions) {
        this.name = name.trim().toUpperCase();
        this.description = description;
        this.hierarchyRank = hierarchyRank;
        this.active = active;
        replacePermissions(permissions);
    }

    public void replacePermissions(Collection<Permission> updatedPermissions) {
        this.permissions.clear();
        if (updatedPermissions != null) {
            this.permissions.addAll(updatedPermissions);
        }
    }

    public void deactivate() {
        this.active = false;
    }
}
