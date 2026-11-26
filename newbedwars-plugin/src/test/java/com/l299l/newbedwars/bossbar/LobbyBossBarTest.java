package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.PapiText;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyBossBarTest {

    @BeforeEach
    void setUp() {
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        NewBedwars.plugin = plugin;
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    @Test
    void lobbyPlaceholdersUseAllArenas() {
        assertEquals("Players: 7 of 32 []", BossBarManager.lobbyInfo("Players: /players/ of /max_players/ [/arena_name//phase/]", 7, 32));
    }

    private static Player player(String name, World world) {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.getName()).thenReturn(name);
        when(p.isOnline()).thenReturn(true);
        when(p.getWorld()).thenReturn(world);
        return p;
    }

    @Test
    void lobbyBarWithPapiTextIsPerPlayerAndFollowsLobbyPlayers() {
        YamlConfiguration conf = new YamlConfiguration();
        conf.set("BossBars.MainLobby.Enabled", true);
        conf.set("BossBars.MainLobby.Texts", List.of("Hi %name%"));
        conf.set("BossBars.MainLobby.Action", "NONE");
        List<BossBar> bars = new ArrayList<>();
        List<Runnable> timers = new ArrayList<>();
        World lobbyWorld = mock(World.class);
        World arenaWorld = mock(World.class);
        Player in = player("in", lobbyWorld);
        Player out = player("out", arenaWorld);
        String saved = Properties.ServerName;
        Properties.ServerName = "S";
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<ApiEvents> events = mockStatic(ApiEvents.class);
             MockedStatic<ArenaRegistry> registry = mockStatic(ArenaRegistry.class);
             MockedStatic<PapiText> papi = mockStatic(PapiText.class)) {
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(inv -> {
                timers.add(inv.getArgument(1));
                return mock(BukkitTask.class);
            });
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            PluginManager pm = mock(PluginManager.class);
            when(pm.getPlugin("PlaceholderAPI")).thenReturn(mock(Plugin.class));
            bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
            bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(inv -> List.of(in, out));
            bukkit.when(() -> Bukkit.createBossBar(any(String.class), any(BarColor.class), any(BarStyle.class))).thenAnswer(inv -> {
                BossBar bar = mock(BossBar.class);
                when(bar.getTitle()).thenReturn(inv.getArgument(0));
                bars.add(bar);
                return bar;
            });
            registry.when(() -> ArenaRegistry.byWorld(lobbyWorld)).thenReturn(null);
            registry.when(() -> ArenaRegistry.byWorld(arenaWorld)).thenReturn(mock(IArena.class));
            papi.when(() -> PapiText.apply(any(), any(String.class))).thenAnswer(inv -> {
                Player p = inv.getArgument(0);
                String text = inv.getArgument(1);
                return p == null ? text : text.replace("%name%", p.getName());
            });
            events.when(() -> ApiEvents.bossBarUpdate(any(), any(), any(), any(), any())).thenReturn(null);

            BossBarManager manager = new BossBarManager(conf);
            manager.loadBossBars();
            manager.startLobbyBossBar();
            timers.get(timers.size() - 1).run();

            assertEquals(1, bars.size());
            assertEquals("Hi in", bars.get(0).getTitle());
            verify(bars.get(0)).addPlayer(in);
            verify(bars.get(0), never()).addPlayer(out);

            manager.stopLobbyBossBar();
            verify(bars.get(0)).removeAll();
        } finally {
            Properties.ServerName = saved;
        }
    }
}
