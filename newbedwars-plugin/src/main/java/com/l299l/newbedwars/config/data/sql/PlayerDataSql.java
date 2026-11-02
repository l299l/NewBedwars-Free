package com.l299l.newbedwars.config.data.sql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * {@code Storage.Type: MYSQL}: player data and stats live in the database, shared by every server that uses it.
 * A player's data is loaded when they join; a game's result is added in SQL; leaderboards are read every minute.
 */
public final class PlayerDataSql implements Listener {
    private static final Gson GSON = new Gson();
    private static final Type FAST_BUY = new TypeToken<Map<String, List<String>>>() {}.getType();
    /** {@code /bw leaderboard} keys -> columns. */
    static final Map<String, String> BOARDS = Map.of("wins", "wins", "kills", "kills", "beds", "beds_broken", "finalKills", "final_kills");

    private final NewBedwars plugin;
    private final Database db;
    private final Set<String> loaded = ConcurrentHashMap.newKeySet();
    private BukkitTask leaderboardTask;

    public PlayerDataSql(NewBedwars plugin) {
        this.plugin = plugin;
        this.db = Database.get(plugin.getLogger());
    }

    private PlayerManager pm() {
        return plugin.getPlayerManager();
    }

    /**
     * Creates the tables. When the database is empty, the data already in memory (read from this server's JSON
     * files just before) is imported once. Then the in-memory stats are dropped: they come from the database now.
     */
    public void load() {
        try {
            db.runNow(c -> {
                PlayerSqlRepo.createTables(c);
                if (PlayerSqlRepo.isEmpty(c) && (!pm().getAllStats().isEmpty() || !pm().getPlayers().isEmpty())) {
                    long now = System.currentTimeMillis();
                    for (PlayerProfile p : pm().getPlayers().values()) PlayerSqlRepo.savePlayer(c, row(p), now);
                    for (Map.Entry<String, PlayerStats> e : pm().getAllStats().entrySet()) PlayerSqlRepo.addStats(c, e.getKey(), e.getValue());
                    plugin.getLogger().info("[MySQL] Imported " + pm().getPlayers().size() + " players and "
                            + pm().getAllStats().size() + " stats from the JSON files");
                }
            });
            plugin.getLogger().info("[MySQL] Player data and stats are stored in MySQL");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[MySQL] Could not connect; player data and stats will not be saved: " + e.getMessage());
        }
        pm().getAllStats().clear();
        for (Player p : Bukkit.getOnlinePlayers()) loadPlayer(p);
        refreshLeaderboards();
        leaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            refreshLeaderboards();
            // Games on other servers change stats too: re-read the online players' stats.
            for (Player p : Bukkit.getOnlinePlayers()) refreshStats(p.getName());
        }, 20L * 60, 20L * 60);
    }

    public void stop() {
        if (leaderboardTask != null) leaderboardTask.cancel();
        leaderboardTask = null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        loadPlayer(e.getPlayer());
    }

    /** Reads the player's data and stats in the background and puts them into the {@link PlayerManager}. */
    void loadPlayer(Player player) {
        String name = player.getName();
        UUID id = player.getUniqueId();
        loaded.remove(name);
        db.submit("Loading " + name, c -> {
            try {
                PlayerSqlRepo.PlayerRow row = PlayerSqlRepo.loadPlayer(c, name);
                PlayerStats stats = PlayerSqlRepo.loadStats(c, name);
                Bukkit.getScheduler().runTask(plugin, () -> apply(name, id, row, stats));
            } catch (SQLException | RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "[MySQL] Could not load the data of " + name + "; it will not be saved until it loads (retrying in 30s)", e);
                Bukkit.getScheduler().runTask(plugin, () -> scheduleRetry(name));
            }
        });
    }

    private void scheduleRetry(String name) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayerExact(name);
            if (online != null && !loaded.contains(name)) loadPlayer(online);
        }, 20L * 30);
    }

    boolean isLoaded(String name) {
        return loaded.contains(name);
    }

    private void refreshStats(String name) {
        db.submit("Reading the stats of " + name, c -> {
            PlayerStats stats = PlayerSqlRepo.loadStats(c, name);
            if (stats != null) Bukkit.getScheduler().runTask(plugin, () -> pm().addStats(name, stats));
        });
    }

    void apply(String name, UUID id, PlayerSqlRepo.PlayerRow row, PlayerStats stats) {
        loaded.add(name);
        pm().addStats(name, stats != null ? stats : PlayerStats.empty());
        if (row == null) {
            // New in the database: store what this server set up on join (default language).
            savePlayer(name);
            return;
        }
        Language lang = parseLanguage(row.language());
        PlayerProfile current = pm().getPlayer(name);
        Map<String, List<String>> fastBuy = parseFastBuy(row.fastBuyJson());
        pm().addPlayer(new PlayerProfile(id, name, lang != null ? lang : current != null ? current.language() : Language.English,
                fastBuy != null ? fastBuy : current != null ? current.fastBuyPerCategory() : PlayerProfile.defaultFastBuy()));
    }

    /** Stores the player's current profile (language, quick buy) in the background. */
    public void savePlayer(String name) {
        if (!loaded.contains(name)) return;
        PlayerProfile p = pm().getPlayer(name);
        if (p == null) return;
        PlayerSqlRepo.PlayerRow row = row(p);
        db.submit("Saving " + name, c -> PlayerSqlRepo.savePlayer(c, row, System.currentTimeMillis()));
    }

    public void saveOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) savePlayer(p.getName());
    }

    /** Adds one game's result to the player's stats in the database. */
    public void recordGame(String name, PlayerStats delta) {
        db.submit("Saving the stats of " + name, c -> PlayerSqlRepo.addStats(c, name, delta));
    }

    public void refreshLeaderboards() {
        db.submit("Reading leaderboards", c -> {
            Map<String, List<Map.Entry<String, PlayerStats>>> boards = new HashMap<>();
            for (Map.Entry<String, String> b : BOARDS.entrySet()) {
                boards.put(b.getKey(), new ArrayList<>(PlayerSqlRepo.top(c, b.getValue(), 10)));
            }
            Bukkit.getScheduler().runTask(plugin, () -> pm().setSharedLeaderboards(boards));
        });
    }

    static PlayerSqlRepo.PlayerRow row(PlayerProfile p) {
        return new PlayerSqlRepo.PlayerRow(p.name(), p.id() == null ? "" : p.id().toString(),
                p.language() == null ? Language.English.name() : p.language().name(),
                p.fastBuyPerCategory() == null ? null : GSON.toJson(p.fastBuyPerCategory()));
    }

    static Language parseLanguage(String s) {
        if (s == null) return null;
        try {
            return Language.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Map<String, List<String>> parseFastBuy(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return GSON.fromJson(json, FAST_BUY);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
