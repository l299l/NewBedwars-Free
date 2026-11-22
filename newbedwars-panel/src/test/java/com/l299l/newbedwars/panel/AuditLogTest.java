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

    @Test
    void controlCharactersNeverReachTheConsole() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        List<String> lines = new java.util.ArrayList<>();
        logger.addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                lines.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        AuditLog log = new AuditLog(dir.resolve("audit.log"), logger, () -> 1);
        log.record("evil\r\n[Server] fake", "1.2.3.4\n", "login", "t\u0000\u001b[31mx\u2028y", "ok");
        assertEquals(1, lines.size());
        assertEquals("[Audit] evil[Server] fake (1.2.3.4) login t[31mxy -> ok", lines.get(0));
        assertEquals("evil\r\n[Server] fake", log.recent(1).get(0).user());
        assertNull(AuditLog.clean(null));
        assertEquals("plain", AuditLog.clean("plain"));
    }

    @Test
    void aGrowingLogIsRotatedWhileRunning() throws Exception {
        Path file = dir.resolve("audit.log");
        AuditLog log = new AuditLog(file, Logger.getAnonymousLogger(), () -> 1);
        log.record("u", "ip", "first", null, "ok");
        Files.write(file, new byte[5 * 1024 * 1024 + 1], java.nio.file.StandardOpenOption.APPEND);
        log.record("u", "ip", "second", null, "ok");
        Path rotated = dir.resolve("audit.log.1");
        assertTrue(Files.isRegularFile(rotated));
        assertTrue(Files.size(file) < 1024);
        assertTrue(Files.readString(file).contains("second"));
        assertEquals(2, log.recent(10).size());
    }
}
