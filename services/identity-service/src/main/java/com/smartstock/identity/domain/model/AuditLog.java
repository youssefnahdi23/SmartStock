package com.smartstock.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 100)
    private String username;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog(UUID userId,
                    String username,
                    String eventType,
                    String action,
                    String resource,
                    String resourceId,
                    String outcome,
                    String details,
                    String ipAddress,
                    String userAgent,
                    String requestId,
                    String correlationId) {
        this.userId = userId;
        this.username = username;
        this.eventType = eventType;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.outcome = outcome;
        this.details = details;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestId = requestId;
        this.correlationId = correlationId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
