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
 * Events are immutable, versioned, and replayable (ADR-0002, ADR-0004, ADR-0005).
 *
 * <p>Metadata fields per ADR-0005 event standard:
 * <ul>
 *   <li>{@code eventId} — unique event UUID</li>
 *   <li>{@code eventType} — simple class name of the concrete event</li>
 *   <li>{@code eventVersion} — monotonic integer; 1 for initial, 2+ for breaking payload changes</li>
 *   <li>{@code timestamp} — when the action occurred (UTC)</li>
 *   <li>{@code correlationId} — propagated from the inbound request header {@code X-Correlation-ID};
 *       defaults to a new UUID when no request context is available (e.g. scheduled jobs)</li>
 *   <li>{@code requestId} — the unique ID of the HTTP request that triggered the action;
 *       defaults to a new UUID when no request context is available</li>
 *   <li>{@code causationId} — optional: eventId of the upstream event that caused this one
 *       (set by event-chain consumers)</li>
 *   <li>{@code serviceName} — canonical name of the publishing service</li>
 *   <li>{@code userId} — authenticated principal who triggered the action (may be null for
 *       system-driven events)</li>
 * </ul>
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
    /** Request correlation ID for end-to-end tracing (ADR-0005). Propagate from X-Correlation-ID. */
    private String correlationId;
    /** Original HTTP request ID (ADR-0005). Propagate from X-Request-ID header when available. */
    private String requestId;
    /** ID of the upstream event that caused this one; null for command-driven events. */
    private String causationId;

    protected DomainEvent(String aggregateId, String aggregateType, String serviceName) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.eventVersion = 1;
        this.timestamp = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.serviceName = serviceName;
        // TODO: replace with MDC.get("correlationId") once request-scoped MDC propagation is
        //       wired through all services (tracked as tech-debt).  For now each event carries
        //       its own UUID so downstream services at least see a non-null correlationId.
        this.correlationId = UUID.randomUUID().toString();
        this.requestId = UUID.randomUUID().toString();
    }
}
