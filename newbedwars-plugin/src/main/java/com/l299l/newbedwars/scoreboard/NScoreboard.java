package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.api.events.ScoreboardUpdateEvent;
import com.l299l.newbedwars.text.TextAnimator;
import com.l299l.newbedwars.text.TextEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
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
    private final List<String> applied = new ArrayList<>();
    private List<String> rendered;
    private String titleOverride;
    private boolean killed;

    public NScoreboard(String id, String name, Scoreboard scoreboard, ScoreboardManager scoreboardManager, IArena arena, Player player, TextEffect textEffect, String rawTitle) {
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
                update();
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
                if (titleOverride != null) return;
                Objective obj = scoreboard.getObjective(id);
                if (obj != null) {
                    obj.setDisplayName(animator.next(rawTitle));
                }
            }
        }.runTaskTimer(NewBedwars.plugin, 0, animator.period(2));
    }

    public void update() {
        if (killed) return;
        List<String> lines = scoreboardManager.render(name, arena, player);
        if (lines == null) {
            kill();
            return;
        }
        String title = scoreboardManager.titleOf(name);
        List<String> draft = new ArrayList<>(lines.size() + 1);
        draft.add(title);
        draft.addAll(lines);
        if (draft.equals(rendered)) return;
        rendered = draft;
        ScoreboardUpdateEvent event = ApiEvents.scoreboardUpdate(player, arena, name, title, lines);
        if (event == null) return;
        List<String> now = ScoreboardManager.makeUnique(event.getLines());
        scoreboardManager.applyLines(id, scoreboard, now, applied);
        applied.clear();
        applied.addAll(now);
        Objective obj = scoreboard.getObjective(id);
        if (obj == null) return;
        if (!event.getTitle().equals(title)) {
            titleOverride = event.getTitle();
            obj.setDisplayName(titleOverride);
        } else if (titleOverride != null) {
            titleOverride = null;
            obj.setDisplayName(title);
        }
    }

    public void refresh() {
        rendered = null;
        update();
    }

    public void kill() {
        killed = true;
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

    public String getName() {
        return name;
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
