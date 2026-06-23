package com.smartstock.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resource", "action", "scope"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 50)
    private String resource;
    
    @Column(nullable = false, length = 50)
    private String action;
    
    @Column(length = 100)
    private String scope;
    
    @Column(length = 255)
    private String description;
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime deletedAt;
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
