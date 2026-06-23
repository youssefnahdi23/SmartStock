package com.smartstock.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, unique = true, length = 255)
    private String tokenHash;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;
    
    @Column
    private LocalDateTime revokedAt;
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
