package com.l299l.newbedwars.panel.auth;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class LoginLimiterTest {
    private final AtomicLong now = new AtomicLong(5_000_000);
    private final LoginLimiter limiter = new LoginLimiter(5, 600_000, now::get);

    @Test
    void locksAfterTheLimitForTheLockTime() {
        for (int i = 0; i < 4; i++) {
            limiter.failure("user:leon");
            assertEquals(0, limiter.lockedFor("user:leon"));
        }
        limiter.failure("user:leon");
        assertEquals(600_000, limiter.lockedFor("user:leon"));
        limiter.failure("user:leon");
        now.addAndGet(300_000);
        assertEquals(300_000, limiter.lockedFor("user:leon"));
        assertEquals(0, limiter.lockedFor("user:other"));
        now.addAndGet(300_000);
        assertEquals(0, limiter.lockedFor("user:leon"));
        limiter.failure("user:leon");
        assertEquals(0, limiter.lockedFor("user:leon"));
    }

    @Test
    void successAndOldFailuresAreForgotten() {
        for (int i = 0; i < 4; i++) limiter.failure("ip:1.2.3.4");
        limiter.success("ip:1.2.3.4");
        limiter.failure("ip:1.2.3.4");
        assertEquals(0, limiter.lockedFor("ip:1.2.3.4"));

        for (int i = 0; i < 3; i++) limiter.failure("user:x");
        now.addAndGet(600_001);
        for (int i = 0; i < 4; i++) limiter.failure("user:x");
        assertEquals(0, limiter.lockedFor("user:x"));
        limiter.failure("user:x");
        assertTrue(limiter.lockedFor("user:x") > 0);
        limiter.cleanup();
        assertTrue(limiter.lockedFor("user:x") > 0);
    }
}
