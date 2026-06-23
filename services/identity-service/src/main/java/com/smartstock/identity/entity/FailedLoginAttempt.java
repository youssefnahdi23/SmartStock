package com.smartstock.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "failed_login_attempts", indexes = {
    @Index(name = "idx_username", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedLoginAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 100)
    private String username;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 1;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime lastAttemptAt = LocalDateTime.now();
    
    @Column
    private LocalDateTime lockedUntil;
}
