package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.config.Language;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

public class PlayerManager {
    private final HashMap<String, PlayerProfile> players;
    private final HashMap<String, PlayerStats> playerStats;
    /** {@code Storage.Type: MYSQL}: leaderboards read from the database ("wins", "kills", "beds", "finalKills"). */
    private Map<String, List<Map.Entry<String, PlayerStats>>> sharedLeaderboards;

    public PlayerManager() {
        players = new HashMap<>();
        playerStats = new HashMap<>();
    }

    public void load() {

    }

    public void save() {
        NewBedwars.plugin.getDataManager().savePlayerData();
    }

    public void addPlayer(PlayerProfile player) {
        players.put(player.name(), player);
    }

    public void addPlayer(Player player, Language lang) {
        players.put(player.getName(), new PlayerProfile(player.getUniqueId(), player.getName(), lang, PlayerProfile.defaultFastBuy()));
    }

    public void removePlayer(String name) {
        players.remove(name);
    }

    public PlayerProfile getPlayer(String name) {
        return players.get(name);
    }

    public void updatePlayerLanguage(String name, Language language) {
        PlayerProfile player = players.get(name);
        if (player == null) {
            Player bukkit = Bukkit.getPlayer(name);
            UUID id = bukkit != null ? bukkit.getUniqueId() : Bukkit.getOfflinePlayer(name).getUniqueId();
            players.put(name, new PlayerProfile(id, name, language, PlayerProfile.defaultFastBuy()));
        } else {
            players.put(name, new PlayerProfile(player.id(), player.name(), language, player.fastBuyPerCategory()));
        }
        if (NewBedwars.plugin != null && NewBedwars.plugin.getDataManager() != null) {
            NewBedwars.plugin.getDataManager().playerChanged(name);
        }
    }

    public void updateFastBuy(String name, Map<String, List<String>> fastBuyPerCategory) {
        PlayerProfile player = players.get(name);
        if (player == null) return;
        players.put(name, new PlayerProfile(player.id(), player.name(), player.language(), fastBuyPerCategory));
        save();
    }

    public HashMap<String, PlayerProfile> getPlayers() {
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

    /** MySQL storage: use these leaderboards (shared by the whole network) instead of the local stats. */
    public void setSharedLeaderboards(Map<String, List<Map.Entry<String, PlayerStats>>> boards) {
        this.sharedLeaderboards = boards;
    }

    private List<Map.Entry<String, PlayerStats>> shared(String key, int limit) {
        if (sharedLeaderboards == null) return null;
        List<Map.Entry<String, PlayerStats>> list = sharedLeaderboards.get(key);
        return list == null ? null : list.subList(0, Math.min(limit, list.size()));
    }

    /** The {@code limit} players with the highest {@code stat}, highest first. */
    public List<Map.Entry<String, PlayerStats>> getTopBy(ToIntFunction<PlayerStats> stat, int limit) {
        List<Map.Entry<String, PlayerStats>> list = new ArrayList<>(playerStats.entrySet());
        list.sort(Comparator.comparingInt((Map.Entry<String, PlayerStats> e) -> stat.applyAsInt(e.getValue())).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, PlayerStats>> getTopByWins(int limit) {
        List<Map.Entry<String, PlayerStats>> shared = shared("wins", limit);
        if (shared != null) return shared;
        return getTopBy(PlayerStats::wins, limit);
    }

    public List<Map.Entry<String, PlayerStats>> getTopByKills(int limit) {
        List<Map.Entry<String, PlayerStats>> shared = shared("kills", limit);
        if (shared != null) return shared;
        return getTopBy(PlayerStats::kills, limit);
    }

    public List<Map.Entry<String, PlayerStats>> getTopByBeds(int limit) {
        List<Map.Entry<String, PlayerStats>> shared = shared("beds", limit);
        if (shared != null) return shared;
        return getTopBy(PlayerStats::bedsBroken, limit);
    }

    public List<Map.Entry<String, PlayerStats>> getTopByFinalKills(int limit) {
        List<Map.Entry<String, PlayerStats>> shared = shared("finalKills", limit);
        if (shared != null) return shared;
        return getTopBy(PlayerStats::finalKills, limit);
    }
}
