package com.smartstock.identity.service.impl;

import com.smartstock.identity.entity.AuditLog;
import com.smartstock.identity.repository.AuditLogRepository;
import com.smartstock.identity.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Override
    public void logSuccessfulLogin(String userId, String username) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", username);
        logEvent(userId, "LOGIN_SUCCESS", "User", userId, "User login successful");
    }
    
    @Override
    public void logFailedLogin(String userId, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        
        AuditLog auditLog = AuditLog.builder()
            .userId(userId == null || userId.isEmpty() ? null : UUID.fromString(userId))
            .action("LOGIN_FAILED")
            .entityType("User")
            .entityId(userId)
            .details(details)
            .status("FAILURE")
            .errorMessage(reason)
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();
        
        auditLogRepository.save(auditLog);
        log.info("Failed login attempt: {}", reason);
    }
    
    @Override
    public void logEvent(String userId, String action, String entityType, String entityId, String details) {
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("message", details);
        
        AuditLog auditLog = AuditLog.builder()
            .userId(userId == null || userId.isEmpty() ? null : UUID.fromString(userId))
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .details(detailsMap)
            .status("SUCCESS")
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .build();
        
        auditLogRepository.save(auditLog);
        log.debug("Audit event logged - Action: {}, User: {}, Entity: {}", action, userId, entityType);
    }
    
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ip = attributes.getRequest().getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = attributes.getRequest().getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Failed to get client IP: {}", e.getMessage());
        }
        return null;
    }
    
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Failed to get user agent: {}", e.getMessage());
        }
        return null;
    }
}
