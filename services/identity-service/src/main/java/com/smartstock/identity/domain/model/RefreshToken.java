package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "issued_ip", length = 64)
    private String issuedIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RefreshToken(User user,
                        String tokenId,
                        String tokenHash,
                        Instant expiresAt,
                        Instant issuedAt,
                        String issuedIp,
                        String userAgent) {
        this.user = user;
        this.tokenId = tokenId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.issuedAt = issuedAt;
        this.issuedIp = issuedIp;
        this.userAgent = userAgent;
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

    public boolean isActive(Clock clock) {
        return revokedAt == null && expiresAt.isAfter(clock.instant());
    }

    public void markUsed(Instant usedAt) {
        this.lastUsedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
