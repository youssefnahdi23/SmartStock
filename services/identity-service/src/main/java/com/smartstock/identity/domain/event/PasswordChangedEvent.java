package com.smartstock.identity.domain.event;

import com.smartstock.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PasswordChangedEvent extends DomainEvent {
    private String username;

    public PasswordChangedEvent(String userId, String username, String serviceName) {
        super(userId, "User", serviceName);
        this.username = username;
    }
}
