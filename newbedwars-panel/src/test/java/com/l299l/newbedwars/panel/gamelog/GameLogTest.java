package com.l299l.newbedwars.panel.gamelog;

import com.l299l.newbedwars.panel.gamelog.GameLog.Type;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class GameLogTest {
    private final AtomicLong now = new AtomicLong(1000);
    private final GameLog log = new GameLog(now::incrementAndGet);

    private void add(String arena, Type type) {
        log.add(arena, type, "p", null, null, null, null, null);
    }

    @Test
    void pagesAreNewerThanTheGivenSeqAndPerArena() {
        add("Castle", Type.JOIN);
        add("Other", Type.JOIN);
        add("castle", Type.CHAT);
        GameLog.Page all = log.after("CASTLE", 0, 100);
        assertEquals(2, all.entries().size());
        assertEquals(3, all.seq());
        assertFalse(all.truncated());
        assertEquals(Type.CHAT, log.after("castle", 1, 100).entries().get(0).type());
        assertTrue(log.after("nope", 0, 10).entries().isEmpty());
    }

    @Test
    void oldEntriesAreDroppedAndReportedAsTruncated() {
        for (int i = 0; i < GameLog.MAX_ENTRIES + 5; i++) add("a", Type.CHAT);
        GameLog.Page page = log.after("a", 0, GameLog.MAX_ENTRIES + 10);
        assertEquals(GameLog.MAX_ENTRIES, page.entries().size());
        assertTrue(log.after("a", 2, 10_000).truncated());
        GameLog.Page last = log.after("a", 0, 10);
        assertEquals(10, last.entries().size());
        assertTrue(last.truncated());
        assertEquals(page.seq(), last.entries().get(9).seq());
    }

    @Test
    void theFinishedGameStaysUntilTheNextGameStarts() {
        add("a", Type.START);
        add("a", Type.END);
        log.markReset("a");
        add("a", Type.JOIN);
        assertEquals(3, log.after("a", 0, 100).entries().size());
        log.newGame("a");
        assertEquals(1, log.after("a", 0, 100).entries().size());
        log.newGame("a");
        assertEquals(1, log.after("a", 0, 100).entries().size());
        log.clear("a");
        assertTrue(log.after("a", 0, 100).entries().isEmpty());
    }
}
