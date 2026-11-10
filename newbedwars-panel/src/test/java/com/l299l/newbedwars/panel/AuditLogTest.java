package com.l299l.newbedwars.panel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {
    @TempDir
    Path dir;

    @Test
    void recordsSurviveARestartNewestFirst() throws Exception {
        AtomicLong now = new AtomicLong(1000);
        Path file = dir.resolve("panel/audit.log");
        AuditLog log = new AuditLog(file, Logger.getAnonymousLogger(), now::getAndIncrement);
        log.record("boss", "10.0.0.1", "login", null, "ok");
        log.record("boss", "10.0.0.1", "arena.backup", "castle", "OK");
        Files.writeString(file, "not json\n", java.nio.file.StandardOpenOption.APPEND);
        log.record(null, "10.0.0.2", "login", "ghost", "failed");

        AuditLog again = new AuditLog(file, Logger.getAnonymousLogger(), now::get);
        List<AuditLog.Entry> recent = again.recent(10);
        assertEquals(3, recent.size());
        assertEquals(new AuditLog.Entry(1002, null, "10.0.0.2", "login", "ghost", "failed"), recent.get(0));
        assertEquals("arena.backup", recent.get(1).action());
        assertEquals("castle", recent.get(1).target());
        assertEquals(1, again.recent(1).size());
    }

    @Test
    void keepsOnlyTheLastEntriesInMemory() {
        AuditLog log = new AuditLog(dir.resolve("audit.log"), Logger.getAnonymousLogger(), () -> 1);
        for (int i = 0; i < AuditLog.KEEP_IN_MEMORY + 20; i++) log.record("u", "ip", "a" + i, null, "ok");
        List<AuditLog.Entry> recent = log.recent(10_000);
        assertEquals(AuditLog.KEEP_IN_MEMORY, recent.size());
        assertEquals("a" + (AuditLog.KEEP_IN_MEMORY + 19), recent.get(0).action());
    }
}
