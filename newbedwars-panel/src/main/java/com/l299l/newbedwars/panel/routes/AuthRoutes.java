package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.LoginLimiter;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.web.HttpError;
import com.l299l.newbedwars.panel.web.Request;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;
import com.l299l.newbedwars.panel.web.SessionCookie;

import java.io.IOException;
import java.util.Optional;

public final class AuthRoutes {
    public static final int MIN_PASSWORD = 10;
    public static final int MAX_PASSWORD = 128;
    private static final int MAX_NAME = 64;

    private final AccountStore accounts;
    private final SessionManager sessions;
    private final LoginLimiter limiter;
    private final PasswordHasher hasher;
    private final SessionCookie cookies;
    private final AuditLog audit;
    private final String defaultLanguage;
    private volatile String dummyHash;

    public AuthRoutes(AccountStore accounts, SessionManager sessions, LoginLimiter limiter, PasswordHasher hasher,
                      SessionCookie cookies, AuditLog audit, String defaultLanguage) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.limiter = limiter;
        this.hasher = hasher;
        this.cookies = cookies;
        this.audit = audit;
        this.defaultLanguage = defaultLanguage;
    }

    public void register(Router router) {
        router.open("GET", "/api/info", r -> {
            JsonObject o = new JsonObject();
            o.addProperty("language", defaultLanguage);
            return Response.ok(o);
        });
        router.open("POST", "/api/login", this::login);
        router.loggedIn("POST", "/api/logout", this::logout);
        router.loggedIn("GET", "/api/me", r -> Response.ok(me(r.account(), r.role(), r.session())));
        router.loggedIn("POST", "/api/password", this::password);
    }

    private Response login(Request request) throws IOException {
        String name = request.string("name");
        String password = request.string("password");
        if (name == null || password == null || name.length() > MAX_NAME || password.length() > MAX_PASSWORD) {
            throw new HttpError(400, "bad_login");
        }
        String userKey = "user:" + Account.key(name);
        String ipKey = "ip:" + request.ip();
        long locked = Math.max(limiter.lockedFor(userKey), limiter.lockedFor(ipKey));
        if (locked > 0) {
            audit.record(name, request.ip(), "login", null, "locked");
            JsonObject o = new JsonObject();
            o.addProperty("error", "locked");
            o.addProperty("retryAfter", (locked + 999) / 1000);
            return Response.json(429, o).header("Retry-After", String.valueOf((locked + 999) / 1000));
        }
        Optional<Account> found = accounts.find(name);
        char[] chars = password.toCharArray();
        boolean ok;
        try {
            ok = hasher.verify(chars, found.map(Account::passwordHash).orElseGet(this::dummyHash)) && found.isPresent();
            if (ok && hasher.needsRehash(found.get().passwordHash())) {
                Account account = found.get();
                accounts.put(account.withPassword(hasher.hash(chars), account.mustChangePassword()));
            }
        } finally {
            PasswordHasher.wipe(chars);
        }
        if (!ok) {
            limiter.failure(userKey);
            limiter.failure(ipKey);
            audit.record(name, request.ip(), "login", null, "failed");
            throw new HttpError(401, "bad_login");
        }
        limiter.success(userKey);
        Account account = found.get();
        if (request.session() != null) sessions.remove(request.session().id());
        SessionManager.Session session = sessions.create(account.name());
        audit.record(account.name(), request.ip(), "login", null, "ok");
        return cookies.attach(Response.ok(me(account, accounts.role(account.role()), session)), request, session.id());
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = hasher.hash(SessionManager.randomToken().toCharArray());
            dummyHash = hash;
        }
        return hash;
    }

    private Response logout(Request request) {
        sessions.remove(request.session().id());
        audit.record(request.user(), request.ip(), "logout", null, "ok");
        return cookies.clear(Response.ok(), request);
    }

    private Response password(Request request) throws IOException {
        String current = request.required("current");
        String next = request.required("password");
        Account account = request.account();
        if (next.length() < MIN_PASSWORD || next.length() > MAX_PASSWORD) throw new HttpError(400, "password_length");
        if (next.equals(current)) throw new HttpError(400, "password_same");
        if (next.equalsIgnoreCase(account.name())) throw new HttpError(400, "password_weak");
        String userKey = "user:" + account.key();
        if (limiter.lockedFor(userKey) > 0) throw new HttpError(429, "locked");
        char[] currentChars = current.toCharArray();
        char[] nextChars = next.toCharArray();
        try {
            if (!hasher.verify(currentChars, account.passwordHash())) {
                limiter.failure(userKey);
                audit.record(account.name(), request.ip(), "password.change", null, "wrong_password");
                throw new HttpError(403, "wrong_password");
            }
            accounts.put(account.withPassword(hasher.hash(nextChars), false));
        } finally {
            PasswordHasher.wipe(currentChars);
            PasswordHasher.wipe(nextChars);
        }
        sessions.removeUser(account.name(), request.session().id());
        audit.record(account.name(), request.ip(), "password.change", null, "ok");
        Account updated = accounts.find(account.name()).orElse(account);
        return Response.ok(me(updated, request.role(), request.session()));
    }

    static JsonObject me(Account account, Role role, SessionManager.Session session) {
        JsonObject o = new JsonObject();
        o.addProperty("name", account.name());
        o.addProperty("role", account.role());
        o.addProperty("mustChange", account.mustChangePassword());
        o.addProperty("csrf", session.csrf());
        JsonArray permissions = new JsonArray();
        if (role != null) Permission.ALL.stream().filter(role::has).forEach(permissions::add);
        o.add("permissions", permissions);
        JsonArray all = new JsonArray();
        Permission.ALL.forEach(all::add);
        o.add("allPermissions", all);
        return o;
    }
}
