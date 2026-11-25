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

public final class PlayerDataSql implements Listener {
    private static final Gson GSON = new Gson();
    private static final Type FAST_BUY = new TypeToken<Map<String, List<String>>>() {}.getType();

    static final Map<String, String> BOARDS = Map.of("wins", "wins", "kills", "kills", "beds", "beds_broken", "finalKills", "final_kills");

    private final NewBedwars plugin;
    private final Database db;
    private final Set<UUID> loaded = ConcurrentHashMap.newKeySet();
    private BukkitTask leaderboardTask;

    public PlayerDataSql(NewBedwars plugin) {
        this.plugin = plugin;
        this.db = Database.get(plugin.getLogger());
    }

    private PlayerManager pm() {
        return plugin.getPlayerManager();
    }

    public void load() {
        try {
            db.runNow(c -> {
                PlayerSqlRepo.createTables(c);
                java.util.List<String> indexProblems = PlayerSqlRepo.ensureLeaderboardIndexes(c);
                if (!indexProblems.isEmpty()) {
                    plugin.getLogger().warning("[MySQL] Could not add the leaderboard indexes (leaderboards still work, only slower): "
                            + String.join("; ", indexProblems));
                }
                if (!PlayerSqlRepo.isEmpty(c)) return;
                c.setAutoCommit(false);
                try {
                    fillEmptyTables(c);
                    c.commit();
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            });
            plugin.getLogger().info("[MySQL] Player data and stats are stored in MySQL");
        } catch (SQLException | RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "[MySQL] Could not prepare the database (nothing was moved or imported; it is tried again on the next start); player data and stats may not be saved", e);
        }
        PendingStatsSpill spill = spill();
        if (spill != null && spill.hasPending()) db.submit("Saving kept stats", spill::replay);
        pm().getAllStats().clear();
        pm().setDropOfflineGameResults(true);
        for (Player p : Bukkit.getOnlinePlayers()) loadPlayer(p);
        refreshLeaderboards();
        leaderboardTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            refreshLeaderboards();

            for (Player p : Bukkit.getOnlinePlayers()) refreshStats(p.getUniqueId(), p.getName());
        }, 20L * 60, 20L * 60);
    }

    private void fillEmptyTables(java.sql.Connection c) throws SQLException {
        PlayerSqlRepo.LegacyRows legacy = PlayerSqlRepo.readLegacy(c);
        if (!legacy.players().isEmpty() || !legacy.stats().isEmpty()) {
            int moved = PlayerSqlRepo.migrateLegacy(c, legacy);
            plugin.getLogger().info("[MySQL] Moved " + legacy.players().size() + " players and the stats of " + moved
                    + " players to the UUID tables " + PlayerSqlRepo.PLAYERS + " and " + PlayerSqlRepo.STATS
                    + " (the old tables " + PlayerSqlRepo.LEGACY_PLAYERS + " and " + PlayerSqlRepo.LEGACY_STATS + " are kept)");
        } else if (!pm().getAllStats().isEmpty() || !pm().getPlayers().isEmpty()) {
            long now = System.currentTimeMillis();
            for (PlayerProfile p : pm().getPlayers().values()) PlayerSqlRepo.savePlayer(c, row(p), now);
            for (Map.Entry<UUID, PlayerStats> e : pm().getAllStats().entrySet()) {
                String name = pm().nameOf(e.getKey());
                PlayerSqlRepo.addStats(c, e.getKey(), name != null ? name : e.getKey().toString(), e.getValue());
            }
            plugin.getLogger().info("[MySQL] Imported " + pm().getPlayers().size() + " players and "
                    + pm().getAllStats().size() + " stats from the JSON files");
        }
    }

    public void stop() {
        if (leaderboardTask != null) leaderboardTask.cancel();
        leaderboardTask = null;
        pm().setDropOfflineGameResults(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        loadPlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        com.l299l.newbedwars.utils.MainThread.run(plugin, () -> {
            if (Bukkit.getPlayer(id) != null) return;
            loaded.remove(id);
            pm().evict(id);
        });
    }

    void loadPlayer(Player player) {
        String name = player.getName();
        UUID id = player.getUniqueId();
        loaded.remove(id);
        db.submit("Loading " + name, c -> {
            try {
                PlayerSqlRepo.PlayerRow row = PlayerSqlRepo.loadPlayer(c, id);
                PlayerStats stats = PlayerSqlRepo.loadStats(c, id);
                com.l299l.newbedwars.utils.MainThread.run(plugin, () -> apply(name, id, row, stats));
            } catch (SQLException | RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "[MySQL] Could not load the data of " + name + "; it will not be saved until it loads (retrying in 30s)", e);
                com.l299l.newbedwars.utils.MainThread.run(plugin, () -> scheduleRetry(id));
            }
        });
    }

    private void scheduleRetry(UUID id) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(id);
            if (online != null && !loaded.contains(id)) loadPlayer(online);
        }, 20L * 30);
    }

    boolean isLoaded(UUID id) {
        return loaded.contains(id);
    }

    private void refreshStats(UUID id, String name) {
        db.submit("Reading the stats of " + name, c -> {
            PlayerStats stats = PlayerSqlRepo.loadStats(c, id);
            if (stats != null) com.l299l.newbedwars.utils.MainThread.run(plugin, () -> applyStats(id, name, stats));
        });
    }

    void applyStats(UUID id, String name, PlayerStats stats) {
        if (Bukkit.getPlayer(id) == null) return;
        pm().addStats(id, name, stats);
    }

    void apply(String name, UUID id, PlayerSqlRepo.PlayerRow row, PlayerStats stats) {
        if (Bukkit.getPlayer(id) == null) return;
        loaded.add(id);
        pm().identify(id, name);
        pm().addStats(id, name, stats != null ? stats : PlayerStats.empty());
        PlayerProfile current = pm().getPlayer(id);
        if (row == null) {

            savePlayer(name);
            return;
        }
        Language lang = parseLanguage(row.language());
        Map<String, List<String>> fastBuy = parseFastBuy(row.fastBuyJson());
        pm().addPlayer(new PlayerProfile(id, name, lang != null ? lang : current != null ? current.language() : Language.English,
                fastBuy != null ? fastBuy : current != null ? current.fastBuyPerCategory() : PlayerProfile.defaultFastBuy()));
        if (!name.equals(row.name())) savePlayer(name);
    }

    public void savePlayer(String name) {
        UUID id = pm().idOf(name);
        if (id == null || !loaded.contains(id)) return;
        PlayerProfile p = pm().getPlayer(id);
        if (p == null) return;
        PlayerSqlRepo.PlayerRow row = row(p);
        db.submit("Saving " + name, c -> PlayerSqlRepo.savePlayer(c, row, System.currentTimeMillis()));
    }

    public void saveOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) savePlayer(p.getName());
    }

    public void recordGame(UUID id, String name, PlayerStats delta) {
        PendingStatsSpill spill = spill();
        db.submitRetrying("Saving the stats of " + name, c -> {
            PlayerSqlRepo.addStats(c, id, name, delta);
            if (spill != null && spill.hasPending()) spill.replay(c);
        }, spill == null ? null : () -> spill.append(id, name, delta));
    }

    private PendingStatsSpill spill() {
        java.io.File folder = plugin.getDataFolder();
        if (folder == null) return null;
        return new PendingStatsSpill(new java.io.File(new java.io.File(folder, "data"), "pending-stats.jsonl").toPath(), plugin.getLogger());
    }

    public void refreshLeaderboards() {
        db.submitLatest("leaderboards", "Reading leaderboards", c -> {
            Map<String, List<Map.Entry<String, PlayerStats>>> boards = new HashMap<>();
            for (Map.Entry<String, String> b : BOARDS.entrySet()) {
                boards.put(b.getKey(), new ArrayList<>(PlayerSqlRepo.top(c, b.getValue(), 10)));
            }
            com.l299l.newbedwars.utils.MainThread.run(plugin, () -> pm().setSharedLeaderboards(boards));
        });
    }

    static PlayerSqlRepo.PlayerRow row(PlayerProfile p) {
        return new PlayerSqlRepo.PlayerRow(p.id() == null ? "" : p.id().toString(), p.name(),
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
