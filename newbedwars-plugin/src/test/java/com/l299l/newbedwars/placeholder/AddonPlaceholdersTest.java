package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.apiimpl.ApiPlaceholders;
import com.l299l.newbedwars.apiimpl.ApiTablists;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddonPlaceholdersTest {
    private ApiRegistrations registrations;
    private ApiPlaceholders api;
    private Player player;

    @BeforeEach
    void setUp() {
        AddonPlaceholders.clear();
        registrations = new ApiRegistrations();
        api = new ApiPlaceholders(registrations);
        player = mock(Player.class);
    }

    @AfterEach
    void tearDown() {
        AddonPlaceholders.clear();
    }

    private Plugin owner(String name) {
        Plugin owner = mock(Plugin.class);
        when(owner.getName()).thenReturn(name);
        return owner;
    }

    @Test
    void namesAreValidated() {
        Plugin o = owner("Addon");
        for (String bad : new String[]{null, "", "a b", "x".repeat(33), "a-b", "{tag}", "/tag/", "ä"}) {
            assertThrows(IllegalArgumentException.class, () -> api.register(o, bad, (p, a) -> "x"), String.valueOf(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> api.register(o, "ok", null));
        assertThrows(IllegalArgumentException.class, () -> api.register(null, "ok", (p, a) -> "x"));
        api.register(o, "tag", (p, a) -> "x");
        assertThrows(IllegalArgumentException.class, () -> api.register(o, "tag", (p, a) -> "y"));
    }

    @Test
    void builtInsOfAllThreeSystemsCannotBeShadowed() {
        Plugin o = owner("Addon");
        String[] tablist = {"player", "arena", "TeamColor", "bedsBroken", "server", "online"};
        String[] scoreboard = {"datetime", "date", "servername", "map", "status", "teams", "bed", "yourteam", "time", "gameTime", "PHASETIME"};
        String[] bossBar = {"server_name", "arena_name", "max_players", "players", "phase"};
        for (String[] group : new String[][]{tablist, scoreboard, bossBar}) {
            for (String name : group) {
                assertThrows(IllegalArgumentException.class, () -> api.register(o, name, (p, a) -> "x"), name);
            }
        }
    }

    @Test
    void delimitersSelectTheSystem() {
        api.register(owner("Addon"), "tag", (p, a) -> "[T]");
        assertEquals("a [T] {tag}", AddonPlaceholders.apply("a /tag/ {tag}", '/', '/', player, null));
        assertEquals("/tag/ b [T]", AddonPlaceholders.apply("/tag/ b {tag}", '{', '}', player, null));
        assertEquals("plain", AddonPlaceholders.apply("plain", '/', '/', player, null));
    }

    @Test
    void passesViewerAndArenaAndNullViewer() {
        List<Object> seen = new ArrayList<>();
        api.register(owner("Addon"), "tag", (p, a) -> {
            seen.add(p);
            seen.add(a == null ? null : a.getName());
            return null;
        });
        IArena arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        assertEquals("", AddonPlaceholders.apply("/tag/", '/', '/', null, arena));
        assertNull(seen.get(0));
        assertEquals("castle", seen.get(1));
    }

    @Test
    void thrownPlaceholderRendersEmptyAndLogsOnce() {
        java.util.logging.Logger log = java.util.logging.Logger.getLogger("NewBedwars");
        List<java.util.logging.LogRecord> records = new ArrayList<>();
        java.util.logging.Handler handler = new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord r) {
                records.add(r);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        log.addHandler(handler);
        try {
            api.register(owner("Addon"), "boom", (p, a) -> {
                throw new IllegalStateException("bad");
            });
            api.register(owner("Addon"), "fine", (p, a) -> "ok");
            assertEquals("a--ok", AddonPlaceholders.apply("a-/boom/-/fine/", '/', '/', player, null));
            assertEquals("", AddonPlaceholders.apply("/boom/", '/', '/', player, null));
        } finally {
            log.removeHandler(handler);
        }
        assertEquals(1, records.size());
    }

    @Test
    void unregisterAllRemovesOnlyTheOwnersPlaceholders() {
        Plugin a = owner("A");
        Plugin b = owner("B");
        api.register(a, "one", (p, x) -> "1");
        api.register(b, "two", (p, x) -> "2");
        assertEquals("1 2", AddonPlaceholders.apply("/one/ /two/", '/', '/', player, null));
        registrations.releaseAll(a);
        assertEquals("/one/ 2", AddonPlaceholders.apply("/one/ /two/", '/', '/', player, null));
        api.register(a, "one", (p, x) -> "again");
        assertEquals("again", AddonPlaceholders.apply("/one/", '/', '/', player, null));
    }

    @Test
    void tablistRegistrationIsTheSameRegistry() {
        Plugin o = owner("Addon");
        ApiTablists tablists = new ApiTablists(registrations);
        tablists.registerPlaceholder(o, "rank", (p, a) -> "VIP");
        assertEquals("VIP", AddonPlaceholders.apply("{rank}", '{', '}', player, null));
        assertEquals("VIP", AddonPlaceholders.apply("/rank/", '/', '/', player, null));
        assertThrows(IllegalArgumentException.class, () -> api.register(o, "rank", (p, a) -> "x"));
        assertThrows(IllegalArgumentException.class, () -> tablists.registerPlaceholder(o, "rank2", null));
        assertThrows(IllegalArgumentException.class, () -> tablists.registerPlaceholder(o, "player", (p, a) -> "x"));
        registrations.releaseAll(o);
        assertTrue(AddonPlaceholders.isEmpty());
    }
}
