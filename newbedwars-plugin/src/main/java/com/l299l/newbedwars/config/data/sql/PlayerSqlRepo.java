package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerSqlRepo {
    public static final String PLAYERS = "nbw_player_data";
    public static final String STATS = "nbw_player_stats";
    public static final String LEGACY_PLAYERS = "nbw_players";
    public static final String LEGACY_STATS = "nbw_stats";

    static final String CREATE_PLAYERS = "CREATE TABLE IF NOT EXISTS " + PLAYERS + " ("
            + "uuid CHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL, language VARCHAR(32) NOT NULL, "
            + "fast_buy MEDIUMTEXT NULL, updated BIGINT NOT NULL, INDEX idx_name (name))";
    static final String CREATE_STATS = "CREATE TABLE IF NOT EXISTS " + STATS + " ("
            + "uuid CHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(32) NOT NULL, wins INT NOT NULL DEFAULT 0, "
            + "losses INT NOT NULL DEFAULT 0, kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0, "
            + "final_kills INT NOT NULL DEFAULT 0, beds_broken INT NOT NULL DEFAULT 0, games_played INT NOT NULL DEFAULT 0, "
            + "INDEX idx_name (name))";
    static final String UPSERT_PLAYER = "INSERT INTO " + PLAYERS + " (uuid, name, language, fast_buy, updated) VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE name = VALUES(name), language = VALUES(language), fast_buy = VALUES(fast_buy), updated = VALUES(updated)";
    static final String RENAME_STATS = "UPDATE " + STATS + " SET name = ? WHERE uuid = ?";
    static final String SELECT_PLAYER = "SELECT uuid, name, language, fast_buy FROM " + PLAYERS + " WHERE uuid = ?";
    static final String ADD_STATS = "INSERT INTO " + STATS
            + " (uuid, name, wins, losses, kills, deaths, final_kills, beds_broken, games_played) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE name = VALUES(name), wins = wins + VALUES(wins), losses = losses + VALUES(losses), "
            + "kills = kills + VALUES(kills), deaths = deaths + VALUES(deaths), final_kills = final_kills + VALUES(final_kills), "
            + "beds_broken = beds_broken + VALUES(beds_broken), games_played = games_played + VALUES(games_played)";
    static final String IMPORT_STATS = "INSERT IGNORE INTO " + STATS
            + " (uuid, name, wins, losses, kills, deaths, final_kills, beds_broken, games_played) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    static final String IMPORT_PLAYER = "INSERT IGNORE INTO " + PLAYERS
            + " (uuid, name, language, fast_buy, updated) VALUES (?, ?, ?, ?, ?)";
    static final String SELECT_STATS = "SELECT * FROM " + STATS + " WHERE uuid = ?";
    static final String SELECT_STATS_BY_NAME = "SELECT s.* FROM " + STATS + " s LEFT JOIN " + PLAYERS
            + " p ON p.uuid = s.uuid WHERE s.name = ? ORDER BY p.updated DESC LIMIT 1";
    static final String COUNT_STATS = "SELECT COUNT(*) FROM " + STATS;
    static final String COUNT_PLAYERS = "SELECT COUNT(*) FROM " + PLAYERS;

    public static final Set<String> STAT_COLUMNS = Set.of("wins", "losses", "kills", "deaths", "final_kills", "beds_broken", "games_played");

    public record PlayerRow(String uuid, String name, String language, String fastBuyJson) {}

    public record LegacyRows(List<PlayerRow> players, Map<String, Long> updated, Map<String, PlayerStats> stats) {}

    private PlayerSqlRepo() {}

    public static void createTables(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(CREATE_PLAYERS);
            st.executeUpdate(CREATE_STATS);
        }
    }

    static final java.util.List<String> RANKED_COLUMNS = java.util.List.of("wins", "kills", "final_kills", "beds_broken");

    public static java.util.List<String> ensureLeaderboardIndexes(Connection c) {
        java.util.List<String> problems = new java.util.ArrayList<>();
        for (String column : RANKED_COLUMNS) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE INDEX idx_" + column + " ON " + STATS + " (" + column + ")");
            } catch (SQLException e) {
                if (e.getErrorCode() != 1061) problems.add(column + ": " + e.getMessage());
            }
        }
        return problems;
    }

    public static boolean isEmpty(Connection c) throws SQLException {
        return count(c, COUNT_STATS) == 0 && count(c, COUNT_PLAYERS) == 0;
    }

    private static int count(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getTables(c.getCatalog(), null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public static LegacyRows readLegacy(Connection c) throws SQLException {
        List<PlayerRow> players = new ArrayList<>();
        Map<String, Long> updated = new HashMap<>();
        Map<String, PlayerStats> stats = new LinkedHashMap<>();
        if (tableExists(c, LEGACY_PLAYERS)) {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name, uuid, language, fast_buy, updated FROM " + LEGACY_PLAYERS)) {
                while (rs.next()) {
                    players.add(new PlayerRow(rs.getString("uuid"), rs.getString("name"), rs.getString("language"), rs.getString("fast_buy")));
                    updated.put(rs.getString("name"), rs.getLong("updated"));
                }
            }
        }
        if (tableExists(c, LEGACY_STATS)) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + LEGACY_STATS)) {
                while (rs.next()) stats.put(rs.getString("name"), stats(rs));
            }
        }
        return new LegacyRows(players, updated, stats);
    }

    public static int migrateLegacy(Connection c, LegacyRows legacy) throws SQLException {
        Map<String, String> uuidByName = new HashMap<>();
        for (PlayerRow row : legacy.players()) {
            UUID id = parse(row.uuid());
            if (id == null) continue;
            uuidByName.put(row.name().toLowerCase(Locale.ROOT), id.toString());
            try (PreparedStatement ps = c.prepareStatement(IMPORT_PLAYER)) {
                ps.setString(1, id.toString());
                ps.setString(2, row.name());
                ps.setString(3, row.language());
                ps.setString(4, row.fastBuyJson());
                ps.setLong(5, legacy.updated().getOrDefault(row.name(), 0L));
                ps.executeUpdate();
            }
        }
        Map<String, Map.Entry<String, PlayerStats>> merged = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerStats> e : legacy.stats().entrySet()) {
            String name = e.getKey();
            String id = uuidByName.getOrDefault(name.toLowerCase(Locale.ROOT), PlayerManager.offlineId(name).toString());
            Map.Entry<String, PlayerStats> old = merged.get(id);
            PlayerStats sum = old == null ? e.getValue() : old.getValue().plus(e.getValue());
            merged.put(id, new AbstractMap.SimpleImmutableEntry<>(name, sum));
        }
        for (Map.Entry<String, Map.Entry<String, PlayerStats>> e : merged.entrySet()) {
            PlayerStats s = e.getValue().getValue();
            try (PreparedStatement ps = c.prepareStatement(IMPORT_STATS)) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue().getKey());
                setStats(ps, 3, s);
                ps.executeUpdate();
            }
        }
        return merged.size();
    }

    private static UUID parse(String s) {
        try {
            return s == null ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void savePlayer(Connection c, PlayerRow row, long now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT_PLAYER)) {
            ps.setString(1, row.uuid());
            ps.setString(2, row.name());
            ps.setString(3, row.language());
            ps.setString(4, row.fastBuyJson());
            ps.setLong(5, now);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(RENAME_STATS)) {
            ps.setString(1, row.name());
            ps.setString(2, row.uuid());
            ps.executeUpdate();
        }
    }

    public static PlayerRow loadPlayer(Connection c, UUID id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PlayerRow(rs.getString("uuid"), rs.getString("name"), rs.getString("language"), rs.getString("fast_buy"));
            }
        }
    }

    public static void addStats(Connection c, UUID id, String name, PlayerStats delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(ADD_STATS)) {
            ps.setString(1, id.toString());
            ps.setString(2, name);
            setStats(ps, 3, delta);
            ps.executeUpdate();
        }
    }

    private static void setStats(PreparedStatement ps, int from, PlayerStats s) throws SQLException {
        ps.setInt(from, s.wins());
        ps.setInt(from + 1, s.losses());
        ps.setInt(from + 2, s.kills());
        ps.setInt(from + 3, s.deaths());
        ps.setInt(from + 4, s.finalKills());
        ps.setInt(from + 5, s.bedsBroken());
        ps.setInt(from + 6, s.gamesPlayed());
    }

    public static PlayerStats loadStats(Connection c, UUID id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_STATS)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? stats(rs) : null;
            }
        }
    }

    public static PlayerStats loadStatsByName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_STATS_BY_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? stats(rs) : null;
            }
        }
    }

    public static List<Map.Entry<String, PlayerStats>> top(Connection c, String column, int limit) throws SQLException {
        if (!STAT_COLUMNS.contains(column)) throw new IllegalArgumentException("Unknown stat " + column);
        List<Map.Entry<String, PlayerStats>> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + STATS + " ORDER BY " + column + " DESC, name ASC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new AbstractMap.SimpleImmutableEntry<>(rs.getString("name"), stats(rs)));
            }
        }
        return out;
    }

    public static List<String> findNames(Connection c, String prefix, int limit) throws SQLException {
        String escaped = prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT DISTINCT name FROM " + STATS + " WHERE LOWER(name) LIKE ? ESCAPE '!' ORDER BY name ASC LIMIT ?")) {
            ps.setString(1, escaped.toLowerCase(Locale.ROOT) + "%");
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString("name"));
            }
        }
        return out;
    }

    private static PlayerStats stats(ResultSet rs) throws SQLException {
        return new PlayerStats(rs.getInt("wins"), rs.getInt("losses"), rs.getInt("kills"), rs.getInt("deaths"),
                rs.getInt("final_kills"), rs.getInt("beds_broken"), rs.getInt("games_played"));
    }

    public static PlayerStats gameDelta(int kills, int deaths, int finalKills, int bedsBroken, boolean won) {
        return PlayerStats.empty().addGameResult(kills, deaths, finalKills, bedsBroken, won);
    }
}
