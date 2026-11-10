package com.l299l.newbedwars.panel.auth;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final SessionManager sessions = new SessionManager(30_000, 100_000, now::get);

    @Test
    void idleSessionsExpireAndUseKeepsThemAlive() {
        SessionManager.Session s = sessions.create("Leon");
        assertEquals("leon", s.user());
        assertNotEquals(s.id(), s.csrf());
        now.addAndGet(20_000);
        assertTrue(sessions.get(s.id()).isPresent());
        now.addAndGet(20_000);
        assertTrue(sessions.get(s.id()).isPresent());
        now.addAndGet(30_001);
        assertTrue(sessions.get(s.id()).isEmpty());
        assertTrue(sessions.get(s.id()).isEmpty());
    }

    @Test
    void sessionsHaveAnAbsoluteLimit() {
        SessionManager.Session s = sessions.create("leon");
        for (int i = 0; i < 4; i++) {
            now.addAndGet(25_000);
            assertTrue(sessions.get(s.id()).isPresent());
        }
        now.addAndGet(1);
        assertTrue(sessions.get(s.id()).isEmpty());
    }

    @Test
    void removeUserCanKeepTheCurrentSession() {
        SessionManager.Session a = sessions.create("leon");
        SessionManager.Session b = sessions.create("LEON");
        SessionManager.Session other = sessions.create("staff1");
        sessions.removeUser("Leon", a.id());
        assertTrue(sessions.get(a.id()).isPresent());
        assertTrue(sessions.get(b.id()).isEmpty());
        assertTrue(sessions.get(other.id()).isPresent());
        sessions.remove(a.id());
        assertEquals(1, sessions.count());
        assertTrue(sessions.get(null).isEmpty());
        assertTrue(sessions.get("missing").isEmpty());
    }
}
