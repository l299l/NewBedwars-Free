package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PlayerManager {
    private final HashMap<String, PlayerIns> players;
    private final HashMap<String, PlayerStats> playerStats;

    public PlayerManager() {
        players = new HashMap<>();
        playerStats = new HashMap<>();
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
        players.put(player.getName(), new PlayerIns(playerId, player.getName(), player.getLang(), PlayerIns.defaultFastBuy()));
    }

    public void addPlayer(Player player, Language lang) {
        players.put(player.getName(), new PlayerIns(player.getUniqueId(), player.getName(), lang, PlayerIns.defaultFastBuy()));
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
            players.put(name, new PlayerIns(id, name, language, PlayerIns.defaultFastBuy()));
            return;
        }
        players.put(name, new PlayerIns(player.id(), player.name(), language, player.fastBuyPerCategory()));
    }

    public void updateFastBuy(String name, Map<String, List<String>> fastBuyPerCategory) {
        PlayerIns player = players.get(name);
        if (player == null) return;
        players.put(name, new PlayerIns(player.id(), player.name(), player.language(), fastBuyPerCategory));
        save();
    }

    public HashMap<String, PlayerIns> getPlayers() {
        return players;
    }

    public PlayerStats getStats(String name) {
        return playerStats.getOrDefault(name, PlayerStats.empty());
    }

    public void addStats(String name, PlayerStats stats) {
        playerStats.put(name, stats);
    }

    public void addGameResult(String name, GamePlayer gp, boolean won) {
        PlayerStats current = playerStats.getOrDefault(name, PlayerStats.empty());
        playerStats.put(name, current.addGameResult(
                gp.getKills(), gp.getDeaths(), gp.getFinalKills(), gp.getBedsBroken(), won));
    }

    public HashMap<String, PlayerStats> getAllStats() {
        return playerStats;
    }

    public List<Map.Entry<String, PlayerStats>> getTopByWins(int limit) {
        List<Map.Entry<String, PlayerStats>> list = new ArrayList<>(playerStats.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> e.getValue().wins()).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, PlayerStats>> getTopByKills(int limit) {
        List<Map.Entry<String, PlayerStats>> list = new ArrayList<>(playerStats.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> e.getValue().kills()).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, PlayerStats>> getTopByBeds(int limit) {
        List<Map.Entry<String, PlayerStats>> list = new ArrayList<>(playerStats.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> e.getValue().bedsBroken()).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, PlayerStats>> getTopByFinalKills(int limit) {
        List<Map.Entry<String, PlayerStats>> list = new ArrayList<>(playerStats.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> e.getValue().finalKills()).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }
}
