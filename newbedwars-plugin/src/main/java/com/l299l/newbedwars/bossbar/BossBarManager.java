package com.l299l.newbedwars.bossbar;

import com.l299l.newbedwars.text.TextAnimator;
import com.l299l.newbedwars.text.TextEffect;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BossBarManager {
    public static final String LOBBY_BOSS_BAR = "MainLobby";
    private final HashMap<String, BossBarSave> bossBars;
    private final Map<BossBar, List<BukkitTask>> tasks = new HashMap<>();
    private final FileConfiguration bossBarsConf;
    private final BarActionManager barActionManager;
    private BossBar lobbyBossBar;
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

    public void removeBossBar(BossBar bossBar) {
        List<BukkitTask> bossBarTasks = tasks.remove(bossBar);
        if (bossBarTasks != null) {
            bossBarTasks.forEach(BukkitTask::cancel);
        }
        bossBar.removeAll();
        bossBar.setVisible(false);
    }

    private BukkitTask setEffect(BossBar bossBar, String template, TextEffect effect, IArena arena) {
        TextAnimator animator = new TextAnimator(effect, addInfo(template, arena), null);
        long period = animator.period(1);
        return new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.setTitle(animator.next(addInfo(template, arena)));
            }
        }.runTaskTimer(NewBedwars.plugin, animator.getEffect() == TextEffect.NONE ? 20 : 0, period);
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
        return text.replace("/arena_name/", "")
                .replace("/players/", String.valueOf(players))
                .replace("/max_players/", String.valueOf(maxPlayers))
                .replace("/phase/", "");
    }

    private String addInfo(String text, IArena arena) {
        if (arena == null) {
            int players = 0;
            int max = 0;
            for (IArena a : ArenaRegistry.all()) {
                if (!a.isEnabled()) continue;
                players += a.getActivePlayerCount();
                max += a.getMaxPlayers();
            }
            return ChatColor.translateAlternateColorCodes('&',
                    lobbyInfo(text.replace("/server_name/", Properties.ServerName), players, max));
        }
        return ChatColor.translateAlternateColorCodes('&', text
                .replace("/server_name/", Properties.ServerName)
                .replace("/arena_name/", arena.getArenaName())
                .replace("/players/", String.valueOf(arena.getActivePlayerCount()))
                .replace("/max_players/", String.valueOf(arena.getMaxPlayers()))
                .replace("/phase/", arena.getCurrentGamePhase()));
    }

}
