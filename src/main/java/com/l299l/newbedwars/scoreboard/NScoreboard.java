package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.text.TextAnimator;
import com.l299l.newbedwars.text.TextEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class NScoreboard {
    private final String id;
    private final String name;
    private final IArena arena;
    private final Player player;
    private final Scoreboard scoreboard;
    private final BukkitTask task;
    private final BukkitTask effectTask;
    private final ScoreboardManager scoreboardManager;
    private final TextEffect textEffect;
    private final String rawTitle;

    public NScoreboard(String id, String name, Scoreboard scoreboard, ScoreboardManager scoreboardManager, IArena arena, Player player, List<String> old, TextEffect textEffect, String rawTitle) {
        this.id = id;
        this.name = name;
        this.scoreboard = scoreboard;
        this.scoreboardManager = scoreboardManager;
        this.arena = arena;
        this.player = player;
        this.textEffect = textEffect;
        this.rawTitle = rawTitle;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                List<String> actual = scoreboardManager.updateScoreboard(id, name, scoreboard, arena, player, old);
                if (actual == null) {
                    kill();
                    return;
                }
                old.clear();
                old.addAll(actual);
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
        effectTask = createEffectTask();
    }

    private BukkitTask createEffectTask() {
        if (textEffect == null || textEffect == TextEffect.NONE) return null;
        TextAnimator animator = new TextAnimator(textEffect, rawTitle, ChatColor.WHITE);
        return new BukkitRunnable() {
            @Override
            public void run() {
                Objective obj = scoreboard.getObjective(id);
                if (obj != null) {
                    obj.setDisplayName(animator.next(rawTitle));
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0, animator.period(2));
    }

    public void kill() {
        task.cancel();
        if (effectTask != null) effectTask.cancel();
        Objective obj = scoreboard.getObjective(id);
        if (obj != null) {
            obj.unregister();
        }
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public String getId() {
        return id;
    }

    public IArena getArena() {
        return arena;
    }

    public Player getPlayer() {
        return player;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
