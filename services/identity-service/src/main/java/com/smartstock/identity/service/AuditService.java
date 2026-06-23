package com.smartstock.identity.service;

public interface AuditService {
    void logSuccessfulLogin(String userId, String username);
    void logFailedLogin(String userId, String reason);
    void logEvent(String userId, String action, String entityType, String entityId, String details);
}
