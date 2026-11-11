package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.api.view.LeaderboardEntry;
import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class StatsRoutes {
    private static final long TIMEOUT_SECONDS = 20;
    private static final int MAX_NAME = 32;

    private final NewBedwarsAPI api;

    public StatsRoutes(NewBedwarsAPI api) {
        this.api = api;
    }

    public void register(Router router) {
        Set<String> view = Set.of(Permission.STATS_VIEW);
        router.allowed("GET", "/api/leaderboard", view, this::leaderboard);
        router.allowed("GET", "/api/players", view, this::search);
        router.allowed("GET", "/api/players/{name}", view, this::player);
    }

    static JsonObject stats(PlayerStatsView s) {
        JsonObject o = new JsonObject();
        for (StatType type : StatType.values()) o.addProperty(type.name().toLowerCase(Locale.ROOT), type.of(s));
        return o;
    }

    private static int number(String raw, int fallback) {
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new HttpError(400, "bad_number");
        }
    }

    private Response leaderboard(Request r) throws Exception {
        StatType stat;
        try {
            stat = StatType.valueOf(Optional.ofNullable(r.query("stat")).orElse("WINS").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new HttpError(400, "unknown_stat");
        }
        int limit = Math.max(1, Math.min(100, number(r.query("limit"), 25)));
        List<LeaderboardEntry> top = await(api.getLeaderboard(stat, limit));
        JsonArray rows = new JsonArray();
        int rank = 0;
        for (LeaderboardEntry entry : top) {
            JsonObject o = stats(entry.getStats());
            o.addProperty("rank", ++rank);
            o.addProperty("name", entry.getName());
            rows.add(o);
        }
        JsonObject body = new JsonObject();
        body.addProperty("stat", stat.name());
        body.add("entries", rows);
        return Response.ok(body);
    }

    private Response search(Request r) throws Exception {
        String q = Optional.ofNullable(r.query("q")).orElse("").trim();
        if (q.length() > MAX_NAME) throw new HttpError(400, "bad_name");
        JsonArray names = new JsonArray();
        if (!q.isEmpty()) await(api.findPlayers(q, 20)).forEach(names::add);
        JsonObject body = new JsonObject();
        body.add("players", names);
        return Response.ok(body);
    }

    private Response player(Request r) throws Exception {
        String name = r.param("name");
        if (name == null || name.isBlank() || name.length() > MAX_NAME) throw new HttpError(400, "bad_name");
        Optional<PlayerStatsView> stats = await(api.loadStats(name));
        if (stats.isEmpty()) throw new HttpError(404, "player_not_found");
        JsonObject body = stats(stats.get());
        body.addProperty("name", name);
        return Response.ok(body);
    }

    static <T> T await(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new HttpError(503, "stats_unavailable");
        } catch (TimeoutException e) {
            throw new HttpError(504, "timeout");
        }
    }
}
