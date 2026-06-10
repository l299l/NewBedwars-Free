package com.l299l.newbedwars.utils;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

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
        task = new BukkitRunnable() {
            int i = time;

            @Override
            public void run() {
                if (i == 0) {
                    cancel();
                    arena.start();
                }else {
                    arena.setPhaseTime(i);
                    for (UUID uuid : players) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            p.sendTitle(ChatColor.RED + "" + i, "", 0, 20, 0);
                            p.sendMessage(ChatColor.RED + "Game starting in " + i);
                            p.playSound(p.getLocation(), "block.note_block.pling", 1, 1);
                        }
                    }
                    i--;
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
    }

    public void cancel() {
        task.cancel();
    }
}
