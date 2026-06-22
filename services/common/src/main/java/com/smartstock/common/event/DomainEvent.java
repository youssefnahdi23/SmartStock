package com.smartstock.common.event;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base domain event class.
 * All business events must extend this class.
 * Events are immutable, versioned, and replayable (ADR-0002, ADR-0004).
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({})
public abstract class DomainEvent {
    private String eventId;
    private String eventType;
    private int eventVersion;
    private LocalDateTime timestamp;
    private String aggregateId;
    private String aggregateType;
    private String userId;
    private String serviceName;
    private String correlationId;
    private String causationId;

    protected DomainEvent(String aggregateId, String aggregateType, String serviceName) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.eventVersion = 1;
        this.timestamp = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.serviceName = serviceName;
        this.correlationId = UUID.randomUUID().toString();
    }
}
