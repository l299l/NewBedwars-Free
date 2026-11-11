package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.game.GameControl;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.gamelog.GameLog;
import com.l299l.newbedwars.panel.gamelog.GameLogService;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class GameRoutes {
    private static final int LOG_PAGE = 500;
    private static final Pattern COLOR_CODE = Pattern.compile("(?i)[&\u00a7][0-9a-fk-orx]");

    private interface Call {
        CompletableFuture<MaintenanceResult> run(GameControl control, String arena, String actor);
    }

    private static final Map<String, Call> ACTIONS = Map.of(
            "start", GameControl::start,
            "forcestart", GameControl::forceStart,
            "stop", GameControl::stop,
            "forcestop", GameControl::forceStop,
            "nextphase", GameControl::nextPhase);

    private final GameControl control;
    private final GameLogService log;
    private final AuditLog audit;

    public GameRoutes(GameControl control, GameLogService log, AuditLog audit) {
        this.control = control;
        this.log = log;
        this.audit = audit;
    }

    public void register(Router router) {
        Set<String> manage = Set.of(Permission.GAME_MANAGE);
        for (Map.Entry<String, Call> action : ACTIONS.entrySet()) {
            router.allowed("POST", "/api/arenas/{name}/game/" + action.getKey(), manage,
                    r -> act(r, action.getKey(), action.getValue()));
        }
        router.allowed("POST", "/api/arenas/{name}/broadcast", manage, this::broadcast);
        router.allowed("GET", "/api/arenas/{name}/log", Set.of(Permission.GAME_LOG), this::log);
    }

    static String actor(Request r) {
        return "Panel: " + r.user();
    }

    private Response act(Request r, String action, Call call) throws Exception {
        MaintenanceResult result = await(call.run(control, r.param("name"), actor(r)));
        audit.record(r.user(), r.ip(), "game." + action, r.param("name"), result.code().name());
        if (result.isSuccess()) log.admin(result.arena(), r.user(), action, null);
        return Response.ok(json(result));
    }

    private Response broadcast(Request r) throws Exception {
        String message = r.required("message");
        MaintenanceResult result = await(control.broadcast(r.param("name"), actor(r), message));
        audit.record(r.user(), r.ip(), "game.broadcast", r.param("name"), result.code().name());
        if (result.isSuccess()) log.admin(result.arena(), r.user(), "broadcast", COLOR_CODE.matcher(message).replaceAll(""));
        return Response.ok(json(result));
    }

    private Response log(Request r) {
        long after = 0;
        String raw = r.query("after");
        if (raw != null) {
            try {
                after = Math.max(0, Long.parseLong(raw.trim()));
            } catch (NumberFormatException e) {
                throw new HttpError(400, "bad_number");
            }
        }
        GameLog.Page page = log.log().after(r.param("name"), after, LOG_PAGE);
        JsonArray entries = new JsonArray();
        for (GameLog.Entry e : page.entries()) {
            JsonObject o = new JsonObject();
            o.addProperty("seq", e.seq());
            o.addProperty("time", e.time());
            o.addProperty("type", e.type().name());
            if (e.player() != null) o.addProperty("player", e.player());
            if (e.team() != null) o.addProperty("team", e.team());
            if (e.teamColor() != null) o.addProperty("teamColor", e.teamColor());
            if (e.target() != null) o.addProperty("target", e.target());
            if (e.targetColor() != null) o.addProperty("targetColor", e.targetColor());
            if (e.detail() != null) o.addProperty("detail", e.detail());
            entries.add(o);
        }
        JsonObject body = new JsonObject();
        body.addProperty("seq", page.seq());
        body.addProperty("truncated", page.truncated());
        body.add("entries", entries);
        return Response.ok(body);
    }

    private static MaintenanceResult await(CompletableFuture<MaintenanceResult> future) throws Exception {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) throw cause;
            throw e;
        } catch (TimeoutException e) {
            throw new HttpError(504, "timeout");
        }
    }

    private static JsonObject json(MaintenanceResult result) {
        JsonObject o = new JsonObject();
        o.addProperty("ok", result.isSuccess());
        o.addProperty("code", result.code().name());
        o.addProperty("arena", result.arena());
        if (result.message() != null) o.addProperty("message", result.message());
        return o;
    }
}
