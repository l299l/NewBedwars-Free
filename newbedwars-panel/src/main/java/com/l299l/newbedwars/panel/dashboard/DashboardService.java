package com.l299l.newbedwars.panel.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.api.view.ArenaGroupView;
import com.l299l.newbedwars.api.view.ArenaView;
import com.l299l.newbedwars.api.view.NetworkArenaView;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.api.view.TeamView;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class DashboardService {
    private static final int MAX_PLAYERS_LISTED = 200;
    private static final long MB = 1024L * 1024L;
    private static final Map<ChatColor, String> COLORS = new HashMap<>();

    static {
        String[] hex = {"#000000", "#0000AA", "#00AA00", "#00AAAA", "#AA0000", "#AA00AA", "#FFAA00", "#AAAAAA",
                "#555555", "#5555FF", "#55FF55", "#55FFFF", "#FF5555", "#FF55FF", "#FFFF55", "#FFFFFF"};
        ChatColor[] colors = {ChatColor.BLACK, ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
                ChatColor.DARK_RED, ChatColor.DARK_PURPLE, ChatColor.GOLD, ChatColor.GRAY, ChatColor.DARK_GRAY,
                ChatColor.BLUE, ChatColor.GREEN, ChatColor.AQUA, ChatColor.RED, ChatColor.LIGHT_PURPLE,
                ChatColor.YELLOW, ChatColor.WHITE};
        for (int i = 0; i < colors.length; i++) COLORS.put(colors[i], hex[i]);
    }

    private final Plugin plugin;
    private final NewBedwarsAPI api;
    private final DashboardState state = new DashboardState(System::currentTimeMillis);
    private final long startedAt = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getUptime();
    private BukkitTask task;

    public DashboardService(Plugin plugin, NewBedwarsAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state.polledRecently()) state.update(build());
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    public DashboardState.Snapshot snapshot() throws Exception {
        if (state.poll()) {
            CompletableFuture<Void> fresh = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    state.update(build());
                    fresh.complete(null);
                } catch (RuntimeException e) {
                    fresh.completeExceptionally(e);
                }
            });
            fresh.get(5, TimeUnit.SECONDS);
        }
        return state.latest();
    }

    private JsonObject build() {
        JsonObject root = new JsonObject();
        root.add("server", server());
        Map<String, String> groupOf = new HashMap<>();
        for (ArenaGroupView group : api.getGroups()) {
            for (String arena : group.getArenaNames()) groupOf.putIfAbsent(arena.toLowerCase(), group.getName());
        }
        JsonArray arenas = new JsonArray();
        for (ArenaView arena : api.getArenas()) arenas.add(arena(arena, groupOf.get(arena.getName().toLowerCase())));
        root.add("arenas", arenas);
        JsonArray network = new JsonArray();
        if (api.getNetworkMode() != NetworkMode.NONE) {
            for (NetworkArenaView arena : api.getNetworkArenas()) network.add(networkArena(arena));
        }
        root.add("network", network);
        JsonArray players = new JsonArray();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.size() >= MAX_PLAYERS_LISTED) break;
            JsonObject p = new JsonObject();
            p.addProperty("name", player.getName());
            api.getArena(player).ifPresent(a -> p.addProperty("arena", a.getName()));
            players.add(p);
        }
        root.add("players", players);
        return root;
    }

    private JsonObject server() {
        JsonObject s = new JsonObject();
        s.addProperty("software", Bukkit.getName() + " " + Bukkit.getBukkitVersion());
        s.addProperty("plugin", api.getVersion());
        s.addProperty("panel", plugin.getDescription().getVersion());
        s.addProperty("networkMode", api.getNetworkMode().name());
        api.getServerId().ifPresent(id -> s.addProperty("serverId", id));
        s.addProperty("online", Bukkit.getOnlinePlayers().size());
        s.addProperty("maxPlayers", Bukkit.getMaxPlayers());
        s.addProperty("startedAt", startedAt);
        JsonArray tps = new JsonArray();
        try {
            for (double value : Bukkit.getTPS()) tps.add(Math.round(Math.min(20.0, value) * 10.0) / 10.0);
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        }
        s.add("tps", tps);
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        s.addProperty("memoryUsedMb", (used / MB) / 32 * 32);
        s.addProperty("memoryMaxMb", runtime.maxMemory() / MB);
        return s;
    }

    private JsonObject arena(ArenaView arena, String group) {
        JsonObject a = new JsonObject();
        a.addProperty("name", arena.getName());
        a.addProperty("state", arena.getState().name());
        a.addProperty("enabled", arena.isEnabled());
        if (group != null) a.addProperty("group", group);
        a.addProperty("players", arena.getPlayers().size());
        a.addProperty("spectators", arena.getSpectators().size());
        a.addProperty("minPlayers", arena.getMinPlayers());
        a.addProperty("maxPlayers", arena.getTeams().size() * arena.getMaxInTeam());
        a.addProperty("maxInTeam", arena.getMaxInTeam());
        if (arena.getCurrentPhase() != null) a.addProperty("phase", arena.getCurrentPhase());
        a.addProperty("gameTime", arena.getGameTime());
        JsonArray problems = new JsonArray();
        arena.getLoadProblems().forEach(problems::add);
        a.add("problems", problems);
        JsonArray teams = new JsonArray();
        for (TeamView team : arena.getTeams()) {
            JsonObject t = new JsonObject();
            t.addProperty("name", team.getName());
            t.addProperty("color", COLORS.getOrDefault(team.getColor(), "#FFFFFF"));
            t.addProperty("players", team.getPlayers().size());
            t.addProperty("alive", team.isAlive());
            t.addProperty("bed", !team.isBedDestroyed());
            teams.add(t);
        }
        a.add("teams", teams);
        return a;
    }

    private static JsonObject networkArena(NetworkArenaView arena) {
        JsonObject a = new JsonObject();
        a.addProperty("server", arena.getServer());
        a.addProperty("name", arena.getName());
        arena.getGroup().ifPresent(g -> a.addProperty("group", g));
        a.addProperty("state", arena.getState().map(Enum::name).orElse("UNKNOWN"));
        a.addProperty("players", arena.getPlayerCount());
        a.addProperty("maxPlayers", arena.getMaxPlayers());
        a.addProperty("joinable", arena.isJoinable());
        return a;
    }
}
