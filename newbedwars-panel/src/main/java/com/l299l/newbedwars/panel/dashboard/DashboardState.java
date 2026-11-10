package com.l299l.newbedwars.panel.dashboard;

import com.google.gson.JsonObject;

import java.util.function.LongSupplier;

public final class DashboardState {
    public static final long IDLE_AFTER_MILLIS = 10_000;

    public record Snapshot(long revision, JsonObject data) {}

    private final LongSupplier clock;
    private volatile Snapshot latest;
    private volatile String latestJson;
    private volatile long lastPoll;

    public DashboardState(LongSupplier clock) {
        this.clock = clock;
        this.lastPoll = Long.MIN_VALUE / 2;
    }

    public synchronized boolean update(JsonObject data) {
        String json = data.toString();
        if (json.equals(latestJson)) return false;
        long revision = latest == null ? clock.getAsLong() : latest.revision() + 1;
        latest = new Snapshot(revision, data);
        latestJson = json;
        return true;
    }

    public boolean polledRecently() {
        return clock.getAsLong() - lastPoll <= IDLE_AFTER_MILLIS;
    }

    public boolean poll() {
        boolean stale = latest == null || !polledRecently();
        lastPoll = clock.getAsLong();
        return stale;
    }

    public Snapshot latest() {
        return latest;
    }
}
