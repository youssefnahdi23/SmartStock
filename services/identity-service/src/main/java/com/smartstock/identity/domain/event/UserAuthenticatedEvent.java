package com.smartstock.identity.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Published when a user successfully authenticates.
 * Consumed by Audit Service and Analytics Service for metrics.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserAuthenticatedEvent extends DomainEvent {
    private String username;
    private String email;

    public UserAuthenticatedEvent(String userId, String username, String email, String serviceName) {
        super(userId, "User", serviceName);
        this.username = username;
        this.email = email;
    }
}
