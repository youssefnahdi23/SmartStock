package com.smartstock.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartstock.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService — transactional append (C-2)")
class OutboxServiceTest {

    @Getter
    @Setter
    @SuperBuilder
    static class SampleEvent extends DomainEvent {
        private String detail;

        SampleEvent(String aggregateId, String detail) {
            super(aggregateId, "Sample", "test-service");
            this.detail = detail;
        }
    }

    @Mock
    OutboxRepository repository;

    // Mirror the Spring-managed mapper the bean receives in production: JSR-310 module
    // registered (DomainEvent carries a LocalDateTime timestamp).
    final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("serializes the event and keys it by aggregate id")
    void appendsSerializedEvent() {
        OutboxService service = new OutboxService(repository, objectMapper);

        service.append("sample.events", new SampleEvent("agg-42", "hello"));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(repository).append(
                org.mockito.ArgumentMatchers.eq("Sample"),
                org.mockito.ArgumentMatchers.eq("agg-42"),
                org.mockito.ArgumentMatchers.eq("sample.events"),
                org.mockito.ArgumentMatchers.eq("agg-42"),
                org.mockito.ArgumentMatchers.eq("SampleEvent"),
                payload.capture());
        assertThat(payload.getValue()).contains("\"detail\":\"hello\"").contains("\"aggregateId\":\"agg-42\"");
    }

    @Test
    @DisplayName("explicit key overrides the derived aggregate key")
    void explicitKeyWins() {
        OutboxService service = new OutboxService(repository, objectMapper);

        service.append("sample.events", "explicit-key", new SampleEvent("agg-1", "x"));

        verify(repository).append(
                org.mockito.ArgumentMatchers.eq("Sample"),
                org.mockito.ArgumentMatchers.eq("agg-1"),
                org.mockito.ArgumentMatchers.eq("sample.events"),
                org.mockito.ArgumentMatchers.eq("explicit-key"),
                org.mockito.ArgumentMatchers.eq("SampleEvent"),
                org.mockito.ArgumentMatchers.anyString());
    }
}
