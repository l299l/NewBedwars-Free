package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.l299l.newbedwars.npc.NpcTestArenas.arena;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class NpcHologramTextTest {

    @Test
    void groupCountsOnlyMatchingEnabledArenas() {
        IArena w1 = arena("solo_1", GameStatus.waiting, 4, 1, 2);
        IArena w2 = arena("solo_2", GameStatus.starting, 4, 1, 4); // full
        IArena p1 = arena("solo_3", GameStatus.playing, 4, 1, 3);
        IArena off = arena("solo_4", GameStatus.waiting, 4, 1, 1);
        when(off.isEnabled()).thenReturn(false);
        IArena other = arena("duo_1", GameStatus.playing, 4, 2, 7);

        Map<String, String> r = NpcHologramText.replacements(JoinNpcAction.parse("GROUP", "solo_*"), List.of(w1, w2, p1, off, other));

        assertEquals("solo_*", r.get("/group/"));
        assertEquals("3", r.get("/playing/"));
        assertEquals("6", r.get("/waiting/"));
        assertEquals("1", r.get("/arenas_free/"));
        assertEquals("solo_1", r.get("/arena/"), "best = fullest arena that still has room");
        assertEquals("2", r.get("/players/"));
        assertEquals("4", r.get("/max/"));
    }

    @Test
    void arenaShowsItsOwnStatus() {
        IArena a = arena("castle", GameStatus.playing, 2, 4, 6);
        Map<String, String> r = NpcHologramText.replacements(JoinNpcAction.parse("ARENA", "castle"), List.of(a));
        assertEquals("castle", r.get("/arena/"));
        assertEquals("6", r.get("/players/"));
        assertEquals("8", r.get("/max/"));
        assertEquals("Playing", r.get("/status/"));

        when(a.isEnabled()).thenReturn(false);
        assertEquals("Disabled", NpcHologramText.replacements(JoinNpcAction.parse("ARENA", "castle"), List.of(a)).get("/status/"));
    }

    @Test
    void missingArenaUsesFallbacks() {
        Map<String, String> r = NpcHologramText.replacements(JoinNpcAction.parse("ARENA", "ghost"), List.of());
        assertEquals("ghost", r.get("/arena/"));
        assertEquals("0", r.get("/players/"));
        assertEquals("-", r.get("/status/"));

        Map<String, String> s = NpcHologramText.replacements(JoinNpcAction.parse("SELECTOR", null), List.of());
        assertEquals("*", s.get("/group/"));
        assertEquals("-", s.get("/arena/"));
        assertEquals("0", s.get("/arenas_free/"));
    }
}
