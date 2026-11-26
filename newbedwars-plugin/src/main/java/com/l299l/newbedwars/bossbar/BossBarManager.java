package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.text.TextEffect;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.placeholder.AddonPlaceholders;
import com.l299l.newbedwars.text.Tokens;
import com.l299l.newbedwars.placeholder.PapiText;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.l299l.newbedwars.extensions.PluginDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BossBarManager {
    public static final String LOBBY_BOSS_BAR = "MainLobby";
    private final HashMap<String, BossBarSave> bossBars;
    private final Set<ManagedBossBar> active = new LinkedHashSet<>();
    private final FileConfiguration bossBarsConf;
    private final BarActionManager barActionManager;
    private ManagedBossBar lobbyBossBar;
    private BukkitTask lobbySync;

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
            if (texts.length == 0) {
                NewBedwars.plugin.getLogger().warning("bossBars.yml: " + key + " has no Texts; it is shown empty");
                texts = new String[]{""};
            }
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

    private static <E extends Enum<E>> E parse(Class<E> type, List<String> values, int index, E fallback) {
        if (index >= values.size()) return fallback;
        try {
            return Enum.valueOf(type, values.get(index).toUpperCase());
        } catch (IllegalArgumentException e) {
            NewBedwars.plugin.getLogger().warning("Unknown " + type.getSimpleName() + " '" + values.get(index) + "' in bossBars.yml");
            return fallback;
        }
    }

    public ManagedBossBar createBossBar(String name, IArena arena) {
        BossBarSave bossBarSave = bossBars.get(name);
        if (bossBarSave == null || !bossBarSave.isEnabled()) {
            return null;
        }
        ManagedBossBar bossBar = new ManagedBossBar(name, bossBarSave, arena, needsPerPlayer(bossBarSave));
        active.add(bossBar);
        bossBar.start();
        return bossBar;
    }

    private static boolean needsPerPlayer(BossBarSave save) {
        boolean percent = false;
        for (String text : save.getTexts()) {
            if (com.l299l.newbedwars.text.LangTokens.contains(text)) return true;
            if (text.indexOf('%') >= 0) percent = true;
        }
        return percent && PluginDetector.isPlaceholderAPIPresent();
    }

    public List<String> getIds() {
        List<String> ids = new ArrayList<>(bossBars.keySet());
        java.util.Collections.sort(ids);
        return ids;
    }

    public boolean isEnabled(String id) {
        BossBarSave save = id == null ? null : bossBars.get(id);
        return save != null && save.isEnabled();
    }

    public void removeBossBar(ManagedBossBar bossBar) {
        active.remove(bossBar);
        bossBar.remove();
    }

    public void stopAll() {
        stopLobbyBossBar();
        for (ManagedBossBar bossBar : new ArrayList<>(active)) {
            removeBossBar(bossBar);
        }
        active.clear();
    }

    public void startLobbyBossBar() {
        stopLobbyBossBar();
        if (Properties.NetworkMode == com.l299l.newbedwars.api.view.NetworkMode.GAME) return;
        lobbyBossBar = createBossBar(LOBBY_BOSS_BAR, null);
        if (lobbyBossBar == null) return;
        lobbySync = new BukkitRunnable() {
            @Override
            public void run() {
                syncLobbyBossBar();
            }
        }.runTaskTimer(NewBedwars.plugin, 0, 20);
    }

    public void stopLobbyBossBar() {
        if (lobbySync != null) {
            lobbySync.cancel();
            lobbySync = null;
        }
        if (lobbyBossBar != null) {
            removeBossBar(lobbyBossBar);
            lobbyBossBar = null;
        }
    }

    private void syncLobbyBossBar() {
        List<Player> shown = lobbyBossBar.getPlayers();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean inLobby = ArenaRegistry.byWorld(player.getWorld()) == null;
            boolean has = shown.contains(player);
            if (inLobby && !has) lobbyBossBar.addPlayer(player);
            else if (!inLobby && has) lobbyBossBar.removePlayer(player);
        }
    }

    static String lobbyInfo(String text, int players, int maxPlayers) {
        return com.l299l.newbedwars.text.Tokens.apply(text, Map.of(
                "arena_name", "",
                "players", String.valueOf(players),
                "max_players", String.valueOf(maxPlayers),
                "phase", ""));
    }

    static String render(String text, IArena arena, Player viewer) {
        text = com.l299l.newbedwars.text.LangTokens.apply(text, viewer);
        String out;
        if (arena == null) {
            int players = 0;
            int max = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (ArenaRegistry.byWorld(online.getWorld()) == null) players++;
            }
            for (IArena a : ArenaRegistry.all()) {
                if (!a.isEnabled()) continue;
                players += a.getActivePlayerCount();
                max += a.getMaxPlayers();
            }
            out = lobbyInfo(Tokens.apply(text, Map.of("server_name", String.valueOf(Properties.ServerName))), players, max);
        } else {
            out = Tokens.apply(text, Map.of(
                    "server_name", String.valueOf(Properties.ServerName),
                    "arena_name", arena.getArenaName(),
                    "players", String.valueOf(arena.getActivePlayerCount()),
                    "max_players", String.valueOf(arena.getMaxPlayers()),
                    "phase", String.valueOf(arena.getCurrentGamePhase())));
        }
        out = AddonPlaceholders.apply(out, '/', '/', viewer, arena);
        out = ChatColor.translateAlternateColorCodes('&', out);
        return PapiText.apply(viewer, out);
    }

}
