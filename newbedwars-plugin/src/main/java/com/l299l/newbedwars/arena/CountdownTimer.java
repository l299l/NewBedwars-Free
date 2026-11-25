package com.l299l.newbedwars.arena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CountdownTimer {
    private final Set<UUID> players;
    private final int time;
    private final IArena arena;
    private BukkitTask task;

    public CountdownTimer(Set<UUID> players, int time, IArena arena) {
        this.players = players;
        this.time = time;
        this.arena = arena;
    }

    public void start() {
        cancel();
        task = new BukkitRunnable() {
            int i = time;

            @Override
            public void run() {
                if (i == 0) {
                    cancel();
                    arena.start();
                } else {
                    arena.setPhaseTime(i);
                    for (UUID uuid : players) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            if (shows(Properties.StartCountdownTitle, i)) p.sendTitle(ChatColor.RED + "" + i, "", 0, 20, 0);
                            if (shows(Properties.StartCountdownChat, i)) {
                                NewBedwars.plugin.getMessages().send(p, "GameStartingCountdown", Map.of("/time/", Integer.toString(i)));
                            }
                            if (shows(Properties.StartCountdownSound, i)) p.playSound(p.getLocation(), "block.note_block.pling", 1, 1);
                        }
                    }
                    i--;
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
    }

    private static boolean shows(CountdownDisplay display, int secondsLeft) {
        return (display == null ? CountdownDisplay.ALWAYS : display).shows(secondsLeft);
    }

    public void cancel() {
        if (task == null) return;
        task.cancel();
        task = null;
    }
}
