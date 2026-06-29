package com.smartstock.common.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay — at-least-once drain to Kafka (C-2)")
class OutboxRelayTest {

    @Mock
    OutboxRepository repository;

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRecord record(long id) {
        return new OutboxRecord(id, "sample.events", "key-" + id, "SampleEvent", "{\"x\":1}", 0);
    }

    @Test
    @DisplayName("publishes pending rows and marks them PUBLISHED after broker ack")
    void publishesAndMarks() {
        when(repository.fetchBatch(anyInt())).thenReturn(List.of(record(1), record(2)));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        new OutboxRelay(repository, kafkaTemplate, 100).relay();

        verify(kafkaTemplate).send("sample.events", "key-1", "{\"x\":1}");
        verify(kafkaTemplate).send("sample.events", "key-2", "{\"x\":1}");
        verify(repository).markPublished(1L);
        verify(repository).markPublished(2L);
        verify(repository, never()).recordFailure(anyLong(), anyString());
    }

    @Test
    @DisplayName("on broker failure records the failure and leaves the row PENDING (no false success)")
    void recordsFailureOnError() {
        when(repository.fetchBatch(anyInt())).thenReturn(List.of(record(1)));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        new OutboxRelay(repository, kafkaTemplate, 100).relay();

        verify(repository).recordFailure(eq(1L), anyString());
        verify(repository, never()).markPublished(anyLong());
    }

    @Test
    @DisplayName("no-op when the outbox is empty")
    void noOpWhenEmpty() {
        when(repository.fetchBatch(anyInt())).thenReturn(List.of());

        new OutboxRelay(repository, kafkaTemplate, 100).relay();

        verifyNoInteractions(kafkaTemplate);
    }
}
