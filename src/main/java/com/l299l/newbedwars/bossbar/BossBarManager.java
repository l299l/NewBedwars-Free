package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class BossBarManager {
    private final HashMap<String, BossBarSave> bossBars;
    private final FileConfiguration bossBarsConf;
    private final BarActionManager barActionManager;

    public BossBarManager(FileConfiguration bossBarsConf) {
        this.bossBarsConf = bossBarsConf;
        bossBars = new HashMap<String, BossBarSave>();
        barActionManager = new BarActionManager();
    }

    public void loadBossBars() {
        NewBedwars.plugin.getLogger().info("Loading BossBars...");
        for (String key : Objects.requireNonNull(bossBarsConf.getConfigurationSection("BossBars")).getKeys(false)) {
            boolean enabled = bossBarsConf.getBoolean("BossBars." + key + ".Enabled");
            String[] texts = bossBarsConf.getStringList("BossBars." + key + ".Texts").toArray(new String[0]);
            BarColor[] colors = new BarColor[texts.length];
            BarStyle[] styles = new BarStyle[texts.length];
            TextEffect[] effects = new TextEffect[texts.length];
            int time = bossBarsConf.getInt("BossBars." + key + ".Time");
            BarAction action = barActionManager.getAction(bossBarsConf.getString("BossBars." + key + ".Action"));
            String[] colorsString = bossBarsConf.getStringList("BossBars." + key + ".Colors").toArray(new String[0]);
            String[] stylesString = bossBarsConf.getStringList("BossBars." + key + ".Styles").toArray(new String[0]);
            String[] effectsString = bossBarsConf.getStringList("BossBars." + key + ".Effects").toArray(new String[0]);
            for (int i = 0; i < texts.length; i++) {
                colors[i] = BarColor.valueOf(colorsString[i]);
                styles[i] = BarStyle.valueOf(stylesString[i]);
                effects[i] = TextEffect.valueOf(effectsString[i]);

            }
            bossBars.put(key, new BossBarSave(enabled, texts, colors, time, styles, effects, action));
        }
        NewBedwars.plugin.getLogger().info("Loaded " + bossBars.size() + " BossBars");
    }

    public BossBar createBossBar(String name, IArena arena) {
        BossBarSave bossBarSave = bossBars.get(name);
        if (bossBarSave == null || !bossBarSave.isEnabled()) {
            return null;
        }
        BossBar bossBar = Bukkit.createBossBar(addInfo(bossBarSave.getTexts()[0], arena),
                bossBarSave.getColor()[0], bossBarSave.getStyle()[0]);
        bossBar.setVisible(bossBarSave.isEnabled());
        final BukkitTask[] task = {setEffect(bossBar, bossBarSave.getEffects()[0], arena)};
        if (bossBarSave.isAnimated()) {
            new BukkitRunnable() {
                int i = 1;
                @Override
                public void run() {
                    bossBar.setTitle(addInfo(bossBarSave.getTexts()[i], arena));
                    bossBar.setColor(bossBarSave.getColor()[i]);
                    bossBar.setStyle(bossBarSave.getStyle()[i]);
                    if (bossBarSave.getEffects()[i] != null) {
                        if (task[0] != null) {
                            task[0].cancel();
                            task[0] = null;
                        }
                        task[0] = setEffect(bossBar, bossBarSave.getEffects()[i], arena);
                    }
                    i++;
                    if (i == bossBarSave.getTexts().length) {
                        i = 0;
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0, 20L * bossBarSave.getTime());
        }
        return bossBar;
    }

    @Deprecated
    private BukkitTask setEffect(BossBar bossBar, TextEffect effect, IArena arena) {
        String text = addInfo(bossBar.getTitle(), arena);
        switch (effect) {
            case GOING_CHANGING -> {
                return new BukkitRunnable() {
                    int i = 0;
                    final ChatColor secondColor = getRandomColor();

                    @Override
                    public void run() {
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = ChatColor.getByChar(text.substring(0, 2).charAt(1));
                        int j = 2;
                        if (color == null) {
                            color = ChatColor.WHITE;
                            j = 0;
                        }
                        newText.append(secondColor);
                        for (; j < text.length(); j++) {
                            if (j <= i) {
                                newText.append(secondColor.toString()).append(text.charAt(j));
                            } else {
                                newText.append(color).append(text.charAt(j));
                            }
                        }
                        bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', newText.toString()));
                        i++;
                        if (i == text.length()) {
                            i = 0;
                        }
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 1);
            }
            case RAINBOW -> {
                return new BukkitRunnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = ChatColor.getByChar(text.substring(0, 2).charAt(1));
                        int j = 2;
                        if (color == null) {
                            color = ChatColor.WHITE;
                            j = 0;
                        }
                        for (; j < text.length(); j++) {
                            if (text.charAt(j) == '§' || text.charAt(j) == '&') {
                                newText.append(text.charAt(j)).append(text.charAt(j + 1));
                                j++;
                                color = ChatColor.getByChar(text.charAt(j));
                                continue;
                            }
                            if (j == i) {
                                newText.append(getRandomColor()).append(text.charAt(j));
                            } else {
                                newText.append(color).append(text.charAt(j));
                            }
                        }
                        bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', newText.toString()));
                        i++;
                        if (i == text.length()) {
                            i = 0;
                        }
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 1);
            }
            case PULSING -> {
                String startingColor = text.substring(0, 2);
                ChatColor color = ChatColor.getByChar(startingColor.charAt(1));
                if (color == null) {
                    color = getRandomColor();
                }
                ChatColor secondColor = getSecondColor(color);
                ChatColor finalColor = color;
                return new BukkitRunnable() {
                    boolean up = true;
                    @Override
                    public void run() {
                        if (up) {
                            bossBar.setTitle(secondColor + text.replaceAll("[&§]" + finalColor.getChar(), ""));
                        } else {
                            bossBar.setTitle(text);
                        }
                        up = !up;
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 15);
            }
            case RAINBOW_PULSING -> {
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        bossBar.setTitle(getRandomColor() + text);
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 5);
            }
            default -> {
                return null;
            }
        }
    }

    @Deprecated
    private ChatColor getSecondColor(ChatColor color) {
        switch (color) {
            case GREEN -> {
                return ChatColor.DARK_GREEN;
            }
            case DARK_GREEN -> {
                return ChatColor.GREEN;
            }
            case RED -> {
                return ChatColor.DARK_RED;
            }
            case DARK_RED -> {
                return ChatColor.RED;
            }
            case BLUE -> {
                return ChatColor.DARK_BLUE;
            }
            case DARK_BLUE -> {
                return ChatColor.BLUE;
            }
            case AQUA -> {
                return ChatColor.DARK_AQUA;
            }
            case DARK_AQUA -> {
                return ChatColor.AQUA;
            }
            case YELLOW -> {
                return ChatColor.GOLD;
            }
            case GOLD -> {
                return ChatColor.YELLOW;
            }
            case WHITE -> {
                return ChatColor.GRAY;
            }
            case DARK_GRAY -> {
                return ChatColor.BLACK;
            }
            case BLACK -> {
                return ChatColor.DARK_GRAY;
            }
            default -> {
                return ChatColor.WHITE;
            }
        }
    }

    @Deprecated
    private ChatColor getRandomColor() {
        List<ChatColor> colors = Arrays.asList(ChatColor.values());
        ChatColor color = colors.get(ThreadLocalRandom.current().nextInt(colors.size()));
        if (color == ChatColor.MAGIC || color == ChatColor.BOLD || color == ChatColor.STRIKETHROUGH || color == ChatColor.UNDERLINE || color == ChatColor.ITALIC
        || color == ChatColor.RESET) {
            return getRandomColor();
        }
        return color;
    }

    private String addInfo(String text, IArena arena) {
        return ChatColor.translateAlternateColorCodes('&', text
                .replace("/server_name/", Properties.ServerName)
                .replace("/arena_name/", arena.getArenaName())
                .replace("/players/", arena.getPlayers() == null ? "0" : String.valueOf(arena.getPlayers().size()))
                .replace("/max_players/", arena.getTeams() == null ? "0" : String.valueOf(arena.getMaxInTeam() * arena.getTeams().size()))
                .replace("/phase/", arena.getCurrentGamePhase()));
    }


}
