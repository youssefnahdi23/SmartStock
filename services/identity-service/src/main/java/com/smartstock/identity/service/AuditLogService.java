package com.smartstock.identity.service;

import com.smartstock.identity.domain.model.AuditLog;
import com.smartstock.identity.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String entityType, String entityId,
                    String actorId, String actionType, String status,
                    String ipAddress, String userAgent, String errorMessage) {
        try {
            AuditLog entry = AuditLog.builder()
                    .eventType(eventType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .actorId(actorId)
                    .actionType(actionType)
                    .status(status)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .errorMessage(errorMessage)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log: eventType={}, entityId={}", eventType, entityId, ex);
        }
    }
}
