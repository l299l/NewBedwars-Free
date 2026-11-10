package com.l299l.newbedwars.panel.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class LoginLimiter {
    private record Entry(int failures, long firstFailure, long lockedUntil) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMillis;
    private final LongSupplier clock;

    public LoginLimiter(int maxAttempts, long lockMillis, LongSupplier clock) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockMillis = lockMillis;
        this.clock = clock;
    }

    public long lockedFor(String key) {
        Entry entry = entries.get(key);
        if (entry == null) return 0;
        long left = entry.lockedUntil() - clock.getAsLong();
        return Math.max(0, left);
    }

    public void failure(String key) {
        long now = clock.getAsLong();
        entries.compute(key, (k, old) -> {
            if (old == null || (old.lockedUntil() <= now && now - old.firstFailure() > lockMillis)) {
                old = new Entry(0, now, 0);
            }
            if (old.lockedUntil() > now) return old;
            int failures = old.failures() + 1;
            if (failures >= maxAttempts) return new Entry(0, now, now + lockMillis);
            return new Entry(failures, old.failures() == 0 ? now : old.firstFailure(), 0);
        });
    }

    public void success(String key) {
        entries.remove(key);
    }

    public void cleanup() {
        long now = clock.getAsLong();
        entries.values().removeIf(e -> e.lockedUntil() <= now && now - e.firstFailure() > lockMillis);
    }
}
