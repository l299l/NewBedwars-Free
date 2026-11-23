package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.events.ScoreboardUpdateEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import com.l299l.newbedwars.scoreboard.NScoreboard;
import com.l299l.newbedwars.scoreboard.ScoreboardOverrides;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApiScoreboardsTest {
    private static final String YAML = """
            Scoreboards:
              Waiting:
                Enabled: true
                Title: "&6Wait"
                Lines:
                  - "Map /map/"
                  - ""
                  - "/tag/"
              Playing:
                Enabled: true
                Title: "&cPlay"
                Lines:
                  - "Go"
              Disabled:
                Enabled: false
                Title: "x"
                Lines: []
            """;

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ArenaRegistry> registry;
    private final List<Consumer<ScoreboardUpdateEvent>> listeners = new ArrayList<>();
    private int events;
    private String savedName;
    private com.l299l.newbedwars.scoreboard.ScoreboardManager manager;
    private ApiScoreboards api;
    private Player player;
    private World world;
    private IArena arena;
    private Scoreboard board;
    private Objective objective;
    private Score score;
    private String mapName = "castle";

    @BeforeEach
    void setUp() {
        savedName = Properties.ServerName;
        Properties.ServerName = "S";
        AddonPlaceholders.clear();
        ScoreboardOverrides.clearAll();
        bukkit = mockStatic(Bukkit.class);
        registry = mockStatic(ArenaRegistry.class);
        PluginManager pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(BukkitTask.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        doAnswer(inv -> {
            events++;
            Event e = inv.getArgument(0);
            if (e instanceof ScoreboardUpdateEvent s) listeners.forEach(l -> l.accept(s));
            return null;
        }).when(pm).callEvent(any(Event.class));
        board = mock(Scoreboard.class);
        objective = mock(Objective.class);
        score = mock(Score.class);
        when(board.getObjective(anyString())).thenReturn(objective);
        when(objective.getScore(anyString())).thenReturn(score);
        org.bukkit.scoreboard.ScoreboardManager sm = mock(org.bukkit.scoreboard.ScoreboardManager.class);
        when(sm.getNewScoreboard()).thenReturn(board);
        when(sm.getMainScoreboard()).thenReturn(mock(Scoreboard.class));
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(sm);

        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        Messages messages = mock(Messages.class);
        when(messages.getMsg(any(Player.class), anyString())).thenReturn("m");
        when(plugin.getMessages()).thenReturn(messages);
        manager = new com.l299l.newbedwars.scoreboard.ScoreboardManager(YamlConfiguration.loadConfiguration(new java.io.StringReader(YAML)));
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        manager.loadScoreboards();
        when(plugin.getScoreboardManager()).thenReturn(manager);
        api = new ApiScoreboards();

        UUID id = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Steve");
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenAnswer(i -> mapName);
        when(arena.getPlayers()).thenReturn(Set.of(id));
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.status()).thenReturn(GameStatus.waiting);
        when(arena.getNextGamePhase()).thenReturn("");
        when(arena.getCurrentGamePhase()).thenReturn("");
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
    }

    @AfterEach
    void tearDown() {
        AddonPlaceholders.clear();
        ScoreboardOverrides.clearAll();
        Properties.ServerName = savedName;
        NewBedwars.plugin = null;
        registry.close();
        bukkit.close();
    }

    private NScoreboard create(String template) {
        return manager.createPlayerScoreboard(template, arena, player);
    }

    @Test
    void serviceListsAndReportsEnabled() {
        assertEquals(List.of("Disabled", "Playing", "Waiting"), api.getScoreboards());
        assertTrue(api.isEnabled("Waiting"));
        assertFalse(api.isEnabled("Disabled"));
        assertFalse(api.isEnabled("Nope"));
        assertFalse(api.isEnabled(null));
    }

    @Test
    void unknownIdIsRejectedAndCaseIsIgnored() {
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerScoreboard(player, "Nope"));
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerScoreboard(player, null));
        assertThrows(IllegalArgumentException.class, () -> api.setPlayerScoreboard(null, "Waiting"));
        api.setPlayerScoreboard(player, "playing");
        assertEquals("Playing", manager.effectiveTemplate("Waiting", arena, player));
        verify(arena).refreshScoreboard(player);
    }

    @Test
    void overrideBeatsTheArenaTemplateUntilCleared() {
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", arena, player));
        api.setPlayerScoreboard(player, "Playing");
        assertEquals("Playing", manager.effectiveTemplate("Waiting", arena, player));
        assertEquals("Playing", manager.effectiveTemplate("Ending", arena, player));
        api.clearPlayerScoreboard(player);
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", arena, player));
        verify(arena, times(2)).refreshScoreboard(player);
    }

    @Test
    void overrideClearsWhenArenaChangesOrPlayerQuits() {
        api.setPlayerScoreboard(player, "Playing");
        IArena other = mock(IArena.class);
        when(other.getArenaName()).thenReturn("other");
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", other, player));
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", arena, player));

        api.setPlayerScoreboard(player, "Playing");
        api.hide(player);
        ScoreboardOverrides.forget(player);
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", arena, player));
    }

    @Test
    void hideAndShow() {
        api.hide(player);
        assertNull(manager.effectiveTemplate("Waiting", arena, player));
        api.show(player);
        assertEquals("Waiting", manager.effectiveTemplate("Waiting", arena, player));
        verify(arena, times(2)).refreshScoreboard(player);
        api.show(player);
        verify(arena, times(2)).refreshScoreboard(player);
    }

    @Test
    void currentScoreboardComesFromTheArenaAndIsNullOutside() {
        when(arena.getCurrentScoreboardId(player)).thenReturn("Waiting");
        assertEquals("Waiting", api.getCurrentScoreboard(player));
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
        assertNull(api.getCurrentScoreboard(player));
        assertNull(api.getCurrentScoreboard(null));
        api.refresh(player);
        verify(arena, never()).refreshScoreboard(player);
    }

    @Test
    void creationRendersAndFiresOneEvent() {
        AddonPlaceholders.register("tag", (p, a) -> "TAG");
        List<ScoreboardUpdateEvent> seen = new ArrayList<>();
        listeners.add(seen::add);
        NScoreboard sb = create("Waiting");
        assertNotNull(sb);
        assertEquals(1, events);
        ScoreboardUpdateEvent e = seen.get(0);
        assertEquals("Waiting", e.getScoreboard());
        assertSame(player, e.getPlayer());
        assertEquals("castle", e.getArena().orElseThrow().getName());
        assertEquals("§6Wait", e.getTitle());
        assertEquals(List.of("Map castle", " ", "TAG"), e.getLines());
        verify(objective).getScore("Map castle");
        verify(player).setScoreboard(board);
    }

    @Test
    void unchangedContentFiresNoEventAndNoResend() {
        NScoreboard sb = create("Waiting");
        for (int i = 0; i < 10; i++) sb.update();
        assertEquals(1, events);
        verify(objective, times(1)).getScore("Map castle");
        mapName = "keep";
        sb.update();
        assertEquals(2, events);
        verify(board).resetScores("Map castle");
        verify(objective).getScore("Map keep");
    }

    @Test
    void listenerChangesTitleAndLinesWithoutResendLoop() {
        listeners.add(e -> {
            e.setTitle("custom");
            e.setLines(List.of("A", "A", "B"));
        });
        NScoreboard sb = create("Waiting");
        for (int i = 0; i < 5; i++) sb.update();
        assertEquals(1, events);
        verify(objective).setDisplayName("custom");
        verify(objective, times(1)).getScore("A");
        verify(objective, times(1)).getScore("A§r");
        verify(objective, times(1)).getScore("B");
        verify(score, times(1)).setScore(3);
    }

    @Test
    void cancelledEventSendsNothingUntilChangeOrRefresh() {
        listeners.add(e -> e.setCancelled(true));
        NScoreboard sb = create("Waiting");
        sb.update();
        sb.update();
        assertEquals(1, events);
        verify(objective, never()).getScore(anyString());
        listeners.clear();
        sb.refresh();
        assertEquals(2, events);
        verify(objective).getScore("Map castle");
    }

    @Test
    void disabledScoreboardIsNotCreated() {
        assertNull(create("Disabled"));
        assertNull(create("Nope"));
        assertEquals(0, events);
    }

    @Test
    void eventValidatesLines() {
        ScoreboardUpdateEvent e = new ScoreboardUpdateEvent(player, null, "x", "t", List.of("a"));
        assertTrue(e.getArena().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> e.setLines(null));
        List<String> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> e.setLines(withNull));
        assertThrows(IllegalArgumentException.class, () -> e.setLines(java.util.Collections.nCopies(16, "x")));
        e.setLines(java.util.Collections.nCopies(15, "x"));
        assertEquals(15, e.getLines().size());
        e.getLines().clear();
        assertEquals(15, e.getLines().size());
        e.setTitle(null);
        assertEquals("", e.getTitle());
        List<String> source = new ArrayList<>(List.of("a"));
        ScoreboardUpdateEvent copy = new ScoreboardUpdateEvent(player, null, "x", "t", source);
        source.add("b");
        assertEquals(List.of("a"), copy.getLines());
    }
}
