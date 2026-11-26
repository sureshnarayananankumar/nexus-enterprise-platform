package com.nexus.identity.infrastructure.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-key fixed-window limiter (in-memory, per instance). This protects identity-service directly;
 * distributed per-tenant quotas are enforced in the API gateway with Redis (Phase 6).
 */
public class FixedWindowRateLimiter {
    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, long windowMillis, Clock clock) {
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** @return seconds until retry is allowed, or 0 if the request is permitted */
    public long tryAcquire(String key) {
        long now = clock.millis();
        long windowStart = now - (now % windowMillis);
        Window w = windows.compute(key, (k, existing) ->
                existing == null || existing.start != windowStart ? new Window(windowStart) : existing);
        if (w.count.incrementAndGet() <= limit) {
            if (windows.size() > 50_000) evictOld(windowStart);
            return 0;
        }
        return Math.max(1, (windowStart + windowMillis - now + 999) / 1000);
    }

    private void evictOld(long currentStart) {
        windows.entrySet().removeIf(e -> e.getValue().start < currentStart);
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) { this.start = start; }
    }
}
