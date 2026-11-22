package com.l299l.newbedwars.proxy.velocity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyConfigTest {
    private final List<String> warnings = new ArrayList<>();

    private ProxyConfig load(Path dir, String yaml) throws Exception {
        Files.writeString(dir.resolve("config.yml"), yaml);
        return ProxyConfig.load(dir, warnings::add);
    }

    @Test
    void stringBooleansAndNumbersAreAccepted(@TempDir Path dir) throws Exception {
        ProxyConfig c = load(dir, "ticket-timeout-seconds: \"20\"\nmysql:\n  enabled: \"true\"\n  port: \"3307\"\nupdates:\n  check: \"false\"\n");
        assertTrue(c.mysqlEnabled);
        assertEquals(3307, c.mysqlPort);
        assertEquals(20, c.ticketTimeoutSeconds);
        assertFalse(c.updatesCheck);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void unreadableValuesWarnWithTheKeyAndFallBack(@TempDir Path dir) throws Exception {
        ProxyConfig c = load(dir, "state-stale-seconds: soon\nmysql:\n  enabled: maybe\nupdates: 5\ngame-servers: game1\n");
        assertEquals(15, c.stateStaleSeconds);
        assertFalse(c.mysqlEnabled);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("state-stale-seconds")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("mysql.enabled")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("'updates'")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("game-servers")));
    }

    @Test
    void emptyConfigWarnsAndUsesDefaults(@TempDir Path dir) throws Exception {
        ProxyConfig c = load(dir, "");
        assertEquals(10, c.ticketTimeoutSeconds);
        assertEquals(1, warnings.size());
    }

    @Test
    void emptyHostKeepsTheDefault(@TempDir Path dir) throws Exception {
        assertEquals("localhost", load(dir, "mysql:\n  host:\n").mysqlHost);
    }
}
