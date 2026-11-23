package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.l299l.newbedwars.api.mode.ArenaModeInfo;
import com.l299l.newbedwars.api.mode.EditResult;
import com.l299l.newbedwars.api.mode.GeneratorConfigInfo;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ModeRoutes {
    static final int MAX_YAML_BYTES = 48 * 1024;
    static final int MAX_PHASES = 100;
    static final int MAX_ACTIONS = 50;
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");

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
        Set<String> edit = Set.of(Permission.MODES_EDIT);
        Set<String> view = Set.of(Permission.MODES_VIEW);
        router.allowed("PUT", "/api/modes/{id}", edit, this::saveMode);
        router.allowed("DELETE", "/api/modes/{id}", edit, r -> delete(r, "modes.delete", modes::deleteMode));
        router.allowed("GET", "/api/phases", view, this::phaseLists);
        router.allowed("PUT", "/api/phases/{id}", edit, this::savePhases);
        router.allowed("DELETE", "/api/phases/{id}", edit, r -> delete(r, "phases.delete", modes::deletePhaseList));
        router.allowed("GET", "/api/generators", view, this::generatorFiles);
        router.allowed("GET", "/api/generators/{id}", view, this::generatorText);
        router.allowed("PUT", "/api/generators/{id}", edit, this::saveGenerator);
        router.allowed("DELETE", "/api/generators/{id}", edit, r -> delete(r, "generators.delete", modes::deleteGeneratorConfig));
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

    private static String id(Request r) {
        String id = r.param("id");
        if (id == null || !ID.matcher(id).matches()) throw new HttpError(400, "invalid_id");
        return id;
    }

    private static String optional(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return null;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new HttpError(400, "bad_value");
        String value = e.getAsString().trim();
        return value.isEmpty() ? null : value;
    }

    private static int number(JsonObject o, String key, int max) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return 0;
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) throw new HttpError(400, "bad_value");
        long value;
        try {
            value = e.getAsBigDecimal().longValueExact();
        } catch (ArithmeticException ex) {
            throw new HttpError(400, "bad_value");
        }
        if (value < 0 || value > max) throw new HttpError(400, "bad_value");
        return (int) value;
    }

    private Response saveMode(Request r) throws Exception {
        String id = id(r);
        JsonObject j = r.json();
        String tablist = j.has("tablist") ? optional(j, "tablist") : currentTablist(id);
        ModeInfo info = new ModeInfo(id, optional(j, "generators"), optional(j, "gamePhases"), optional(j, "shopGui"),
                optional(j, "upgradeGui"), number(j, "maxInTeam", 64), number(j, "teams", 64), false, tablist);
        return result(r, "modes.save", id, await(modes.saveMode(info)));
    }

    private String currentTablist(String id) throws Exception {
        for (ModeInfo mode : await(modes.getModes())) {
            if (mode.id().equalsIgnoreCase(id)) return mode.tablist();
        }
        return null;
    }

    static List<PhaseInfo> parsePhases(JsonObject j) {
        JsonElement e = j.get("phases");
        if (e == null || !e.isJsonArray()) throw new HttpError(400, "bad_value");
        JsonArray array = e.getAsJsonArray();
        if (array.size() > MAX_PHASES) throw new HttpError(400, "bad_value");
        List<PhaseInfo> out = new ArrayList<>();
        for (JsonElement item : array) {
            if (!item.isJsonObject()) throw new HttpError(400, "bad_value");
            JsonObject o = item.getAsJsonObject();
            String name = optional(o, "name");
            if (name == null || name.length() > 64) throw new HttpError(400, "bad_value");
            int duration = number(o, "duration", 24 * 3600);
            JsonElement actionsElement = o.get("actions");
            List<String> actions = new ArrayList<>();
            if (actionsElement != null && !actionsElement.isJsonNull()) {
                if (!actionsElement.isJsonArray() || actionsElement.getAsJsonArray().size() > MAX_ACTIONS) throw new HttpError(400, "bad_value");
                for (JsonElement a : actionsElement.getAsJsonArray()) {
                    if (!a.isJsonPrimitive() || !a.getAsJsonPrimitive().isString()) throw new HttpError(400, "bad_value");
                    String action = a.getAsString().trim();
                    if (action.isEmpty() || action.length() > 64) throw new HttpError(400, "bad_value");
                    actions.add(action);
                }
            }
            out.add(new PhaseInfo(name, duration, actions));
        }
        return out;
    }

    private Response savePhases(Request r) throws Exception {
        String id = id(r);
        PhaseListInfo list = new PhaseListInfo(id, parsePhases(r.json()));
        return result(r, "phases.save", id, await(modes.savePhaseList(list)));
    }

    private Response phaseLists(Request r) throws Exception {
        JsonArray listArray = new JsonArray();
        for (PhaseListInfo list : await(modes.getPhaseLists())) {
            JsonObject o = new JsonObject();
            o.addProperty("id", list.id());
            o.add("phases", phases(list));
            listArray.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("phaseLists", listArray);
        return Response.ok(body);
    }

    private Response generatorFiles(Request r) throws Exception {
        JsonArray array = new JsonArray();
        for (GeneratorConfigInfo info : await(modes.getGeneratorConfigFiles())) {
            JsonObject o = new JsonObject();
            o.addProperty("id", info.id());
            o.addProperty("fileName", info.fileName());
            o.addProperty("builtIn", info.builtIn());
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("generators", array);
        return Response.ok(body);
    }

    private Response generatorText(Request r) throws Exception {
        String id = id(r);
        String yaml = await(modes.getGeneratorConfigText(id));
        if (yaml == null) return Response.error(404, "not_found");
        JsonObject body = new JsonObject();
        body.addProperty("id", id);
        body.addProperty("yaml", yaml);
        return Response.ok(body);
    }

    private Response saveGenerator(Request r) throws Exception {
        String id = id(r);
        String yaml = r.string("yaml");
        if (yaml == null || yaml.isBlank()) throw new HttpError(400, "missing_yaml");
        if (yaml.getBytes(StandardCharsets.UTF_8).length > MAX_YAML_BYTES) throw new HttpError(413, "yaml_too_large");
        return result(r, "generators.save", id, await(modes.saveGeneratorConfig(id, yaml)));
    }

    private Response delete(Request r, String action, java.util.function.Function<String, CompletableFuture<EditResult>> call) throws Exception {
        String id = id(r);
        return result(r, action, id, await(call.apply(id)));
    }

    private Response result(Request r, String action, String id, EditResult result) {
        audit.record(r.user(), r.ip(), action, id, result.code().name());
        JsonObject o = new JsonObject();
        o.addProperty("code", result.code().name());
        JsonArray problems = new JsonArray();
        result.problems().forEach(problems::add);
        o.add("problems", problems);
        JsonArray usedBy = new JsonArray();
        result.usedBy().forEach(usedBy::add);
        o.add("usedBy", usedBy);
        int status = switch (result.code()) {
            case OK -> 200;
            case NOT_FOUND -> 404;
            case INVALID -> 400;
            case IN_USE, BUILT_IN -> 409;
            default -> 500;
        };
        if (status != 200) o.addProperty("error", "edit_" + result.code().name().toLowerCase(java.util.Locale.ROOT));
        return Response.json(status, o);
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
