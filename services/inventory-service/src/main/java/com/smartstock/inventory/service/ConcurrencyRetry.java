package com.smartstock.inventory.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Retries an operation that lost an optimistic-lock race (debt C-3). The supplied action must
 * run in its <em>own</em> transaction (call a {@code @Transactional} method via the bean's
 * self-proxy), so each attempt re-reads the current row version: a stale stock decrement is
 * re-evaluated against fresh state — succeeding if stock remains, or correctly rejecting an
 * oversell — instead of silently clobbering a concurrent update.
 */
@Slf4j
@Component
public class ConcurrencyRetry {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    public <T> T execute(Supplier<T> action) {
        return execute(DEFAULT_MAX_ATTEMPTS, action);
    }

    public <T> T execute(int maxAttempts, Supplier<T> action) {
        ObjectOptimisticLockingFailureException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (ObjectOptimisticLockingFailureException ex) {
                last = ex;
                log.warn("Optimistic lock conflict (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
            }
        }
        throw last;
    }
}
