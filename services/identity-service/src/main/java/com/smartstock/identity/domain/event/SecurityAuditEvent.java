package com.smartstock.identity.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.Getter;

@Getter
public class SecurityAuditEvent extends DomainEvent {

    private final String action;
    private final String resource;
    private final String outcome;
    private final String details;

    public SecurityAuditEvent(String aggregateId,
                              String aggregateType,
                              String serviceName,
                              String action,
                              String resource,
                              String outcome,
                              String details) {
        super(aggregateId, aggregateType, serviceName);
        this.action = action;
        this.resource = resource;
        this.outcome = outcome;
        this.details = details;
    }
}
