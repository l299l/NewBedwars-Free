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
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
