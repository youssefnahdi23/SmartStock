package com.smartstock.identity.service;

import com.smartstock.common.outbox.OutboxService;
import com.smartstock.identity.domain.event.PasswordChangedEvent;
import com.smartstock.identity.domain.event.UserAuthenticatedEvent;
import com.smartstock.identity.domain.event.UserCreatedEvent;
import com.smartstock.identity.domain.event.UserDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.smartstock.common.event.Topics.IDENTITY_EVENTS;

/**
 * Publishes identity domain events through the transactional outbox so they commit atomically
 * with the user-state change and are relayed to Kafka by the shared OutboxRelay.
 */
@Service
@RequiredArgsConstructor
public class IdentityEventPublisher {

    private final OutboxService outbox;

    public void publishUserCreated(UserCreatedEvent event)           { outbox.append(IDENTITY_EVENTS, event); }

    public void publishUserAuthenticated(UserAuthenticatedEvent event){ outbox.append(IDENTITY_EVENTS, event); }

    public void publishUserDeactivated(UserDeactivatedEvent event)   { outbox.append(IDENTITY_EVENTS, event); }

    public void publishPasswordChanged(PasswordChangedEvent event)   { outbox.append(IDENTITY_EVENTS, event); }
}
