package com.l299l.newbedwars.panel.dashboard;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class DashboardStateTest {
    private final AtomicLong now = new AtomicLong(1_700_000_000_000L);
    private final DashboardState state = new DashboardState(now::get);

    private static JsonObject data(int players) {
        JsonObject o = new JsonObject();
        o.addProperty("players", players);
        return o;
    }

    @Test
    void revisionOnlyChangesWithTheData() {
        assertNull(state.latest());
        assertTrue(state.update(data(1)));
        long first = state.latest().revision();
        assertEquals(now.get(), first);
        assertFalse(state.update(data(1)));
        assertEquals(first, state.latest().revision());
        assertTrue(state.update(data(2)));
        assertEquals(first + 1, state.latest().revision());
    }

    @Test
    void pollsKeepTheSnapshotFresh() {
        assertFalse(state.polledRecently());
        assertTrue(state.poll());
        state.update(data(1));
        assertTrue(state.polledRecently());
        now.addAndGet(DashboardState.IDLE_AFTER_MILLIS);
        assertFalse(state.poll());
        now.addAndGet(DashboardState.IDLE_AFTER_MILLIS + 1);
        assertFalse(state.polledRecently());
        assertTrue(state.poll());
    }
}
