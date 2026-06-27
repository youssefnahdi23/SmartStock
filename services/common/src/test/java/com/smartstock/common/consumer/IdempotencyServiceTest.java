package com.smartstock.common.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService — at-least-once dedupe (H-3)")
class IdempotencyServiceTest {

    @Mock
    JdbcTemplate jdbc;

    @Test
    @DisplayName("claims an unseen event (insert affects one row)")
    void claimsFirstTime() {
        when(jdbc.update(any(String.class), eq("c"), eq("e1"))).thenReturn(1);
        assertThat(new IdempotencyService(jdbc).claim("c", "e1")).isTrue();
    }

    @Test
    @DisplayName("rejects an already-processed event (ON CONFLICT DO NOTHING affects zero rows)")
    void rejectsDuplicate() {
        when(jdbc.update(any(String.class), eq("c"), eq("e1"))).thenReturn(0);
        assertThat(new IdempotencyService(jdbc).claim("c", "e1")).isFalse();
    }

    @Test
    @DisplayName("passes through when there is no event id to dedupe on")
    void allowsNullEventId() {
        assertThat(new IdempotencyService(jdbc).claim("c", null)).isTrue();
        assertThat(new IdempotencyService(jdbc).claim("c", "  ")).isTrue();
    }
}
