package com.yep.security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.yep.exception.ApiException;

@Component
public class LoginRateLimiter {

    private static final int LIMIT = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void checkAndRecord(String ipAddress, String email) {
        String key = (ipAddress == null ? "unknown" : ipAddress) + "|" + (email == null ? "" : email.toLowerCase());
        Instant now = Instant.now();
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant cutoff = now.minusSeconds(WINDOW_SECONDS);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= LIMIT) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Too many login attempts. Please wait a minute and try again.");
            }
            timestamps.addLast(now);
        }
    }

    public void clear(String ipAddress, String email) {
        attempts.remove((ipAddress == null ? "unknown" : ipAddress) + "|"
                + (email == null ? "" : email.toLowerCase()));
    }
}
