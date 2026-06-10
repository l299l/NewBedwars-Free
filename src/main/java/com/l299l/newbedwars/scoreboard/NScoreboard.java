package com.l299l.newbedwars.scoreboard;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.bossbar.TextEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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
        }.runTaskTimerAsynchronously(NewBedwars.plugin, 0, 20);
        effectTask = createEffectTask();
    }

    private BukkitTask createEffectTask() {
        if (textEffect == null || textEffect == TextEffect.NONE) return null;
        switch (textEffect) {
            case RAINBOW: {
                return new BukkitRunnable() {
                    int i = 0;
                    @Override
                    public void run() {
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = ChatColor.WHITE;
                        for (int j = 0; j < rawTitle.length(); j++) {
                            if (rawTitle.charAt(j) == '&' && j + 1 < rawTitle.length()) {
                                ChatColor code = ChatColor.getByChar(rawTitle.charAt(j + 1));
                                if (code != null) color = code;
                                newText.append(rawTitle.charAt(j)).append(rawTitle.charAt(j + 1));
                                j++;
                                continue;
                            }
                            if (j == i) {
                                newText.append(getRandomColor()).append(rawTitle.charAt(j));
                            } else {
                                newText.append(color).append(rawTitle.charAt(j));
                            }
                        }
                        try {
                            scoreboard.getObjective(id).setDisplayName(ChatColor.translateAlternateColorCodes('&', newText.toString()));
                        } catch (Exception ignored) {}
                        i++;
                        if (i >= rawTitle.length()) i = 0;
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 2);
            }
            case GOING_CHANGING: {
                return new BukkitRunnable() {
                    int i = 0;
                    final ChatColor secondColor = getRandomColor();
                    @Override
                    public void run() {
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = ChatColor.WHITE;
                        int start = 0;
                        if (rawTitle.startsWith("&")) {
                            ChatColor code = ChatColor.getByChar(rawTitle.charAt(1));
                            if (code != null) color = code;
                            start = 2;
                        }
                        newText.append(secondColor);
                        for (int j = start; j < rawTitle.length(); j++) {
                            if (j <= i) {
                                newText.append(secondColor).append(rawTitle.charAt(j));
                            } else {
                                newText.append(color).append(rawTitle.charAt(j));
                            }
                        }
                        try {
                            scoreboard.getObjective(id).setDisplayName(ChatColor.translateAlternateColorCodes('&', newText.toString()));
                        } catch (Exception ignored) {}
                        i++;
                        if (i >= rawTitle.length()) i = 0;
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 2);
            }
            case PULSING: {
                ChatColor color = ChatColor.WHITE;
                if (rawTitle.startsWith("&")) {
                    ChatColor code = ChatColor.getByChar(rawTitle.charAt(1));
                    if (code != null) color = code;
                }
                ChatColor secondColor = getPulseColor(color);
                ChatColor finalColor = color;
                return new BukkitRunnable() {
                    boolean up = true;
                    @Override
                    public void run() {
                        try {
                            if (up) {
                                scoreboard.getObjective(id).setDisplayName(secondColor + ChatColor.translateAlternateColorCodes('&', rawTitle).replaceAll("[&§]" + finalColor.getChar(), ""));
                            } else {
                                scoreboard.getObjective(id).setDisplayName(ChatColor.translateAlternateColorCodes('&', rawTitle));
                            }
                        } catch (Exception ignored) {}
                        up = !up;
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 15);
            }
            case RAINBOW_PULSING: {
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            scoreboard.getObjective(id).setDisplayName(getRandomColor() + ChatColor.translateAlternateColorCodes('&', rawTitle));
                        } catch (Exception ignored) {}
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 5);
            }
            default: return null;
        }
    }

    public void kill() {
        task.cancel();
        if (effectTask != null) effectTask.cancel();
        try {
            Objects.requireNonNull(scoreboard.getObjective(id)).unregister();
        } catch (Exception ignored) {}
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

    private ChatColor getRandomColor() {
        List<ChatColor> colors = Arrays.asList(ChatColor.values());
        ChatColor color = colors.get(new Random().nextInt(colors.size()));
        if (color == ChatColor.MAGIC || color == ChatColor.BOLD || color == ChatColor.STRIKETHROUGH || color == ChatColor.UNDERLINE || color == ChatColor.ITALIC || color == ChatColor.RESET) {
            return getRandomColor();
        }
        return color;
    }

    private ChatColor getPulseColor(ChatColor color) {
        switch (color) {
            case GREEN: return ChatColor.DARK_GREEN;
            case DARK_GREEN: return ChatColor.GREEN;
            case RED: return ChatColor.DARK_RED;
            case DARK_RED: return ChatColor.RED;
            case BLUE: return ChatColor.DARK_BLUE;
            case DARK_BLUE: return ChatColor.BLUE;
            case AQUA: return ChatColor.DARK_AQUA;
            case DARK_AQUA: return ChatColor.AQUA;
            case YELLOW: return ChatColor.GOLD;
            case GOLD: return ChatColor.YELLOW;
            case WHITE: return ChatColor.GRAY;
            case DARK_GRAY: return ChatColor.BLACK;
            case BLACK: return ChatColor.DARK_GRAY;
            default: return ChatColor.WHITE;
        }
    }
}
