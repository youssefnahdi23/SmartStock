package com.smartstock.identity.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column
    private UUID userId;
    
    @Column(nullable = false, length = 50)
    private String action;
    
    @Column(nullable = false, length = 50)
    private String entityType;
    
    @Column(length = 100)
    private String entityId;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(nullable = false, length = 50)
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
