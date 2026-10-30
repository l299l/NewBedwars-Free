package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.junit.jupiter.api.Test;

import static com.l299l.newbedwars.npc.NpcTestArenas.arena;
import static org.junit.jupiter.api.Assertions.*;

class JoinNpcActionTest {

    @Test
    void parsesTypeCaseInsensitive() {
        JoinNpcAction a = JoinNpcAction.parse("group", "solo_*");
        assertEquals(JoinNpcAction.Type.GROUP, a.getType());
        assertEquals("solo_*", a.getTarget());
        assertEquals("GROUP solo_*", a.toString());
    }

    @Test
    void unknownType_throwsType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JoinNpcAction.parse("teleport", "x"));
        assertEquals("type", e.getMessage());
        assertNull(JoinNpcAction.tryParse("teleport", "x"));
        assertNull(JoinNpcAction.tryParse(null, null));
    }

    @Test
    void missingTarget_throwsTargetExceptForSelector() {
        for (String type : new String[]{"GROUP", "ARENA", "NETWORK"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JoinNpcAction.parse(type, " "));
            assertEquals("target", e.getMessage());
        }
        JoinNpcAction selector = JoinNpcAction.parse("SELECTOR", null);
        assertNull(selector.getTarget());
        assertEquals("SELECTOR", selector.toString());
    }

    @Test
    void arenaMatchesExactNameIgnoringCase() {
        JoinNpcAction a = JoinNpcAction.parse("ARENA", "Arena1");
        assertTrue(a.matches(arena("arena1", GameStatus.waiting, 2, 1, 0)));
        assertFalse(a.matches(arena("arena10", GameStatus.waiting, 2, 1, 0)));
    }

    @Test
    void groupGlobPattern() {
        JoinNpcAction a = JoinNpcAction.parse("GROUP", "solo_*");
        assertTrue(a.matches(arena("solo_1", GameStatus.waiting, 8, 1, 0)));
        assertTrue(a.matches(arena("SOLO_city", GameStatus.waiting, 8, 1, 0)));
        assertFalse(a.matches(arena("duo_1", GameStatus.waiting, 8, 2, 0)));
        assertFalse(a.matches(arena("xsolo_1", GameStatus.waiting, 8, 1, 0)));
        JoinNpcAction literal = JoinNpcAction.parse("GROUP", "a.b");
        assertFalse(literal.matches(arena("axb", GameStatus.waiting, 2, 1, 0)), "dots are literal");
    }

    @Test
    void groupTeamSize() {
        JoinNpcAction a = JoinNpcAction.parse("GROUP", "4x2");
        assertTrue(a.matches(arena("anything", GameStatus.waiting, 4, 2, 0)));
        assertFalse(a.matches(arena("anything", GameStatus.waiting, 4, 1, 0)));
        assertFalse(a.matches(arena("anything", GameStatus.waiting, 2, 2, 0)));
    }

    @Test
    void selectorWithoutGroupMatchesAll() {
        IArena any = arena("x", GameStatus.playing, 2, 1, 0);
        assertTrue(JoinNpcAction.parse("SELECTOR", null).matches(any));
        assertFalse(JoinNpcAction.parse("SELECTOR", "solo_*").matches(any));
    }

    @Test
    void equalsByTypeAndTarget() {
        assertEquals(JoinNpcAction.parse("group", "a"), JoinNpcAction.parse("GROUP", " a "));
        assertNotEquals(JoinNpcAction.parse("GROUP", "a"), JoinNpcAction.parse("NETWORK", "a"));
    }

    @Test
    void groupNameWinsOverPattern() {
        com.l299l.newbedwars.arena.group.ArenaGroupManager groups = new com.l299l.newbedwars.arena.group.ArenaGroupManager();
        try {
            com.l299l.newbedwars.arena.group.ArenaGroup solo = new com.l299l.newbedwars.arena.group.ArenaGroup("solo", null);
            solo.add("castle");
            groups.register(solo);

            JoinNpcAction a = JoinNpcAction.parse("GROUP", "solo");
            assertTrue(a.matches(arena("Castle", GameStatus.waiting, 4, 1, 0)));
            assertFalse(a.matches(arena("solo", GameStatus.waiting, 4, 1, 0)), "group members only, not the name pattern");
        } finally {
            new com.l299l.newbedwars.arena.group.ArenaGroupManager();
        }
    }
}
