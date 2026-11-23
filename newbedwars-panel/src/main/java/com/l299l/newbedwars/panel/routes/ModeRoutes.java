package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.api.mode.ArenaModeInfo;
import com.l299l.newbedwars.api.mode.ModeInfo;
import com.l299l.newbedwars.api.mode.ModeResult;
import com.l299l.newbedwars.api.mode.ModeService;
import com.l299l.newbedwars.api.mode.PhaseInfo;
import com.l299l.newbedwars.api.mode.PhaseListInfo;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.dashboard.ModeMatch;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ModeRoutes {
    private final ModeService modes;
    private final AuditLog audit;

    public ModeRoutes(ModeService modes, AuditLog audit) {
        this.modes = modes;
        this.audit = audit;
    }

    public void register(Router router) {
        router.allowed("GET", "/api/modes", Set.of(Permission.MODES_VIEW), this::list);
        router.allowed("GET", "/api/modes/arenas", Set.of(Permission.MODES_VIEW), this::arenas);
        router.allowed("POST", "/api/arenas/{name}/mode", Set.of(Permission.MODES_APPLY), this::apply);
    }

    private Response list(Request r) throws Exception {
        List<ModeInfo> modeList = await(modes.getModes());
        List<PhaseListInfo> lists = await(modes.getPhaseLists());
        List<String> generators = await(modes.getGeneratorConfigs());
        JsonArray modeArray = new JsonArray();
        for (ModeInfo mode : modeList) {
            JsonObject o = new JsonObject();
            o.addProperty("id", mode.id());
            o.addProperty("generators", mode.generators());
            o.addProperty("gamePhases", mode.gamePhases());
            o.addProperty("shopGui", mode.shopGui());
            o.addProperty("upgradeGui", mode.upgradeGui());
            o.addProperty("maxInTeam", mode.maxInTeam());
            o.addProperty("teams", mode.teams());
            o.addProperty("builtIn", mode.builtIn());
            if (mode.gamePhases() != null) {
                for (PhaseListInfo list : lists) {
                    if (list.id().equalsIgnoreCase(mode.gamePhases())) {
                        o.add("phases", phases(list));
                        break;
                    }
                }
            }
            modeArray.add(o);
        }
        JsonArray listArray = new JsonArray();
        for (PhaseListInfo list : lists) {
            JsonObject o = new JsonObject();
            o.addProperty("id", list.id());
            o.add("phases", phases(list));
            listArray.add(o);
        }
        JsonArray configs = new JsonArray();
        generators.forEach(configs::add);
        JsonObject body = new JsonObject();
        body.add("modes", modeArray);
        body.add("phaseLists", listArray);
        body.add("generatorConfigs", configs);
        return Response.ok(body);
    }

    private Response arenas(Request r) throws Exception {
        List<ModeInfo> modeList = await(modes.getModes());
        List<ArenaModeInfo> infos = await(modes.getArenaModes());
        JsonArray array = new JsonArray();
        for (ArenaModeInfo info : infos) {
            JsonObject o = new JsonObject();
            o.addProperty("arena", info.arena());
            o.addProperty("waiting", info.waiting());
            o.addProperty("generators", info.generators());
            o.addProperty("gamePhases", info.gamePhases());
            o.addProperty("shopGui", info.shopGui());
            o.addProperty("upgradeGui", info.upgradeGui());
            o.addProperty("maxInTeam", info.maxInTeam());
            o.addProperty("teams", info.teams());
            o.addProperty("mode", ModeMatch.of(info, modeList));
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("arenas", array);
        return Response.ok(body);
    }

    private static JsonArray phases(PhaseListInfo list) {
        JsonArray out = new JsonArray();
        long start = 0;
        for (PhaseInfo phase : list.phases()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", phase.name());
            o.addProperty("duration", phase.duration());
            o.addProperty("start", start);
            JsonArray actions = new JsonArray();
            phase.actions().forEach(actions::add);
            o.add("actions", actions);
            out.add(o);
            start += phase.duration();
        }
        return out;
    }

    private Response apply(Request r) throws Exception {
        String mode = r.required("mode").trim();
        String arena = r.param("name");
        ModeResult result = await(modes.applyMode(arena, mode));
        audit.record(r.user(), r.ip(), "arena.mode", arena + " -> " + mode, result.code().name());
        switch (result.code()) {
            case OK -> {
                JsonObject o = new JsonObject();
                o.addProperty("ok", true);
                o.addProperty("teamsDiffer", result.teamsDiffer());
                return Response.ok(o);
            }
            case NOT_FOUND -> {
                return Response.error(404, "mode_not_found");
            }
            case MISSING_PARTS -> {
                JsonObject o = new JsonObject();
                o.addProperty("error", "mode_missing_parts");
                JsonArray missing = new JsonArray();
                result.missing().forEach(missing::add);
                o.add("missing", missing);
                return Response.json(400, o);
            }
            default -> {
                return Response.error(409, "arena_busy");
            }
        }
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof Exception cause) throw cause;
            throw ex;
        } catch (TimeoutException ex) {
            throw new HttpError(504, "timeout");
        }
    }
}
