package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class PlayerManager {
    private final HashMap<String, PlayerIns> players;

    public PlayerManager() {
        players = new HashMap<>();
    }

    public void load() {

    }

    public void save() {
        NewBedwars.plugin.getDataManager().savePlayerData();
    }

    public void addPlayer(PlayerIns player) {
        players.put(player.name(), player);
    }

    public void addPlayer(PlayerModel player) {
        UUID playerId = Objects.requireNonNull(Bukkit.getPlayer(player.getName())).getUniqueId();
        players.put(player.getName(), new PlayerIns(playerId, player.getName(), player.getLang(), player.getShopGui(), player.getUpgradeGui()));
    }

    public void addPlayer(Player player, Language lang) {
        players.put(player.getName(), new PlayerIns(player.getUniqueId(), player.getName(), lang, Properties.DefaultTeamShopGui, Properties.DefaultUpgradeShopGui));
    }

    public void removePlayer(String name) {
        players.remove(name);
    }

    public PlayerIns getPlayer(String name) {
        return players.get(name);
    }

    public void updatePlayerLanguage(String name, Language language) {
        PlayerIns player = players.get(name);
        if (player == null) {
            Player bukkit = Bukkit.getPlayer(name);
            UUID id = bukkit != null ? bukkit.getUniqueId() : UUID.randomUUID();
            players.put(name, new PlayerIns(id, name, language,
                    Properties.DefaultTeamShopGui, Properties.DefaultUpgradeShopGui));
            return;
        }
        players.put(name, new PlayerIns(player.id(), player.name(), language, player.shopGui(), player.upgradeGui()));
    }

    public void updatePlayerShopGui(String name, String shopGui) {
        PlayerIns player = players.get(name);
        if (player == null) return;
        player = new PlayerIns(player.id(), player.name(), player.language(), shopGui, player.upgradeGui());
        players.put(name, player);
    }

    public void updatePlayerUpgradeGui(String name, String upgradeGui) {
        PlayerIns player = players.get(name);
        if (player == null) return;
        player = new PlayerIns(player.id(), player.name(), player.language(), player.shopGui(), upgradeGui);
        players.put(name, player);
    }

    public HashMap<String, PlayerIns> getPlayers() {
        return players;
    }


}
