package com.l299l.newbedwars.config.data.mysql.repos;

import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.mysql.MySQLManager;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;

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

    public void updatePlayerShopGui(String name, String shopGui) {
        String sql = "INSERT INTO Players (name, shopGui) VALUES (?, ?) ON DUPLICATE KEY UPDATE shopGui=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, shopGui);
            stmt.setString(3, shopGui);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update player shopGui", e);
        }
    }

    public void updatePlayerUpgrades(String name, String upgradeGui) {
        String sql = "INSERT INTO Players (name, upgradeGui) VALUES (?, ?) ON DUPLICATE KEY UPDATE upgradeGui=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, upgradeGui);
            stmt.setString(3, upgradeGui);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update player upgradeGui", e);
        }
    }

    public List<PlayerModel> findAll() {
        List<PlayerModel> players = new ArrayList<>();
        String sql = "SELECT name, lang, shopGui, upgradeGui FROM Players";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                players.add(new PlayerModel(
                        rs.getString("name"),
                        Language.valueOf(rs.getString("lang")),
                        rs.getString("shopGui"),
                        rs.getString("upgradeGui")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load players", e);
        }
        return players;
    }

    public void close() {}
}
