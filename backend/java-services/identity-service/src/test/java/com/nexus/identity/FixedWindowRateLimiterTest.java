package com.nexus.identity;

import com.nexus.identity.infrastructure.security.FixedWindowRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowRateLimiterTest {

    private final AtomicLong millis = new AtomicLong(1_800_000_000_000L - (1_800_000_000_000L % 60_000));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        @Override public long millis() { return millis.get(); }
    };

    @Test
    void allowsUpToLimitThenBlocksUntilNextWindow() {
        var limiter = new FixedWindowRateLimiter(3, 60_000, clock);
        assertThat(limiter.tryAcquire("ip")).isZero();
        assertThat(limiter.tryAcquire("ip")).isZero();
        assertThat(limiter.tryAcquire("ip")).isZero();
        assertThat(limiter.tryAcquire("ip")).isEqualTo(60);

        millis.addAndGet(30_000);
        assertThat(limiter.tryAcquire("ip")).isEqualTo(30);

        millis.addAndGet(30_000);
        assertThat(limiter.tryAcquire("ip")).isZero();
    }

    @Test
    void keysAreIndependent() {
        var limiter = new FixedWindowRateLimiter(1, 60_000, clock);
        assertThat(limiter.tryAcquire("a")).isZero();
        assertThat(limiter.tryAcquire("b")).isZero();
        assertThat(limiter.tryAcquire("a")).isPositive();
    }
}
