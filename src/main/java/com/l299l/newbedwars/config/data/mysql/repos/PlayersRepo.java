package com.l299l.newbedwars.config.data.mysql.repos;

import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.mysql.MySQLManager;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;
import com.l299l.newbedwars.player.PlayerStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayersRepo {
    private final Connection connection;

    public PlayersRepo(MySQLManager mySQLManager) {
        this.connection = mySQLManager.getConnection();
        ensureStatsColumns();
    }

    private void ensureStatsColumns() {
        String[] columns = {"wins INT DEFAULT 0", "losses INT DEFAULT 0", "kills INT DEFAULT 0",
                "deaths INT DEFAULT 0", "final_kills INT DEFAULT 0", "beds_broken INT DEFAULT 0",
                "games_played INT DEFAULT 0"};
        for (String col : columns) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "ALTER TABLE Players ADD COLUMN IF NOT EXISTS " + col)) {
                stmt.executeUpdate();
            } catch (SQLException ignored) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "ALTER TABLE Players ADD COLUMN " + col)) {
                    stmt.executeUpdate();
                } catch (SQLException e2) {
                    // Column already exists — ignore
                }
            }
        }
    }

    public void updatePlayerLanguage(String name, Language lang) {
        String sql = "INSERT INTO Players (name, lang) VALUES (?, ?) ON DUPLICATE KEY UPDATE lang=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, lang.name());
            stmt.setString(3, lang.name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update player language", e);
        }
    }

    public List<PlayerModel> findAll() {
        List<PlayerModel> players = new ArrayList<>();
        String sql = "SELECT name, lang, wins, losses, kills, deaths, final_kills, beds_broken, games_played FROM Players";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                PlayerModel m = new PlayerModel(
                        rs.getString("name"),
                        Language.valueOf(rs.getString("lang"))
                );
                m.setWins(rs.getInt("wins"));
                m.setLosses(rs.getInt("losses"));
                m.setKills(rs.getInt("kills"));
                m.setDeaths(rs.getInt("deaths"));
                m.setFinalKills(rs.getInt("final_kills"));
                m.setBedsBroken(rs.getInt("beds_broken"));
                m.setGamesPlayed(rs.getInt("games_played"));
                players.add(m);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load players", e);
        }
        return players;
    }

    public void updateStats(String name, PlayerStats stats) {
        String sql = "INSERT INTO Players (name, wins, losses, kills, deaths, final_kills, beds_broken, games_played) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE wins=?, losses=?, kills=?, deaths=?, final_kills=?, beds_broken=?, games_played=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, stats.wins());
            stmt.setInt(3, stats.losses());
            stmt.setInt(4, stats.kills());
            stmt.setInt(5, stats.deaths());
            stmt.setInt(6, stats.finalKills());
            stmt.setInt(7, stats.bedsBroken());
            stmt.setInt(8, stats.gamesPlayed());
            stmt.setInt(9, stats.wins());
            stmt.setInt(10, stats.losses());
            stmt.setInt(11, stats.kills());
            stmt.setInt(12, stats.deaths());
            stmt.setInt(13, stats.finalKills());
            stmt.setInt(14, stats.bedsBroken());
            stmt.setInt(15, stats.gamesPlayed());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update player stats", e);
        }
    }

    public void close() {}
}
