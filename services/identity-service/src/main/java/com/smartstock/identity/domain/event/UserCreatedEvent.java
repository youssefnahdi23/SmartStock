package com.smartstock.identity.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Published when a new user is registered.
 * Consumed by Audit Service and Notification Service.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserCreatedEvent extends DomainEvent {
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public UserCreatedEvent(String userId, String username, String email,
                           String firstName, String lastName, String serviceName) {
        super(userId, "User", serviceName);
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
