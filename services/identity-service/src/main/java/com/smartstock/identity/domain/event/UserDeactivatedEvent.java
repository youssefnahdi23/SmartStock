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
public class UserDeactivatedEvent extends DomainEvent {
    private String username;
    private String deactivatedBy;

    public UserDeactivatedEvent(String userId, String username, String deactivatedBy, String serviceName) {
        super(userId, "User", serviceName);
        this.username = username;
        this.deactivatedBy = deactivatedBy;
    }
}
