package com.smartstock.inventory.unit;

import com.smartstock.inventory.service.ConcurrencyRetry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConcurrencyRetry — retry on optimistic-lock conflict (C-3)")
class ConcurrencyRetryTest {

    private final ConcurrencyRetry retry = new ConcurrencyRetry();

    @Test
    @DisplayName("returns immediately when the action succeeds")
    void succeedsFirstTry() {
        AtomicInteger calls = new AtomicInteger();
        String result = retry.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("retries past transient conflicts and then succeeds")
    void retriesThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        String result = retry.execute(5, () -> {
            if (calls.incrementAndGet() < 3) {
                throw new ObjectOptimisticLockingFailureException("InventoryLevel", "id");
            }
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    @DisplayName("rethrows after exhausting attempts")
    void exhaustsAndThrows() {
        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> retry.execute(3, () -> {
            calls.incrementAndGet();
            throw new ObjectOptimisticLockingFailureException("InventoryLevel", "id");
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(calls).hasValue(3);
    }
}
