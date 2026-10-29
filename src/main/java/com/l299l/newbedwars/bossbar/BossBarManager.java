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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class BossBarManager {
    private final HashMap<String, BossBarSave> bossBars;
    private final Map<BossBar, List<BukkitTask>> tasks = new HashMap<>();
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
            List<String> colorsString = bossBarsConf.getStringList("BossBars." + key + ".Colors");
            List<String> stylesString = bossBarsConf.getStringList("BossBars." + key + ".Styles");
            List<String> effectsString = bossBarsConf.getStringList("BossBars." + key + ".Effects");
            for (int i = 0; i < texts.length; i++) {
                colors[i] = parse(BarColor.class, colorsString, i, BarColor.WHITE);
                styles[i] = parse(BarStyle.class, stylesString, i, BarStyle.SOLID);
                effects[i] = parse(TextEffect.class, effectsString, i, TextEffect.NONE);
            }
            bossBars.put(key, new BossBarSave(enabled, texts, colors, time, styles, effects, action));
        }
        NewBedwars.plugin.getLogger().info("Loaded " + bossBars.size() + " BossBars");
    }

    /** Entry {@code index} of {@code values} as an enum; a missing or unknown entry falls back to {@code fallback}. */
    private static <E extends Enum<E>> E parse(Class<E> type, List<String> values, int index, E fallback) {
        if (index >= values.size()) return fallback;
        try {
            return Enum.valueOf(type, values.get(index).toUpperCase());
        } catch (IllegalArgumentException e) {
            NewBedwars.plugin.getLogger().warning("Unknown " + type.getSimpleName() + " '" + values.get(index) + "' in bossBars.yml");
            return fallback;
        }
    }

    public BossBar createBossBar(String name, IArena arena) {
        BossBarSave bossBarSave = bossBars.get(name);
        if (bossBarSave == null || !bossBarSave.isEnabled()) {
            return null;
        }
        BossBar bossBar = Bukkit.createBossBar(addInfo(bossBarSave.getTexts()[0], arena),
                bossBarSave.getColor()[0], bossBarSave.getStyle()[0]);
        bossBar.setVisible(bossBarSave.isEnabled());
        List<BukkitTask> bossBarTasks = new ArrayList<>();
        tasks.put(bossBar, bossBarTasks);
        BukkitTask[] effect = {setEffect(bossBar, bossBarSave.getTexts()[0], bossBarSave.getEffects()[0], arena)};
        bossBarTasks.add(effect[0]);
        if (bossBarSave.isAnimated()) {
            bossBarTasks.add(new BukkitRunnable() {
                int i = 1;
                @Override
                public void run() {
                    bossBar.setTitle(addInfo(bossBarSave.getTexts()[i], arena));
                    bossBar.setColor(bossBarSave.getColor()[i]);
                    bossBar.setStyle(bossBarSave.getStyle()[i]);
                    effect[0].cancel();
                    bossBarTasks.remove(effect[0]);
                    effect[0] = setEffect(bossBar, bossBarSave.getTexts()[i], bossBarSave.getEffects()[i], arena);
                    bossBarTasks.add(effect[0]);
                    i++;
                    if (i == bossBarSave.getTexts().length) {
                        i = 0;
                    }
                }
            }.runTaskTimer(NewBedwars.plugin, 0, 20L * bossBarSave.getTime()));
        }
        return bossBar;
    }

    /** Stops the animation tasks of a bar made by {@link #createBossBar} and hides it from everyone. */
    public void removeBossBar(BossBar bossBar) {
        List<BukkitTask> bossBarTasks = tasks.remove(bossBar);
        if (bossBarTasks != null) {
            bossBarTasks.forEach(BukkitTask::cancel);
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
    }

    /** Leading color code of {@code text}, or {@code null} when it does not start with one. */
    private static ChatColor leadingColor(String text) {
        if (text.length() < 2 || (text.charAt(0) != '\u00a7' && text.charAt(0) != '&')) return null;
        return ChatColor.getByChar(text.charAt(1));
    }

    /** Starts the effect for {@code template}; the title is rebuilt on every tick so placeholders stay current. */
    private BukkitTask setEffect(BossBar bossBar, String template, TextEffect effect, IArena arena) {
        switch (effect) {
            case GOING_CHANGING -> {
                return new BukkitRunnable() {
                    int i = 0;
                    final ChatColor secondColor = getRandomColor();

                    @Override
                    public void run() {
                        String text = addInfo(template, arena);
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = leadingColor(text);
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
                        if (i >= text.length()) {
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
                        String text = addInfo(template, arena);
                        StringBuilder newText = new StringBuilder();
                        ChatColor color = leadingColor(text);
                        int j = 2;
                        if (color == null) {
                            color = ChatColor.WHITE;
                            j = 0;
                        }
                        for (; j < text.length(); j++) {
                            if ((text.charAt(j) == '\u00a7' || text.charAt(j) == '&') && j + 1 < text.length()) {
                                newText.append(text.charAt(j)).append(text.charAt(j + 1));
                                j++;
                                ChatColor code = ChatColor.getByChar(text.charAt(j));
                                if (code != null) color = code;
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
                        if (i >= text.length()) {
                            i = 0;
                        }
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 1);
            }
            case PULSING -> {
                ChatColor leading = leadingColor(addInfo(template, arena));
                ChatColor color = leading != null ? leading : getRandomColor();
                ChatColor secondColor = getSecondColor(color);
                return new BukkitRunnable() {
                    boolean up = true;
                    @Override
                    public void run() {
                        String text = addInfo(template, arena);
                        if (up) {
                            bossBar.setTitle(secondColor + text.replaceAll("[&\u00a7]" + color.getChar(), ""));
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
                        bossBar.setTitle(getRandomColor() + addInfo(template, arena));
                    }
                }.runTaskTimer(NewBedwars.plugin, 0, 5);
            }
            default -> {
                return new BukkitRunnable() {
                    @Override
                    public void run() {
                        bossBar.setTitle(addInfo(template, arena));
                    }
                }.runTaskTimer(NewBedwars.plugin, 20, 20);
            }
        }
    }

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
                .replace("/players/", String.valueOf(arena.getActivePlayerCount()))
                .replace("/max_players/", arena.getTeams() == null ? "0" : String.valueOf(arena.getMaxInTeam() * arena.getTeams().size()))
                .replace("/phase/", arena.getCurrentGamePhase()));
    }


}
