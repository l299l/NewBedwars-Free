package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.PapiText;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BossBarManagerTest {

    private String savedServerName;

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        NewBedwars.plugin = plugin;
        savedServerName = Properties.ServerName;
        Properties.ServerName = "Server";
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
        Properties.ServerName = savedServerName;
    }

    @Test
    void stopAllCancelsTasksOfCreatedBars() {
        YamlConfiguration conf = new YamlConfiguration();
        conf.set("BossBars.WaitingBeforeStart.Enabled", true);
        conf.set("BossBars.WaitingBeforeStart.Texts", List.of("one", "two"));
        conf.set("BossBars.WaitingBeforeStart.Time", 5);
        conf.set("BossBars.WaitingBeforeStart.Action", "NONE");
        List<BukkitTask> created = new ArrayList<>();
        BossBar bar = mock(BossBar.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class);
             MockedStatic<PapiText> papi = mockStatic(PapiText.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTaskTimer(any(org.bukkit.plugin.Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(inv -> {
                        BukkitTask task = mock(BukkitTask.class);
                        created.add(task);
                        return task;
                    });
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.createBossBar(any(String.class), any(BarColor.class), any(BarStyle.class))).thenReturn(bar);
            papi.when(() -> PapiText.apply(any(), any(String.class))).thenAnswer(inv -> inv.getArgument(1));
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenReturn(null);

            BossBarManager manager = new BossBarManager(conf);
            manager.loadBossBars();
            manager.createBossBar("WaitingBeforeStart", null);
            assertFalse(created.isEmpty());

            manager.stopAll();

            for (BukkitTask task : created) verify(task).cancel();
            verify(bar).removeAll();
        }
    }

    private static final String PAPI_YAML = """
            BossBars:
              Waiting:
                Enabled: true
                Time: 5
                Texts: ["A %x%", "B %x%"]
                Colors: [RED, BLUE]
              Plain:
                Enabled: true
                Time: 5
                Texts: ["plain", "plain2"]
              Lang:
                Enabled: true
                Time: 5
                Texts: ["{lang:Greeting}", "plain"]
              """;

    private final List<BossBar> bars = new ArrayList<>();
    private final List<Runnable> timers = new ArrayList<>();
    private final List<BukkitTask> taskMocks = new ArrayList<>();
    private boolean papiPresent;

    private static Player player(String name) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(java.util.UUID.nameUUIDFromBytes(name.getBytes()));
        when(p.getName()).thenReturn(name);
        when(p.isOnline()).thenReturn(true);
        return p;
    }

    private void withBukkit(java.util.function.Consumer<MockedStatic<ApiEvents>> body) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class);
             MockedStatic<PapiText> papi = mockStatic(PapiText.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTaskTimer(any(org.bukkit.plugin.Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                    .thenAnswer(inv -> {
                        timers.add(inv.getArgument(1));
                        BukkitTask task = mock(BukkitTask.class);
                        taskMocks.add(task);
                        return task;
                    });
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            org.bukkit.plugin.PluginManager pm = mock(org.bukkit.plugin.PluginManager.class);
            when(pm.getPlugin("PlaceholderAPI")).thenReturn(papiPresent ? mock(org.bukkit.plugin.Plugin.class) : null);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
            bukkit.when(() -> Bukkit.createBossBar(any(String.class), any(BarColor.class), any(BarStyle.class))).thenAnswer(inv -> {
                BossBar bar = mock(BossBar.class);
                when(bar.getTitle()).thenReturn(inv.getArgument(0));
                bars.add(bar);
                return bar;
            });
            papi.when(() -> PapiText.apply(any(), any(String.class))).thenAnswer(inv -> {
                Player p = inv.getArgument(0);
                String text = inv.getArgument(1);
                return p == null ? text : text.replace("%x%", "x-" + p.getName());
            });
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenReturn(null);
            body.accept(events);
        }
    }

    private static com.l299l.newbedwars.arena.IArena arena() {
        com.l299l.newbedwars.arena.IArena arena = mock(com.l299l.newbedwars.arena.IArena.class);
        when(arena.getArenaName()).thenReturn("castle");
        when(arena.getActivePlayerCount()).thenReturn(1);
        when(arena.getMaxPlayers()).thenReturn(8);
        when(arena.getCurrentGamePhase()).thenReturn("");
        return arena;
    }

    private BossBarManager manager() {
        BossBarManager manager = new BossBarManager(YamlConfiguration.loadConfiguration(new java.io.StringReader(PAPI_YAML)));
        manager.loadBossBars();
        return manager;
    }

    @Test
    void sharedBarWithoutPercentEvenWithPlaceholderApi() {
        papiPresent = true;
        withBukkit(events -> {
            ManagedBossBar bar = manager().createBossBar("Plain", arena());
            assertFalse(bar.isPerPlayer());
            Player a = player("a");
            Player b = player("b");
            bar.addPlayer(a);
            bar.addPlayer(b);
            assertEquals(1, bars.size());
            verify(bars.get(0)).addPlayer(a);
            verify(bars.get(0)).addPlayer(b);
        });
    }

    @Test
    void langTokenMakesBarPerPlayerWithoutPlaceholderApi() {
        papiPresent = false;
        com.l299l.newbedwars.config.Messages messages = mock(com.l299l.newbedwars.config.Messages.class);
        when(messages.getRaw(any(), any(String.class))).thenAnswer(inv ->
                inv.getArgument(0) == null ? "default" : "hi " + ((Player) inv.getArgument(0)).getName());
        when(NewBedwars.plugin.getMessages()).thenReturn(messages);
        withBukkit(events -> {
            ManagedBossBar bar = manager().createBossBar("Lang", arena());
            assertTrue(bar.isPerPlayer());
            assertTrue(bars.isEmpty());
            bar.addPlayer(player("a"));
            assertEquals("hi a", bars.get(0).getTitle());
        });
    }

    @Test
    void sharedBarWithoutPlaceholderApi() {
        papiPresent = false;
        withBukkit(events -> {
            ManagedBossBar bar = manager().createBossBar("Waiting", arena());
            assertFalse(bar.isPerPlayer());
            assertEquals(1, bars.size());
        });
    }

    @Test
    void perPlayerBarsResolvePlaceholdersForEachViewer() {
        papiPresent = true;
        withBukkit(events -> {
            ManagedBossBar bar = manager().createBossBar("Waiting", arena());
            assertTrue(bar.isPerPlayer());
            assertTrue(bars.isEmpty());
            Player a = player("a");
            Player b = player("b");
            bar.addPlayer(a);
            bar.addPlayer(b);
            bar.addPlayer(a);
            assertEquals(2, bars.size());
            assertEquals("A x-a", bars.get(0).getTitle());
            assertEquals("A x-b", bars.get(1).getTitle());
            verify(bars.get(0)).addPlayer(a);
            verify(bars.get(1)).addPlayer(b);
            assertEquals(List.of(a, b), bar.getPlayers());
        });
    }

    @Test
    void stepIsSharedFiresOneEventAndUpdatesEveryViewer() {
        papiPresent = true;
        withBukkit(events -> {
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenAnswer(inv ->
                    new com.l299l.newbedwars.api.events.BossBarUpdateEvent(null, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4)));
            ManagedBossBar bar = manager().createBossBar("Waiting", arena());
            Player a = player("a");
            Player b = player("b");
            bar.addPlayer(a);
            bar.addPlayer(b);
            events.verify(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any()), times(1));
            timers.get(1).run();
            events.verify(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any()), times(2));
            events.verify(() -> ApiEvents.bossBarUpdate(any(), any(), org.mockito.ArgumentMatchers.eq("B %x%"), any(), any()), times(1));
            verify(bars.get(0)).setTitle("B x-a");
            verify(bars.get(1)).setTitle("B x-b");
        });
    }

    @Test
    void lateJoinerGetsCurrentStep() {
        papiPresent = true;
        withBukkit(events -> {
            com.l299l.newbedwars.api.events.BossBarUpdateEvent[] last = new com.l299l.newbedwars.api.events.BossBarUpdateEvent[1];
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenAnswer(inv ->
                    last[0] = new com.l299l.newbedwars.api.events.BossBarUpdateEvent(null, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4)));
            ManagedBossBar bar = manager().createBossBar("Waiting", arena());
            Player a = player("a");
            bar.addPlayer(a);
            timers.get(1).run();
            verify(bars.get(0)).setTitle("B x-a");
            verify(bars.get(0)).setColor(BarColor.BLUE);
            bar.addPlayer(player("late"));
            assertEquals("B x-late", bars.get(1).getTitle());
        });
    }

    @Test
    void addonChangedTextGetsPlaceholdersPerViewer() {
        papiPresent = true;
        withBukkit(events -> {
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenAnswer(inv -> {
                com.l299l.newbedwars.api.events.BossBarUpdateEvent e = new com.l299l.newbedwars.api.events.BossBarUpdateEvent(
                        null, inv.getArgument(1), inv.getArgument(2), inv.getArgument(3), inv.getArgument(4));
                e.setText("hi %x%");
                return e;
            });
            ManagedBossBar bar = manager().createBossBar("Waiting", arena());
            bar.addPlayer(player("a"));
            assertEquals("hi x-a", bars.get(0).getTitle());
        });
    }

    @Test
    void removePlayerAndRemoveCleanUp() {
        papiPresent = true;
        withBukkit(events -> {
            BossBarManager manager = manager();
            ManagedBossBar bar = manager.createBossBar("Waiting", arena());
            Player a = player("a");
            Player b = player("b");
            bar.addPlayer(a);
            bar.addPlayer(b);
            bar.removePlayer(a);
            verify(bars.get(0)).removeAll();
            assertEquals(List.of(b), bar.getPlayers());
            manager.removeBossBar(bar);
            verify(bars.get(1)).removeAll();
            assertTrue(bar.getPlayers().isEmpty());
            for (BukkitTask task : taskMocks) verify(task).cancel();
            bar.addPlayer(a);
            assertEquals(2, bars.size());
        });
    }
}
