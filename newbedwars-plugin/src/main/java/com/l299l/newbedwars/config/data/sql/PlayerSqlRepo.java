package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.player.PlayerStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PlayerSqlRepo {
    public static final String PLAYERS = "nbw_players";
    public static final String STATS = "nbw_stats";

    static final String CREATE_PLAYERS = "CREATE TABLE IF NOT EXISTS " + PLAYERS + " ("
            + "name VARCHAR(32) NOT NULL PRIMARY KEY, uuid CHAR(36) NOT NULL, language VARCHAR(32) NOT NULL, "
            + "fast_buy MEDIUMTEXT NULL, updated BIGINT NOT NULL)";
    static final String CREATE_STATS = "CREATE TABLE IF NOT EXISTS " + STATS + " ("
            + "name VARCHAR(32) NOT NULL PRIMARY KEY, wins INT NOT NULL DEFAULT 0, losses INT NOT NULL DEFAULT 0, "
            + "kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0, final_kills INT NOT NULL DEFAULT 0, "
            + "beds_broken INT NOT NULL DEFAULT 0, games_played INT NOT NULL DEFAULT 0)";
    static final String UPSERT_PLAYER = "INSERT INTO " + PLAYERS + " (name, uuid, language, fast_buy, updated) VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), language = VALUES(language), fast_buy = VALUES(fast_buy), updated = VALUES(updated)";
    static final String SELECT_PLAYER = "SELECT name, uuid, language, fast_buy FROM " + PLAYERS + " WHERE name = ?";
    static final String ADD_STATS = "INSERT INTO " + STATS
            + " (name, wins, losses, kills, deaths, final_kills, beds_broken, games_played) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE wins = wins + VALUES(wins), losses = losses + VALUES(losses), kills = kills + VALUES(kills), "
            + "deaths = deaths + VALUES(deaths), final_kills = final_kills + VALUES(final_kills), "
            + "beds_broken = beds_broken + VALUES(beds_broken), games_played = games_played + VALUES(games_played)";
    static final String SELECT_STATS = "SELECT * FROM " + STATS + " WHERE name = ?";
    static final String COUNT_STATS = "SELECT COUNT(*) FROM " + STATS;
    static final String COUNT_PLAYERS = "SELECT COUNT(*) FROM " + PLAYERS;

    public static final Set<String> STAT_COLUMNS = Set.of("wins", "losses", "kills", "deaths", "final_kills", "beds_broken", "games_played");

    public record PlayerRow(String name, String uuid, String language, String fastBuyJson) {}

    private PlayerSqlRepo() {}

    public static void createTables(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(CREATE_PLAYERS);
            st.executeUpdate(CREATE_STATS);
        }
    }

    public static boolean isEmpty(Connection c) throws SQLException {
        return count(c, COUNT_STATS) == 0 && count(c, COUNT_PLAYERS) == 0;
    }

    private static int count(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static void savePlayer(Connection c, PlayerRow row, long now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT_PLAYER)) {
            ps.setString(1, row.name());
            ps.setString(2, row.uuid());
            ps.setString(3, row.language());
            ps.setString(4, row.fastBuyJson());
            ps.setLong(5, now);
            ps.executeUpdate();
        }
    }

    public static PlayerRow loadPlayer(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_PLAYER)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PlayerRow(rs.getString("name"), rs.getString("uuid"), rs.getString("language"), rs.getString("fast_buy"));
            }
        }
    }

    public static void addStats(Connection c, String name, PlayerStats delta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(ADD_STATS)) {
            ps.setString(1, name);
            ps.setInt(2, delta.wins());
            ps.setInt(3, delta.losses());
            ps.setInt(4, delta.kills());
            ps.setInt(5, delta.deaths());
            ps.setInt(6, delta.finalKills());
            ps.setInt(7, delta.bedsBroken());
            ps.setInt(8, delta.gamesPlayed());
            ps.executeUpdate();
        }
    }

    public static PlayerStats loadStats(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_STATS)) {
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
        try (PreparedStatement ps = c.prepareStatement("SELECT name FROM " + STATS + " WHERE LOWER(name) LIKE ? ESCAPE '!' ORDER BY name ASC LIMIT ?")) {
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
