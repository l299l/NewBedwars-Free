package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.command.UserCommands;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class AdminRoutes {
    private static final Map<UserCommands.Code, Integer> STATUS = Map.of(
            UserCommands.Code.EXISTS, 409,
            UserCommands.Code.NOT_FOUND, 404,
            UserCommands.Code.INVALID_NAME, 400,
            UserCommands.Code.UNKNOWN_ROLE, 400,
            UserCommands.Code.LAST_ADMIN, 409,
            UserCommands.Code.FORBIDDEN, 403,
            UserCommands.Code.SAVE_FAILED, 500);

    private final AccountStore accounts;
    private final UserCommands users;
    private final AuditLog audit;

    public AdminRoutes(AccountStore accounts, UserCommands users, AuditLog audit) {
        this.accounts = accounts;
        this.users = users;
        this.audit = audit;
    }

    public void register(Router router) {
        Set<String> manage = Set.of(Permission.USERS_MANAGE);
        router.allowed("GET", "/api/users", manage, r -> usersJson());
        router.allowed("POST", "/api/users", manage, r -> result(users.create(actor(r), r.required("name"), r.required("role"))));
        router.allowed("POST", "/api/users/{name}/reset", manage, r -> result(users.reset(actor(r), r.param("name"))));
        router.allowed("POST", "/api/users/{name}/role", manage, r -> result(users.setRole(actor(r), r.param("name"), r.required("role"))));
        router.allowed("DELETE", "/api/users/{name}", manage, r -> result(users.delete(actor(r), r.param("name"))));

        router.allowed("GET", "/api/roles", Set.of(Permission.ROLES_MANAGE, Permission.USERS_MANAGE), r -> rolesJson());
        router.allowed("POST", "/api/roles", Set.of(Permission.ROLES_MANAGE), this::saveRole);
        router.allowed("DELETE", "/api/roles/{name}", Set.of(Permission.ROLES_MANAGE), this::deleteRole);

        router.allowed("GET", "/api/audit", Set.of(Permission.AUDIT_VIEW), this::auditJson);
    }

    private static UserCommands.Actor actor(Request r) {
        return new UserCommands.Actor(r.user(), r.ip(), r.role());
    }

    private static Response result(UserCommands.Result result) {
        JsonObject o = new JsonObject();
        o.addProperty("code", result.code().name());
        o.addProperty("name", result.name());
        if (result.role() != null) o.addProperty("role", result.role());
        if (result.password() != null) o.addProperty("password", result.password());
        if (result.success()) {
            o.addProperty("ok", true);
            return Response.ok(o);
        }
        o.addProperty("error", result.code().name().toLowerCase());
        return Response.json(STATUS.getOrDefault(result.code(), 400), o);
    }

    private Response usersJson() {
        JsonArray array = new JsonArray();
        for (Account account : users.list()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", account.name());
            o.addProperty("role", account.role());
            o.addProperty("mustChange", account.mustChangePassword());
            o.addProperty("created", account.created());
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("users", array);
        return Response.ok(body);
    }

    private Response rolesJson() {
        Map<String, Integer> usage = new TreeMap<>();
        for (Account account : accounts.accounts()) usage.merge(account.role(), 1, Integer::sum);
        JsonArray array = new JsonArray();
        for (Role role : accounts.roles()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", role.name());
            o.addProperty("builtIn", role.builtIn());
            o.addProperty("users", usage.getOrDefault(role.name(), 0));
            JsonArray permissions = new JsonArray();
            Permission.ALL.stream().filter(role::has).forEach(permissions::add);
            o.add("permissions", permissions);
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("roles", array);
        return Response.ok(body);
    }

    private Response saveRole(Request r) throws IOException {
        String name = r.required("name");
        JsonElement list = r.json().get("permissions");
        if (list == null || !list.isJsonArray()) throw new HttpError(400, "missing_permissions");
        Set<String> permissions = new HashSet<>();
        for (JsonElement e : list.getAsJsonArray()) {
            if (!e.isJsonPrimitive()) throw new HttpError(400, "unknown_permission");
            permissions.add(e.getAsString());
        }
        Role role;
        try {
            role = Role.custom(name, permissions);
        } catch (IllegalArgumentException e) {
            throw new HttpError(400, e.getMessage());
        }
        UserCommands.Actor actor = actor(r);
        Role old = accounts.role(role.name());
        if (!actor.mayGrant(role) || (old != null && !actor.mayGrant(old))) {
            audit.record(r.user(), r.ip(), "role.save", role.name(), "forbidden");
            throw new HttpError(403, "forbidden");
        }
        accounts.putRole(role);
        audit.record(r.user(), r.ip(), "role.save", role.name() + " " + String.join(",", role.permissions()), "ok");
        return Response.ok();
    }

    private Response deleteRole(Request r) throws IOException {
        String name = r.param("name");
        Role old = accounts.role(name);
        if (old == null) throw new HttpError(404, "not_found");
        if (old.builtIn()) throw new HttpError(400, "built_in_role");
        if (!actor(r).mayGrant(old)) throw new HttpError(403, "forbidden");
        try {
            accounts.removeRole(old.name());
        } catch (IllegalStateException e) {
            throw new HttpError(409, "role_in_use");
        }
        audit.record(r.user(), r.ip(), "role.delete", old.name(), "ok");
        return Response.ok();
    }

    private Response auditJson(Request r) {
        int limit = 200;
        String wanted = r.query("limit");
        if (wanted != null) {
            try {
                limit = Math.max(1, Math.min(AuditLog.KEEP_IN_MEMORY, Integer.parseInt(wanted)));
            } catch (NumberFormatException ignored) {
            }
        }
        JsonArray array = new JsonArray();
        for (AuditLog.Entry entry : audit.recent(limit)) {
            JsonObject o = new JsonObject();
            o.addProperty("time", entry.time());
            o.addProperty("user", entry.user());
            o.addProperty("ip", entry.ip());
            o.addProperty("action", entry.action());
            o.addProperty("target", entry.target());
            o.addProperty("result", entry.result());
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.add("entries", array);
        return Response.ok(body);
    }
}
