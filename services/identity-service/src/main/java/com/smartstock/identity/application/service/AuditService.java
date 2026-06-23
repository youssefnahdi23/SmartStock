package com.smartstock.identity.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.identity.domain.event.SecurityAuditEvent;
import com.smartstock.identity.domain.model.AuditLog;
import com.smartstock.identity.domain.repository.AuditLogRepository;
import com.smartstock.identity.infrastructure.security.RequestContext;
import com.smartstock.identity.infrastructure.security.RequestContextHolder;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public AuditService(AuditLogRepository auditLogRepository,
                        ObjectMapper objectMapper,
                        ApplicationEventPublisher applicationEventPublisher) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void log(String eventType,
                    String action,
                    String resource,
                    String resourceId,
                    String outcome,
                    Map<String, Object> details,
                    UUID userId,
                    String username) {
        RequestContext context = RequestContextHolder.get().orElse(null);
        String payload = serialize(details);
        auditLogRepository.save(new AuditLog(
                userId,
                username,
                eventType,
                action,
                resource,
                resourceId,
                outcome,
                payload,
                context != null ? context.ipAddress() : null,
                context != null ? context.userAgent() : null,
                context != null ? context.requestId() : null,
                context != null ? context.correlationId() : null
        ));
        applicationEventPublisher.publishEvent(new SecurityAuditEvent(
                resourceId != null ? resourceId : "system",
                resource,
                "identity-service",
                action,
                resource,
                outcome,
                payload
        ));
    }

    private String serialize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            return "{\"error\":\"Unable to serialize audit details\"}";
        }
    }
}
