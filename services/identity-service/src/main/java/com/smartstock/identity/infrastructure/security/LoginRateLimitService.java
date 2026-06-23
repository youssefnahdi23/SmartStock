package com.smartstock.identity.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.smartstock.identity.infrastructure.config.SecurityProperties;

@Service
public class LoginRateLimitService {

    private final Map<String, Deque<Instant>> requestWindows = new ConcurrentHashMap<>();
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public LoginRateLimitService(SecurityProperties securityProperties, Clock clock) {
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    public boolean isAllowed(String key) {
        Deque<Instant> requests = requestWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant now = clock.instant();
        Instant threshold = now.minus(securityProperties.getRateLimit().getLoginWindow());
        synchronized (requests) {
            while (!requests.isEmpty() && requests.peekFirst().isBefore(threshold)) {
                requests.pollFirst();
            }
            if (requests.size() >= securityProperties.getRateLimit().getLoginMaxRequests()) {
                return false;
            }
            requests.addLast(now);
            return true;
        }
    }
}
