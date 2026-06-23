package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "failed_login_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "first_attempt_at")
    private Instant firstAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "last_ip_address", length = 64)
    private String lastIpAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FailedLoginAttempt(String username, UUID userId) {
        this.username = username.trim().toLowerCase();
        this.userId = userId;
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

    public void registerFailure(Instant now, String ipAddress, int maxAttempts, java.time.Duration resetWindow, java.time.Duration lockoutDuration) {
        if (firstAttemptAt == null || lastAttemptAt == null || lastAttemptAt.plus(resetWindow).isBefore(now)) {
            this.attemptCount = 0;
            this.firstAttemptAt = now;
        }
        this.attemptCount++;
        this.lastAttemptAt = now;
        this.lastIpAddress = ipAddress;
        if (attemptCount >= maxAttempts) {
            this.lockExpiresAt = now.plus(lockoutDuration);
        }
    }

    public void reset() {
        this.attemptCount = 0;
        this.firstAttemptAt = null;
        this.lastAttemptAt = null;
        this.lockExpiresAt = null;
        this.lastIpAddress = null;
    }

    public boolean isLocked(Clock clock) {
        return lockExpiresAt != null && lockExpiresAt.isAfter(clock.instant());
    }
}
