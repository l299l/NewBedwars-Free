package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.TablistUpdateEvent;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.apiimpl.edit.ApiEditor;
import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.tablist.TablistConfig;
import com.l299l.newbedwars.tablist.TablistManager;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import com.l299l.newbedwars.tablist.TablistRenderer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApiTablistsTest {
    private static final String YAML = """
            Lobby:
              Enabled: true
              Header: ["lobby"]
            Layouts:
              Default:
                Playing:
                  Header: ["P {arena}"]
                  PlayerFormat: "{player}"
              Tagged:
                Playing:
                  Header: ["T {tag}"]
                  PlayerFormat: "{player}{tag}"
              Zeta:
                Playing:
                  Header: ["Z"]
              Alpha:
                Playing:
                  Header: ["A"]
            """;

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ArenaRegistry> registry;
    private PluginManager pm;
    private final List<Consumer<TablistUpdateEvent>> listeners = new ArrayList<>();
    private int events;
    private String savedDefault;
    private boolean savedEnabled;
    private ApiRegistrations registrations;
    private ApiTablists api;
    private TablistManager manager;
    private Player player;
    private World world;
    private IArena arena;
    private NewBedwars plugin;

    @BeforeEach
    void setUp() {
        savedDefault = Properties.DefaultTablist;
        savedEnabled = Properties.TablistEnabled;
        Properties.DefaultTablist = "Default";
        Properties.TablistEnabled = true;
        AddonPlaceholders.clear();
        bukkit = mockStatic(Bukkit.class);
        registry = mockStatic(ArenaRegistry.class);
        pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        bukkit.when(Bukkit::getScheduler).thenReturn(mock(org.bukkit.scheduler.BukkitScheduler.class));
        doAnswer(inv -> {
            events++;
            Event e = inv.getArgument(0);
            if (e instanceof TablistUpdateEvent t) listeners.forEach(l -> l.accept(t));
            return null;
        }).when(pm).callEvent(any(Event.class));
        plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        manager = new TablistManager(TablistConfig.parse(YamlConfiguration.loadConfiguration(new java.io.StringReader(YAML)), null), null);
        when(plugin.getTablistManager()).thenReturn(manager);
        registrations = new ApiRegistrations();
        api = new ApiTablists(registrations);

        UUID id = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Steve");
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        when(arena.getPlayers()).thenReturn(Set.of(id));
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.status()).thenReturn(GameStatus.playing);
        when(arena.getArenaWorld()).thenReturn(world);
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
        registry.when(() -> ArenaRegistry.byName("castle")).thenReturn(arena);
        bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        AddonPlaceholders.clear();
        Properties.DefaultTablist = savedDefault;
        Properties.TablistEnabled = savedEnabled;
        NewBedwars.plugin = null;
        registry.close();
        bukkit.close();
    }

    private Plugin owner(String name) {
        Plugin owner = mock(Plugin.class);
        when(owner.getName()).thenReturn(name);
        return owner;
    }

    @Test
    void serviceReportsConfig() {
        assertTrue(api.isEnabled());
        assertTrue(api.isLobbyEnabled());
        assertEquals(List.of("Alpha", "Default", "Tagged", "Zeta"), api.getLayouts());
        assertEquals("Default", api.getDefaultLayout());
        Properties.TablistEnabled = false;
        assertFalse(api.isEnabled());
    }

    @Test
    void serviceWithoutManagerIsEmpty() {
        when(plugin.getTablistManager()).thenReturn(null);
        assertEquals(List.of(), api.getLayouts());
        assertFalse(api.isLobbyEnabled());
        assertEquals("Default", api.getEffectiveLayout(null));
    }

    @Test
    void effectiveLayoutPrefersArenaThenDefaultThenFallback() {
        ArenaView view = new ApiArena(arena);
        assertEquals("Default", api.getEffectiveLayout(view));
        assertEquals("Default", api.getEffectiveLayout(null));
        when(arena.getTablistId()).thenReturn("zeta");
        assertEquals("Zeta", api.getEffectiveLayout(new ApiArena(arena)));
        when(arena.getTablistId()).thenReturn("Nope");
        assertEquals("Default", api.getEffectiveLayout(new ApiArena(arena)));
        when(arena.getTablistId()).thenReturn(null);
        Properties.DefaultTablist = "Alpha";
        assertEquals("Alpha", api.getEffectiveLayout(new ApiArena(arena)));
    }

    @Test
    void viewExposesArenaTablist() {
        assertNull(new ApiArena(arena).getTablist());
        when(arena.getTablistId()).thenReturn("Alpha");
        assertEquals("Alpha", new ApiArena(arena).getTablist());
    }

    @Test
    void editorSetsAndClearsTablist() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        ApiEditor editor = new ApiEditor(arena);
        editor.setTablist("zeta");
        verify(arena).setTablistId("Zeta");
        editor.setTablist(null);
        verify(arena).setTablistId(null);
        assertThrows(IllegalArgumentException.class, () -> editor.setTablist("Nope"));
        assertThrows(IllegalArgumentException.class, () -> editor.setTablist(""));
        verify(arena, times(2)).setTablistId(any());
    }

    @Test
    void editorRefreshesPlayersOfTheArena() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        manager.update(player);
        int before = events;
        when(arena.getTablistId()).thenReturn("Zeta");
        new ApiEditor(arena).setTablist("Zeta");
        assertEquals(before + 1, events);
        verify(player).setPlayerListHeaderFooter("Z", "");
    }

    @Test
    void editorRejectsRunningArena() {
        when(arena.status()).thenReturn(GameStatus.waiting);
        ApiEditor editor = new ApiEditor(arena);
        when(arena.status()).thenReturn(GameStatus.playing);
        assertThrows(IllegalStateException.class, () -> editor.setTablist("Zeta"));
    }

    @Test
    void placeholderNamesAreValidated() {
        Plugin o = owner("Addon");
        for (String bad : new String[]{null, "", "a b", "x".repeat(33), "a-b", "{tag}", "ä"}) {
            assertThrows(IllegalArgumentException.class, () -> api.registerPlaceholder(o, bad, (p, a) -> "x"), String.valueOf(bad));
        }
        for (String shadow : new String[]{"player", "arena", "TeamColor", "bedsBroken", "server"}) {
            assertThrows(IllegalArgumentException.class, () -> api.registerPlaceholder(o, shadow, (p, a) -> "x"), shadow);
        }
        assertThrows(IllegalArgumentException.class, () -> api.registerPlaceholder(o, "ok", null));
        api.registerPlaceholder(o, "tag", (p, a) -> "x");
        assertThrows(IllegalArgumentException.class, () -> api.registerPlaceholder(o, "tag", (p, a) -> "y"));
    }

    @Test
    void placeholderRendersWithViewerAndArena() {
        List<Object> seen = new ArrayList<>();
        api.registerPlaceholder(owner("Addon"), "tag", (p, a) -> {
            seen.add(p);
            seen.add(a == null ? null : a.getName());
            return "[T]";
        });
        assertEquals("Steve [T]", TablistRenderer.render("{player} {tag}", player, arena, player));
        assertEquals(List.of(player, "castle"), seen);
        assertEquals("[T]", TablistRenderer.render("{tag}", player, null, player));
        assertNull(seen.get(3));
        assertEquals("{unknown}", TablistRenderer.render("{unknown}", player, arena, player));
    }

    @Test
    void placeholderIsUsedInLayoutsAndNullIsEmpty() {
        api.registerPlaceholder(owner("Addon"), "tag", (p, a) -> null);
        when(arena.getTablistId()).thenReturn("Tagged");
        manager.update(player);
        verify(player).setPlayerListHeaderFooter("T ", "");
        verify(player).setPlayerListName("Steve");
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
            api.registerPlaceholder(owner("Addon"), "boom", (p, a) -> {
                throw new IllegalStateException("bad");
            });
            api.registerPlaceholder(owner("Addon"), "fine", (p, a) -> "ok");
            assertEquals("a--ok", TablistRenderer.render("a-{boom}-{fine}", player, arena, player));
            assertEquals("", TablistRenderer.render("{boom}", player, arena, player));
        } finally {
            log.removeHandler(handler);
        }
        assertEquals(1, records.size());
    }

    @Test
    void unregisterAllAndDisableRemovePlaceholders() {
        Plugin a = owner("A");
        Plugin b = owner("B");
        api.registerPlaceholder(a, "one", (p, x) -> "1");
        api.registerPlaceholder(b, "two", (p, x) -> "2");
        assertEquals("1 2", TablistRenderer.render("{one} {two}", player, arena, player));
        registrations.releaseAll(a);
        assertEquals("{one} 2", TablistRenderer.render("{one} {two}", player, arena, player));
        api.registerPlaceholder(a, "one", (p, x) -> "again");
        assertEquals("again", TablistRenderer.render("{one}", player, arena, player));
        registrations.releaseAll(a);
        registrations.releaseAll(b);
        assertEquals("{one} {two}", TablistRenderer.render("{one} {two}", player, arena, player));
    }

    @Test
    void playerLayoutBeatsArenaAndDefault() {
        when(arena.getTablistId()).thenReturn("Zeta");
        manager.update(player);
        verify(player).setPlayerListHeaderFooter("Z", "");
        api.setPlayerLayout(player, "alpha");
        verify(player).setPlayerListHeaderFooter("A", "");
        api.clearPlayerLayout(player);
        verify(player, times(2)).setPlayerListHeaderFooter("Z", "");
    }

    @Test
    void unknownPlayerLayoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerLayout(player, "Nope"));
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerLayout(player, null));
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerLayout(null, "Zeta"));
        verify(player, never()).setPlayerListHeaderFooter(anyString(), anyString());
    }

    @Test
    void playerLayoutClearsWhenLeavingTheArena() {
        api.setPlayerLayout(player, "Alpha");
        verify(player).setPlayerListHeaderFooter("A", "");
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
        manager.update(player);
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
        manager.update(player);
        verify(player).setPlayerListHeaderFooter("P castle", "");
    }

    @Test
    void playerLayoutClearsOnQuitAndWhenArenaChanges() {
        api.setPlayerLayout(player, "Alpha");
        manager.forget(player);
        manager.refresh(player);
        verify(player).setPlayerListHeaderFooter("P castle", "");

        api.setPlayerLayout(player, "Alpha");
        IArena other = mock(IArena.class);
        when(other.getArenaName()).thenReturn("other");
        UUID playerId = player.getUniqueId();
        when(other.getPlayers()).thenReturn(Set.of(playerId));
        when(other.getSpectators()).thenReturn(new ArrayList<>());
        when(other.status()).thenReturn(GameStatus.playing);
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(other);
        manager.update(player);
        verify(player).setPlayerListHeaderFooter("P other", "");
    }

    @Test
    void playerLayoutSurvivesReload() {
        api.setPlayerLayout(player, "Alpha");
        TablistManager reloaded = new TablistManager(TablistConfig.parse(YamlConfiguration.loadConfiguration(new java.io.StringReader(YAML)), null), null);
        reloaded.inheritOverrides(manager);
        reloaded.update(player);
        verify(player, times(2)).setPlayerListHeaderFooter("A", "");
    }

    @Test
    void refreshForcesRenderAndArenaRefreshReachesPlayers() {
        manager.update(player);
        manager.update(player);
        assertEquals(1, events);
        api.refresh(player);
        assertEquals(2, events);
        verify(player, times(1)).setPlayerListHeaderFooter("P castle", "");
        api.refresh(new ApiArena(arena));
        assertEquals(3, events);
        api.refresh((ArenaView) null);
        api.refresh((Player) null);
    }

    @Test
    void eventListenerChangesWhatIsSentWithoutResendLoop() {
        listeners.add(e -> {
            e.setHeader(e.getHeader() + "!");
            e.setFooter("foot");
            e.setPlayerListName("custom");
        });
        for (int i = 0; i < 5; i++) manager.update(player);
        verify(player, times(1)).setPlayerListHeaderFooter("P castle!", "foot");
        verify(player, times(1)).setPlayerListName("custom");
        assertEquals(1, events);
    }

    @Test
    void eventCarriesContext() {
        List<TablistUpdateEvent> seen = new ArrayList<>();
        listeners.add(seen::add);
        manager.update(player);
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
        manager.update(player);
        assertEquals(2, seen.size());
        TablistUpdateEvent game = seen.get(0);
        assertSame(player, game.getPlayer());
        assertEquals("castle", game.getArena().orElseThrow().getName());
        assertEquals("Default", game.getLayout());
        assertEquals("P castle", game.getHeader());
        assertEquals("", game.getFooter());
        assertEquals("Steve", game.getPlayerListName());
        TablistUpdateEvent lobby = seen.get(1);
        assertTrue(lobby.getArena().isEmpty());
        assertNull(lobby.getLayout());
        assertEquals("lobby", lobby.getHeader());
        assertNull(lobby.getPlayerListName());
    }

    @Test
    void cancelledEventSendsNothingAndDoesNotRefireUntilChange() {
        listeners.add(e -> e.setCancelled(true));
        manager.update(player);
        manager.update(player);
        manager.update(player);
        assertEquals(1, events);
        verify(player, never()).setPlayerListName(any());
        verify(player, never()).setPlayerListHeaderFooter(anyString(), anyString());
        listeners.clear();
        api.refresh(player);
        verify(player).setPlayerListName("Steve");
    }

    @Test
    void unchangedTablistFiresNoEvent() {
        manager.update(player);
        for (int i = 0; i < 10; i++) manager.update(player);
        assertEquals(1, events);
    }
}
