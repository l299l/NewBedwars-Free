package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.bossbar.AddonBossBar;
import com.l299l.newbedwars.api.events.BossBarUpdateEvent;
import com.l299l.newbedwars.apiimpl.adapter.ApiRegistrations;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.bossbar.AddonBossBars;
import com.l299l.newbedwars.bossbar.BossBarManager;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApiBossBarsTest {
    private static final String YAML = """
            BossBars:
              Cycle:
                Enabled: true
                Time: 7
                Texts: ["One /players/", "Two /tag/"]
                Colors: [RED, BLUE]
                Styles: [SOLID, SEGMENTED_10]
              Disabled:
                Enabled: false
                Texts: ["x"]
            """;

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ArenaRegistry> registry;
    private BukkitScheduler scheduler;
    private final List<BossBar> created = new ArrayList<>();
    private final List<Consumer<BossBarUpdateEvent>> listeners = new ArrayList<>();
    private int events;
    private String savedName;
    private ApiRegistrations registrations;
    private ApiBossBars api;
    private AddonBossBars bars;
    private BossBarManager manager;
    private Player player;
    private World world;
    private IArena arena;
    private Plugin addon;

    @BeforeEach
    void setUp() {
        savedName = Properties.ServerName;
        Properties.ServerName = "S";
        AddonPlaceholders.clear();
        bukkit = mockStatic(Bukkit.class);
        registry = mockStatic(ArenaRegistry.class);
        scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(BukkitTask.class));
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        PluginManager pm = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
        doAnswer(inv -> {
            events++;
            Event e = inv.getArgument(0);
            if (e instanceof BossBarUpdateEvent b) listeners.forEach(l -> l.accept(b));
            return null;
        }).when(pm).callEvent(any(Event.class));
        bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class))).thenAnswer(inv -> {
            BossBar bar = mock(BossBar.class);
            when(bar.getTitle()).thenReturn(inv.getArgument(0));
            created.add(bar);
            return bar;
        });
        NewBedwars plugin = mock(NewBedwars.class);
        NewBedwars.plugin = plugin;
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        manager = new BossBarManager(YamlConfiguration.loadConfiguration(new java.io.StringReader(YAML)));
        manager.loadBossBars();
        when(plugin.getBossBarManager()).thenReturn(manager);
        bars = new AddonBossBars();
        when(plugin.getAddonBossBars()).thenReturn(bars);
        registrations = new ApiRegistrations();
        api = new ApiBossBars(registrations);

        UUID id = UUID.randomUUID();
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        arena = mock(IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        when(arena.getPlayers()).thenReturn(Set.of(id));
        when(arena.getSpectators()).thenReturn(new ArrayList<>());
        when(arena.getActivePlayerCount()).thenReturn(3);
        when(arena.getMaxPlayers()).thenReturn(8);
        when(arena.getCurrentGamePhase()).thenReturn("");
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(null);
        registry.when(ArenaRegistry::all).thenReturn(List.of());
        addon = mock(Plugin.class);
        when(addon.getName()).thenReturn("Addon");
    }

    @AfterEach
    void tearDown() {
        AddonPlaceholders.clear();
        Properties.ServerName = savedName;
        NewBedwars.plugin = null;
        registry.close();
        bukkit.close();
    }

    private void inArena() {
        registry.when(() -> ArenaRegistry.byWorld(world)).thenReturn(arena);
    }

    private Runnable cycleStep() {
        ArgumentCaptor<Runnable> run = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> period = ArgumentCaptor.forClass(Long.class);
        verify(scheduler, atLeastOnce()).runTaskTimer(any(), run.capture(), anyLong(), period.capture());
        for (int i = 0; i < period.getAllValues().size(); i++) {
            if (period.getAllValues().get(i) == 140L) return run.getAllValues().get(i);
        }
        throw new AssertionError("no cycle task");
    }

    @Test
    void serviceListsBars() {
        assertEquals(List.of("Cycle", "Disabled"), api.getBossBars());
        assertTrue(api.isEnabled("Cycle"));
        assertFalse(api.isEnabled("Disabled"));
        assertFalse(api.isEnabled("Nope"));
    }

    @Test
    void showUpdateAndRemove() {
        AddonBossBar handle = api.show(addon, player, "&aHello", BarColor.GREEN, BarStyle.SOLID, 0.5);
        BossBar bar = created.get(0);
        verify(bar).addPlayer(player);
        verify(bar).setProgress(0.5);
        assertSame(player, handle.getPlayer());
        assertTrue(handle.isVisible());
        handle.setText("&bNew");
        verify(bar).setTitle("§bNew");
        handle.setColor(BarColor.RED);
        handle.setStyle(BarStyle.SEGMENTED_6);
        verify(bar).setColor(BarColor.RED);
        verify(bar).setStyle(BarStyle.SEGMENTED_6);
        handle.remove();
        assertFalse(handle.isVisible());
        verify(bar).removeAll();
        handle.remove();
        verify(bar, times(1)).removeAll();
        assertEquals(0, bars.count());
        handle.setProgress(1);
        verify(bar, never()).setProgress(1);
    }

    @Test
    void progressIsClamped() {
        AddonBossBar handle = api.show(addon, player, "x", BarColor.WHITE, BarStyle.SOLID, 5);
        BossBar bar = created.get(0);
        verify(bar).setProgress(1.0);
        handle.setProgress(-3);
        verify(bar).setProgress(0.0);
        handle.setProgress(Double.NaN);
        verify(bar, times(2)).setProgress(0.0);
        handle.setProgress(0.25);
        verify(bar).setProgress(0.25);
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> api.show(null, player, "x", BarColor.WHITE, BarStyle.SOLID, 0));
        assertThrows(IllegalArgumentException.class, () -> api.show(addon, null, "x", BarColor.WHITE, BarStyle.SOLID, 0));
        assertThrows(IllegalArgumentException.class, () -> api.show(addon, player, "x", null, BarStyle.SOLID, 0));
        assertThrows(IllegalArgumentException.class, () -> api.show(addon, player, "x", BarColor.WHITE, null, 0));
        assertEquals(0, bars.count());
    }

    @Test
    void placeholdersResolveAndOnlyDynamicTextsRefresh() {
        inArena();
        AddonPlaceholders.register("tag", (p, a) -> "TAG");
        AddonBossBar dynamic = api.show(addon, player, "/arena_name/ /players/ /tag/", BarColor.WHITE, BarStyle.SOLID, 1);
        AddonBossBar fixed = api.show(addon, player, "static", BarColor.WHITE, BarStyle.SOLID, 1);
        BossBar dynamicBar = created.get(0);
        BossBar fixedBar = created.get(1);
        bukkit.verify(() -> Bukkit.createBossBar(eq("castle 3 TAG"), any(BarColor.class), any(BarStyle.class)));
        when(arena.getActivePlayerCount()).thenReturn(4);
        bars.refresh();
        verify(dynamicBar).setTitle("castle 4 TAG");
        verify(fixedBar, never()).setTitle(anyString());
        assertTrue(dynamic.isVisible() && fixed.isVisible());
    }

    @Test
    void removedOnQuitAndWhenPlayerIsGone() {
        AddonBossBar a = api.show(addon, player, "x", BarColor.WHITE, BarStyle.SOLID, 1);
        bars.removeForPlayer(player);
        assertFalse(a.isVisible());
        AddonBossBar b = api.show(addon, player, "x", BarColor.WHITE, BarStyle.SOLID, 1);
        when(player.isOnline()).thenReturn(false);
        bars.refresh();
        assertFalse(b.isVisible());
        assertEquals(0, bars.count());
    }

    @Test
    void arenaBarsGoOnLeaveButLobbyBarsStay() {
        AddonBossBar lobby = api.show(addon, player, "lobby", BarColor.WHITE, BarStyle.SOLID, 1);
        inArena();
        AddonBossBar inGame = api.show(addon, player, "game", BarColor.WHITE, BarStyle.SOLID, 1);
        bars.removeForArena(player, "other");
        assertTrue(inGame.isVisible());
        bars.removeForArena(player, "castle");
        assertFalse(inGame.isVisible());
        assertTrue(lobby.isVisible());
    }

    @Test
    void unregisterAllRemovesOnlyThatOwnersBars() {
        Plugin other = mock(Plugin.class);
        AddonBossBar mine = api.show(addon, player, "a", BarColor.WHITE, BarStyle.SOLID, 1);
        AddonBossBar mine2 = api.show(addon, player, "b", BarColor.WHITE, BarStyle.SOLID, 1);
        AddonBossBar theirs = api.show(other, player, "c", BarColor.WHITE, BarStyle.SOLID, 1);
        registrations.releaseAll(addon);
        assertFalse(mine.isVisible());
        assertFalse(mine2.isVisible());
        assertTrue(theirs.isVisible());
        AddonBossBar again = api.show(addon, player, "d", BarColor.WHITE, BarStyle.SOLID, 1);
        registrations.releaseAll(addon);
        assertFalse(again.isVisible());
        bars.stop();
        assertFalse(theirs.isVisible());
    }

    @Test
    void configuredBarFiresEventAtCreationAndEachStepNotForAddonBars() {
        inArena();
        List<BossBarUpdateEvent> seen = new ArrayList<>();
        listeners.add(seen::add);
        BossBar bar = manager.createBossBar("Cycle", arena).getSharedBar();
        assertEquals(1, events);
        BossBarUpdateEvent first = seen.get(0);
        assertEquals("Cycle", first.getBossBar());
        assertEquals("castle", first.getArena().orElseThrow().getName());
        assertEquals("One 3", first.getText());
        assertEquals(BarColor.RED, first.getColor());
        cycleStep().run();
        assertEquals(2, events);
        assertEquals("Two /tag/", seen.get(1).getText());
        assertEquals(BarStyle.SEGMENTED_10, seen.get(1).getStyle());
        verify(bar).setColor(BarColor.BLUE);
        api.show(addon, player, "x", BarColor.WHITE, BarStyle.SOLID, 1);
        assertEquals(2, events);
    }

    @Test
    void lobbyBarEventHasNoArena() {
        List<BossBarUpdateEvent> seen = new ArrayList<>();
        listeners.add(seen::add);
        manager.createBossBar("Cycle", null);
        assertTrue(seen.get(0).getArena().isEmpty());
        assertEquals("One 0", seen.get(0).getText());
    }

    @Test
    void eventModifiesTextColorAndStyle() {
        inArena();
        listeners.add(e -> {
            e.setText("custom " + e.getBossBar());
            e.setColor(BarColor.PURPLE);
            e.setStyle(BarStyle.SEGMENTED_20);
            e.setColor(null);
        });
        BossBar bar = manager.createBossBar("Cycle", arena).getSharedBar();
        verify(bar).setTitle("custom Cycle");
        verify(bar).setColor(BarColor.PURPLE);
        verify(bar).setStyle(BarStyle.SEGMENTED_20);
        cycleStep().run();
        verify(bar, times(2)).setTitle("custom Cycle");
    }

    @Test
    void cancelledStepKeepsPreviousDisplay() {
        inArena();
        BossBar bar = manager.createBossBar("Cycle", arena).getSharedBar();
        verify(bar).setColor(BarColor.RED);
        listeners.add(e -> e.setCancelled(true));
        cycleStep().run();
        verify(bar, never()).setColor(BarColor.BLUE);
        verify(bar, never()).setTitle("Two /tag/");
    }

    @Test
    void cancelledFirstDisplayIsShownAsConfigured() {
        inArena();
        listeners.add(e -> e.setCancelled(true));
        BossBar bar = manager.createBossBar("Cycle", arena).getSharedBar();
        verify(bar).setTitle("One 3");
        verify(bar).setColor(BarColor.RED);
    }
}
