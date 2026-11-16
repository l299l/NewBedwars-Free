package com.l299l.newbedwars.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.config.Language;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

public class PlayerManager {
    private final HashMap<UUID, PlayerProfile> players = new HashMap<>();
    private final HashMap<UUID, PlayerStats> playerStats = new HashMap<>();
    private final HashMap<UUID, String> names = new HashMap<>();
    private final HashMap<String, UUID> ids = new HashMap<>();
    private Map<String, List<Map.Entry<String, PlayerStats>>> sharedLeaderboards;

    public void load() {

    }

    public void save() {
        NewBedwars.plugin.getDataManager().savePlayerData();
    }

    public static UUID offlineId(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public void identify(UUID id, String name) {
        if (id == null || name == null) return;
        String old = names.put(id, name);
        if (old != null && !old.equalsIgnoreCase(name) && id.equals(ids.get(key(old)))) ids.remove(key(old));
        ids.put(key(name), id);
        PlayerProfile profile = players.get(id);
        if (profile != null && !name.equals(profile.name())) {
            players.put(id, new PlayerProfile(id, name, profile.language(), profile.fastBuyPerCategory()));
        }
    }

    private void remember(UUID id, String name) {
        if (id == null || name == null) return;
        names.putIfAbsent(id, name);
        ids.putIfAbsent(key(name), id);
    }

    public UUID idOf(String name) {
        return name == null ? null : ids.get(key(name));
    }

    public String nameOf(UUID id) {
        return id == null ? null : names.get(id);
    }

    public void addPlayer(PlayerProfile player) {
        if (player.id() == null) return;
        players.put(player.id(), player);
        remember(player.id(), player.name());
    }

    public void addPlayer(Player player, Language lang) {
        identify(player.getUniqueId(), player.getName());
        players.put(player.getUniqueId(), new PlayerProfile(player.getUniqueId(), player.getName(), lang, PlayerProfile.defaultFastBuy()));
    }

    public void removePlayer(String name) {
        UUID id = idOf(name);
        if (id != null) players.remove(id);
    }

    public PlayerProfile getPlayer(String name) {
        UUID id = idOf(name);
        return id == null ? null : players.get(id);
    }

    public PlayerProfile getPlayer(UUID id) {
        return id == null ? null : players.get(id);
    }

    public void updatePlayerLanguage(String name, Language language) {
        UUID id = idOf(name);
        if (id == null) {
            Player bukkit = Bukkit.getPlayer(name);
            id = bukkit != null ? bukkit.getUniqueId() : offlineId(name);
            identify(id, bukkit != null ? bukkit.getName() : name);
        }
        PlayerProfile player = players.get(id);
        if (player == null) {
            players.put(id, new PlayerProfile(id, nameOf(id), language, PlayerProfile.defaultFastBuy()));
        } else {
            players.put(id, new PlayerProfile(id, player.name(), language, player.fastBuyPerCategory()));
        }
        if (NewBedwars.plugin != null && NewBedwars.plugin.getDataManager() != null) {
            NewBedwars.plugin.getDataManager().playerChanged(name);
        }
    }

    public void updateFastBuy(String name, Map<String, List<String>> fastBuyPerCategory) {
        PlayerProfile player = getPlayer(name);
        if (player == null) return;
        if (player.fastBuyPerCategory().equals(fastBuyPerCategory)) return;
        players.put(player.id(), new PlayerProfile(player.id(), player.name(), player.language(), fastBuyPerCategory));
        save();
    }

    public HashMap<UUID, PlayerProfile> getPlayers() {
        return players;
    }

    public PlayerStats getStats(String name) {
        return getStats(idOf(name));
    }

    public PlayerStats getStats(UUID id) {
        return id == null ? PlayerStats.empty() : playerStats.getOrDefault(id, PlayerStats.empty());
    }

    public void addStats(UUID id, String name, PlayerStats stats) {
        if (id == null) return;
        remember(id, name);
        playerStats.put(id, stats);
    }

    public void mergeStats(UUID id, String name, PlayerStats stats) {
        if (id == null) return;
        remember(id, name);
        playerStats.merge(id, stats, PlayerStats::plus);
    }

    public void addGameResult(UUID id, String name, GamePlayer gp, boolean won) {
        if (id == null) return;
        identify(id, name);
        PlayerStats current = playerStats.getOrDefault(id, PlayerStats.empty());
        playerStats.put(id, current.addGameResult(
                gp.getKills(), gp.getDeaths(), gp.getFinalKills(), gp.getBedsBroken(), won));
    }

    public HashMap<UUID, PlayerStats> getAllStats() {
        return playerStats;
    }

    public List<Map.Entry<String, PlayerStats>> namedStats() {
        List<Map.Entry<String, PlayerStats>> out = new ArrayList<>();
        for (Map.Entry<UUID, PlayerStats> e : playerStats.entrySet()) {
            String name = nameOf(e.getKey());
            out.add(new AbstractMap.SimpleImmutableEntry<>(name != null ? name : e.getKey().toString(), e.getValue()));
        }
        return out;
    }

    public void setSharedLeaderboards(Map<String, List<Map.Entry<String, PlayerStats>>> boards) {
        this.sharedLeaderboards = boards;
    }

    private List<Map.Entry<String, PlayerStats>> shared(String key, int limit) {
        if (sharedLeaderboards == null) return null;
        List<Map.Entry<String, PlayerStats>> list = sharedLeaderboards.get(key);
        return list == null ? null : list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<String, PlayerStats>> getTopBy(ToIntFunction<PlayerStats> stat, int limit) {
        List<Map.Entry<String, PlayerStats>> list = namedStats();
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
