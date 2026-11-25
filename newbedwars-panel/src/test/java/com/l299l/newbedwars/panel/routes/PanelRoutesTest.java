package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.api.maintenance.ArenaBackup;
import com.l299l.newbedwars.api.maintenance.ArenaMaintenance;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.api.NewBedwarsAPI;
import com.l299l.newbedwars.api.game.GameControl;
import com.l299l.newbedwars.api.view.LeaderboardEntry;
import com.l299l.newbedwars.api.view.PlayerStatsView;
import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.gamelog.GameLog;
import com.l299l.newbedwars.panel.gamelog.GameLogService;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.LoginLimiter;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.command.UserCommands;
import com.l299l.newbedwars.panel.web.PanelServer;
import com.l299l.newbedwars.panel.web.Router;
import com.l299l.newbedwars.panel.web.SessionCookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PanelRoutesTest {
    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private final PasswordHasher hasher = new PasswordHasher(1);
    private AccountStore accounts;
    private SessionManager sessions;
    private AuditLog audit;
    private ArenaMaintenance maintenance;
    private NewBedwarsAPI api;
    private GameControl control;
    private com.l299l.newbedwars.api.shop.ShopEditor shop;
    private com.l299l.newbedwars.api.mode.ModeService modeService;
    private GameLogService gameLog;
    private Router router;
    private PanelServer server;
    private String base;

    record Login(String cookie, String csrf) {}

    @BeforeEach
    void setUp() throws Exception {
        accounts = AccountStore.open(dir.resolve("accounts.dat"), new byte[32]);
        accounts.put(new Account("boss", hasher.hash("boss-password".toCharArray()), "admin", false, 0));
        accounts.put(new Account("helper", hasher.hash("helper-password".toCharArray()), "staff", false, 0));
        accounts.put(new Account("fresh", hasher.hash("one-time-pass".toCharArray()), "staff", true, 0));
        sessions = new SessionManager(60_000, 600_000, System::currentTimeMillis);
        audit = new AuditLog(dir.resolve("audit.log"), Logger.getAnonymousLogger(), System::currentTimeMillis);
        maintenance = mock(ArenaMaintenance.class);
        when(maintenance.getExportFolder()).thenReturn(dir.resolve("exports"));
        LoginLimiter limiter = new LoginLimiter(3, 60_000, System::currentTimeMillis);
        SessionCookie cookies = new SessionCookie(false, false);
        UserCommands users = new UserCommands(accounts, sessions, hasher, audit, System::currentTimeMillis);
        router = new Router();
        new AuthRoutes(accounts, sessions, limiter, hasher, cookies, audit, "pl").register(router);
        new ArenaRoutes(maintenance, audit, 1024).register(router);
        new AdminRoutes(accounts, users, audit).register(router);
        api = mock(NewBedwarsAPI.class);
        control = mock(GameControl.class);
        gameLog = new GameLogService(new GameLog(System::currentTimeMillis));
        new StatsRoutes(api).register(router);
        new GameRoutes(control, gameLog, audit).register(router);
        shop = mock(com.l299l.newbedwars.api.shop.ShopEditor.class);
        new ShopRoutes(shop, audit).register(router);
        modeService = mock(com.l299l.newbedwars.api.mode.ModeService.class);
        new ModeRoutes(modeService, audit).register(router);
        server = new PanelServer(router, sessions, accounts, cookies, false, Logger.getAnonymousLogger());
        server.start(new InetSocketAddress("127.0.0.1", 0), null);
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private HttpResponse<String> send(String method, String path, Login login, String body, boolean csrf) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (login != null) {
            b.header("Cookie", login.cookie());
            if (csrf) b.header("X-CSRF-Token", login.csrf());
        }
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, Login login, String body) throws Exception {
        return send(method, path, login, body, true);
    }

    private static JsonObject json(HttpResponse<String> r) {
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private HttpResponse<String> loginRaw(String name, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("password", password);
        return send("POST", "/api/login", null, body.toString());
    }

    private Login login(String name, String password) throws Exception {
        HttpResponse<String> r = loginRaw(name, password);
        assertEquals(200, r.statusCode(), r.body());
        String cookie = r.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"));
        return new Login(cookie.substring(0, cookie.indexOf(';')), json(r).get("csrf").getAsString());
    }

    @Test
    void loginMeAndLogout() throws Exception {
        assertEquals("pl", json(send("GET", "/api/info", null, null)).get("language").getAsString());
        assertEquals(401, loginRaw("boss", "wrong").statusCode());
        assertEquals(401, loginRaw("ghost", "whatever").statusCode());
        Login boss = login("BOSS", "boss-password");
        JsonObject me = json(send("GET", "/api/me", boss, null));
        assertEquals("boss", me.get("name").getAsString());
        assertEquals(Permission.ALL.size(), me.getAsJsonArray("permissions").size());
        assertEquals(boss.csrf(), me.get("csrf").getAsString());
        assertEquals(403, send("POST", "/api/logout", boss, "{}", false).statusCode());
        assertEquals(200, send("POST", "/api/logout", boss, "{}").statusCode());
        assertEquals(401, send("GET", "/api/me", boss, null).statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("login") && "failed".equals(e.result())));
    }

    @Test
    void wrongPasswordsLockTheAccount() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(401, loginRaw("helper", "nope").statusCode());
        HttpResponse<String> locked = loginRaw("helper", "helper-password");
        assertEquals(429, locked.statusCode());
        assertTrue(json(locked).get("retryAfter").getAsLong() > 0);
        assertTrue(locked.headers().firstValue("Retry-After").isPresent());
    }

    @Test
    void oneTimePasswordMustBeChangedFirst() throws Exception {
        Login fresh = login("fresh", "one-time-pass");
        assertEquals(403, send("GET", "/api/roles", fresh, null).statusCode());
        assertTrue(json(send("GET", "/api/me", fresh, null)).get("mustChange").getAsBoolean());
        assertEquals("password_length", json(send("POST", "/api/password", fresh, "{\"current\":\"one-time-pass\",\"password\":\"short\"}")).get("error").getAsString());
        assertEquals("wrong_password", json(send("POST", "/api/password", fresh, "{\"current\":\"bad\",\"password\":\"a-long-new-password\"}")).get("error").getAsString());
        assertEquals("password_same", json(send("POST", "/api/password", fresh, "{\"current\":\"one-time-pass\",\"password\":\"one-time-pass\"}")).get("error").getAsString());
        HttpResponse<String> changed = send("POST", "/api/password", fresh, "{\"current\":\"one-time-pass\",\"password\":\"a-long-new-password\"}");
        assertEquals(200, changed.statusCode());
        assertFalse(json(changed).get("mustChange").getAsBoolean());
        assertFalse(accounts.find("fresh").orElseThrow().mustChangePassword());
        assertEquals(401, loginRaw("fresh", "one-time-pass").statusCode());
        login("fresh", "a-long-new-password");
    }

    @Test
    void everyProtectedRouteChecksPermissionAndCsrf() throws Exception {
        Login helper = login("helper", "helper-password");
        Login boss = login("boss", "boss-password");
        for (Router.Route route : router.routes()) {
            if (route.access() != Router.Access.PERMISSION) continue;
            String path = route.pattern().replaceAll("\\{[^}]+}", "x.zip");
            boolean staffAllowed = route.permissions().stream().anyMatch(Role.STAFF_ROLE::has);
            if (!staffAllowed) {
                assertEquals(403, send(route.method(), path, helper, "{}").statusCode(), route.pattern());
                assertTrue(send(route.method(), path, helper, "{}").body().contains("forbidden"), route.pattern());
            }
            if (!route.method().equals("GET")) {
                HttpResponse<String> noCsrf = send(route.method(), path, boss, "{}", false);
                assertEquals(403, noCsrf.statusCode(), route.pattern());
                assertTrue(noCsrf.body().contains("csrf"), route.pattern());
            }
            assertEquals(401, send(route.method(), path, null, "{}").statusCode(), route.pattern());
        }
        verifyNoInteractions(maintenance);
    }

    @Test
    void arenaActionsGoThroughMaintenance() throws Exception {
        Login boss = login("boss", "boss-password");
        when(maintenance.backup("castle")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.OK, "castle", "Backup made", "20261111-120000")));
        when(maintenance.disable("castle")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.BUSY, "castle", "Players inside", null)));
        when(maintenance.restore("castle", "20261111-120000")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.OK, "castle", "Restored", null)));
        when(maintenance.export("castle")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.OK, "castle", "Exported", dir.resolve("exports/castle-1.zip").toString())));
        when(maintenance.listBackups("castle")).thenReturn(CompletableFuture.completedFuture(List.of(new ArenaBackup("20261111-120000", true, false))));

        JsonObject backup = json(send("POST", "/api/arenas/castle/backup", boss, "{}"));
        assertTrue(backup.get("ok").getAsBoolean());
        assertEquals("20261111-120000", backup.get("detail").getAsString());
        JsonObject disable = json(send("POST", "/api/arenas/castle/disable", boss, "{}"));
        assertFalse(disable.get("ok").getAsBoolean());
        assertEquals("BUSY", disable.get("code").getAsString());
        assertTrue(json(send("POST", "/api/arenas/castle/restore", boss, "{\"backup\":\"20261111-120000\"}")).get("ok").getAsBoolean());
        assertEquals("castle-1.zip", json(send("POST", "/api/arenas/castle/export", boss, "{}")).get("detail").getAsString());
        JsonObject backups = json(send("GET", "/api/arenas/castle/backups", boss, null));
        assertEquals("20261111-120000", backups.getAsJsonArray("backups").get(0).getAsJsonObject().get("id").getAsString());
        assertTrue(audit.recent(20).stream().anyMatch(e -> e.action().equals("arena.disable") && e.result().equals("BUSY")));
    }

    @Test
    void uploadsAndDownloadsStayInTheExportFolder() throws Exception {
        Login boss = login("boss", "boss-password");
        when(maintenance.importArena(anyString())).thenAnswer(i -> CompletableFuture.completedFuture(
                new MaintenanceResult(i.getArgument(0).equals("good.zip") ? MaintenanceResult.Code.OK : MaintenanceResult.Code.NAME_TAKEN, "castle", "msg", null)));
        assertEquals(200, send("POST", "/api/import?name=good.zip", boss, "PKzipdata").statusCode());
        assertEquals("PKzipdata", Files.readString(dir.resolve("exports/good.zip")));
        assertEquals(409, send("POST", "/api/import?name=good.zip", boss, "PKzipdata").statusCode());
        assertFalse(json(send("POST", "/api/import?name=taken.zip", boss, "PK")).get("ok").getAsBoolean());
        assertFalse(Files.exists(dir.resolve("exports/taken.zip")));
        assertEquals(413, send("POST", "/api/import?name=big.zip", boss, "x".repeat(2048)).statusCode());
        assertFalse(Files.exists(dir.resolve("exports/big.zip")));
        assertEquals(400, send("POST", "/api/import?name=..%2Fevil.zip", boss, "PK").statusCode());
        assertEquals(400, send("POST", "/api/import?name=evil.txt", boss, "PK").statusCode());

        HttpResponse<String> download = send("GET", "/api/exports/good.zip", boss, null);
        assertEquals(200, download.statusCode());
        assertEquals("PKzipdata", download.body());
        assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow().contains("good.zip"));
        assertEquals(400, send("GET", "/api/exports/..%2Faccounts.dat", boss, null).statusCode());
        assertEquals(404, send("GET", "/api/exports/missing.zip", boss, null).statusCode());
        JsonObject list = json(send("GET", "/api/exports", boss, null));
        assertEquals(1, list.getAsJsonArray("files").size());
        try (var files = Files.list(dir.resolve("exports"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void usersAndRolesCanNotGrantMoreThanTheActorHas() throws Exception {
        Login boss = login("boss", "boss-password");
        HttpResponse<String> created = send("POST", "/api/users", boss, "{\"name\":\"keeper\",\"role\":\"staff\"}");
        assertEquals(200, created.statusCode());
        String password = json(created).get("password").getAsString();
        assertEquals(UserCommands.PASSWORD_LENGTH, password.length());
        assertEquals(409, send("POST", "/api/users", boss, "{\"name\":\"keeper\",\"role\":\"staff\"}").statusCode());

        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"managers\",\"permissions\":[\"dashboard.view\",\"stats.view\",\"network.servers\",\"users.manage\"]}").statusCode());
        assertEquals(400, send("POST", "/api/roles", boss, "{\"name\":\"admin\",\"permissions\":[]}").statusCode());
        assertEquals(400, send("POST", "/api/roles", boss, "{\"name\":\"x\",\"permissions\":[\"nope\"]}").statusCode());
        assertEquals(200, send("POST", "/api/users/keeper/role", boss, "{\"role\":\"managers\"}").statusCode());
        assertEquals("managers", accounts.find("keeper").orElseThrow().role());

        accounts.put(accounts.find("keeper").orElseThrow().withPassword(hasher.hash("keeper-password".toCharArray()), false));
        Login keeper = login("keeper", "keeper-password");
        assertEquals(403, send("POST", "/api/users", keeper, "{\"name\":\"sneaky\",\"role\":\"admin\"}").statusCode());
        assertEquals(403, send("POST", "/api/users/boss/reset", keeper, "{}").statusCode());
        assertEquals(403, send("DELETE", "/api/users/boss", keeper, null).statusCode());
        assertEquals(403, send("POST", "/api/users/helper/role", keeper, "{\"role\":\"admin\"}").statusCode());
        assertEquals(403, send("POST", "/api/roles", keeper, "{\"name\":\"managers\",\"permissions\":[]}").statusCode());
        assertEquals(200, send("POST", "/api/users", keeper, "{\"name\":\"viewer\",\"role\":\"staff\"}").statusCode());
        assertEquals(200, send("POST", "/api/users/viewer/reset", keeper, "{}").statusCode());

        assertEquals(409, send("DELETE", "/api/roles/managers", boss, null).statusCode());
        assertEquals(409, send("POST", "/api/users/boss/role", boss, "{\"role\":\"staff\"}").statusCode());
        assertEquals(409, send("DELETE", "/api/users/boss", boss, null).statusCode());
        assertEquals(200, send("DELETE", "/api/users/keeper", boss, null).statusCode());
        assertEquals(401, send("GET", "/api/me", keeper, null).statusCode());
        assertEquals(200, send("DELETE", "/api/roles/managers", boss, null).statusCode());
        assertNull(accounts.role("managers"));
        assertEquals(Set.of("admin", "staff"), Set.copyOf(accounts.roles().stream().map(Role::name).toList()));

        JsonObject log = json(send("GET", "/api/audit?limit=5", boss, null));
        assertEquals(5, log.getAsJsonArray("entries").size());
        assertFalse(log.toString().contains(password));
    }

    private static PlayerStatsView stats(int wins, int kills) {
        PlayerStatsView v = mock(PlayerStatsView.class);
        when(v.getWins()).thenReturn(wins);
        when(v.getKills()).thenReturn(kills);
        return v;
    }

    @Test
    void statsRoutesForStaff() throws Exception {
        Login helper = login("helper", "helper-password");
        List<LeaderboardEntry> entries = List.of(new LeaderboardEntry("Alice", stats(3, 40)), new LeaderboardEntry("Bob", stats(5, 10)));
        PlayerStatsView alice = stats(3, 40);
        when(api.getLeaderboard(StatType.KILLS, 100)).thenReturn(CompletableFuture.completedFuture(entries));
        JsonObject top = json(send("GET", "/api/leaderboard?stat=kills&limit=500", helper, null));
        assertEquals("KILLS", top.get("stat").getAsString());
        JsonObject first = top.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(1, first.get("rank").getAsInt());
        assertEquals("Alice", first.get("name").getAsString());
        assertEquals(40, first.get("kills").getAsInt());
        assertEquals(400, send("GET", "/api/leaderboard?stat=nope", helper, null).statusCode());
        assertEquals(400, send("GET", "/api/leaderboard?limit=x", helper, null).statusCode());

        when(api.findPlayers("al", 20)).thenReturn(CompletableFuture.completedFuture(List.of("Alice", "Alex")));
        assertEquals(2, json(send("GET", "/api/players?q=al", helper, null)).getAsJsonArray("players").size());
        assertEquals(0, json(send("GET", "/api/players?q=", helper, null)).getAsJsonArray("players").size());

        when(api.loadStats("Alice")).thenReturn(CompletableFuture.completedFuture(Optional.of(alice)));
        when(api.loadStats("ghost")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(api.loadStats("broken")).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("db")));
        assertEquals(3, json(send("GET", "/api/players/Alice", helper, null)).get("wins").getAsInt());
        assertEquals(404, send("GET", "/api/players/ghost", helper, null).statusCode());
        assertEquals(503, send("GET", "/api/players/broken", helper, null).statusCode());
        assertEquals(403, send("POST", "/api/arenas/castle/game/start", helper, "{}").statusCode());
        assertEquals(403, send("GET", "/api/arenas/castle/log", helper, null).statusCode());
    }

    @Test
    void modesAreListedWithPhaseStarts() throws Exception {
        var squads = new com.l299l.newbedwars.api.mode.ModeInfo("Squads", "fast", "quick", null, "ups", 4, 2, true);
        var plain = new com.l299l.newbedwars.api.mode.ModeInfo("Plain", null, null, null, null, 0, 0, false);
        var phases = new com.l299l.newbedwars.api.mode.PhaseListInfo("quick", List.of(
                new com.l299l.newbedwars.api.mode.PhaseInfo("One", 60, List.of("A:1")),
                new com.l299l.newbedwars.api.mode.PhaseInfo("Two", 120, List.of()),
                new com.l299l.newbedwars.api.mode.PhaseInfo("Three", 0, List.of("B", "C"))));
        when(modeService.getModes()).thenReturn(CompletableFuture.completedFuture(List.of(squads, plain)));
        when(modeService.getPhaseLists()).thenReturn(CompletableFuture.completedFuture(List.of(phases)));
        when(modeService.getGeneratorConfigs()).thenReturn(CompletableFuture.completedFuture(List.of("fast", "slow")));
        Login helper = login("helper", "helper-password");
        Login boss = login("boss", "boss-password");
        assertEquals(403, send("GET", "/api/modes", helper, null).statusCode());
        JsonObject body = json(send("GET", "/api/modes", boss, null));
        JsonObject first = body.getAsJsonArray("modes").get(0).getAsJsonObject();
        assertEquals("Squads", first.get("id").getAsString());
        assertEquals("fast", first.get("generators").getAsString());
        assertTrue(first.get("builtIn").getAsBoolean());
        assertEquals(4, first.get("maxInTeam").getAsInt());
        var timeline = first.getAsJsonArray("phases");
        assertEquals(0, timeline.get(0).getAsJsonObject().get("start").getAsInt());
        assertEquals(60, timeline.get(1).getAsJsonObject().get("start").getAsInt());
        assertEquals(180, timeline.get(2).getAsJsonObject().get("start").getAsInt());
        assertEquals(2, timeline.get(2).getAsJsonObject().getAsJsonArray("actions").size());
        JsonObject second = body.getAsJsonArray("modes").get(1).getAsJsonObject();
        assertFalse(second.has("phases"));
        assertTrue(second.get("generators").isJsonNull() || !second.has("generators"));
        assertEquals(1, body.getAsJsonArray("phaseLists").size());
        assertEquals(2, body.getAsJsonArray("generatorConfigs").size());
    }

    @Test
    void arenaModesAreDerivedPerArena() throws Exception {
        var squads = new com.l299l.newbedwars.api.mode.ModeInfo("Squads", "fast", "quick", null, "ups", 4, 2, true);
        when(modeService.getModes()).thenReturn(CompletableFuture.completedFuture(List.of(squads)));
        when(modeService.getArenaModes()).thenReturn(CompletableFuture.completedFuture(List.of(
                new com.l299l.newbedwars.api.mode.ArenaModeInfo("castle", true, "fast", "quick", "shop", "ups", 4, 2),
                new com.l299l.newbedwars.api.mode.ArenaModeInfo("ruins", false, "slow", null, null, null, 2, 8))));
        assertEquals(403, send("GET", "/api/modes/arenas", login("helper", "helper-password"), null).statusCode());
        JsonObject body = json(send("GET", "/api/modes/arenas", login("boss", "boss-password"), null));
        var arenas = body.getAsJsonArray("arenas");
        assertEquals(2, arenas.size());
        JsonObject castle = arenas.get(0).getAsJsonObject();
        assertEquals("castle", castle.get("arena").getAsString());
        assertTrue(castle.get("waiting").getAsBoolean());
        assertEquals("Squads", castle.get("mode").getAsString());
        assertEquals("fast", castle.get("generators").getAsString());
        assertEquals(4, castle.get("maxInTeam").getAsInt());
        assertEquals(2, castle.get("teams").getAsInt());
        JsonObject ruins = arenas.get(1).getAsJsonObject();
        assertFalse(ruins.get("waiting").getAsBoolean());
        assertTrue(ruins.get("mode").isJsonNull());
        assertEquals(8, ruins.get("teams").getAsInt());
    }

    @Test
    void applyingAModeMapsResultsAndIsAudited() throws Exception {
        Login boss = login("boss", "boss-password");
        Login helper = login("helper", "helper-password");
        
        when(modeService.applyMode("castle", "Squads")).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.mode.ModeResult(com.l299l.newbedwars.api.mode.ModeResult.Code.OK, List.of(), true)));
        when(modeService.applyMode("castle", "Ghost")).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.mode.ModeResult(com.l299l.newbedwars.api.mode.ModeResult.Code.NOT_FOUND, List.of(), false)));
        when(modeService.applyMode("castle", "Broken")).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.mode.ModeResult(com.l299l.newbedwars.api.mode.ModeResult.Code.MISSING_PARTS, List.of("fast", "gui:x"), false)));
        when(modeService.applyMode("castle", "Busy")).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.mode.ModeResult(com.l299l.newbedwars.api.mode.ModeResult.Code.BUSY, List.of(), false)));

        assertEquals(403, send("POST", "/api/arenas/castle/mode", helper, "{\"mode\":\"Squads\"}").statusCode());
        verifyNoInteractions(modeService);
        assertEquals(400, send("POST", "/api/arenas/castle/mode", boss, "{}").statusCode());

        JsonObject ok = json(send("POST", "/api/arenas/castle/mode", boss, "{\"mode\":\"Squads\"}"));
        assertTrue(ok.get("ok").getAsBoolean());
        assertTrue(ok.get("teamsDiffer").getAsBoolean());
        HttpResponse<String> notFound = send("POST", "/api/arenas/castle/mode", boss, "{\"mode\":\"Ghost\"}");
        assertEquals(404, notFound.statusCode());
        assertEquals("mode_not_found", json(notFound).get("error").getAsString());
        HttpResponse<String> missing = send("POST", "/api/arenas/castle/mode", boss, "{\"mode\":\"Broken\"}");
        assertEquals(400, missing.statusCode());
        assertEquals("mode_missing_parts", json(missing).get("error").getAsString());
        assertEquals("gui:x", json(missing).getAsJsonArray("missing").get(1).getAsString());
        HttpResponse<String> busy = send("POST", "/api/arenas/castle/mode", boss, "{\"mode\":\"Busy\"}");
        assertEquals(409, busy.statusCode());
        assertEquals("arena_busy", json(busy).get("error").getAsString());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("arena.mode") && e.target().equals("castle -> Squads") && e.result().equals("OK")));
    }

    private static CompletableFuture<com.l299l.newbedwars.api.mode.EditResult> edit(com.l299l.newbedwars.api.mode.EditResult.Code code, List<String> problems, List<String> usedBy) {
        return CompletableFuture.completedFuture(new com.l299l.newbedwars.api.mode.EditResult(code, problems, usedBy));
    }

    @Test
    void modeEditingNeedsItsOwnPermission() throws Exception {
        Login helper = login("helper", "helper-password");
        for (String[] call : new String[][]{{"PUT", "/api/modes/Squads"}, {"DELETE", "/api/modes/Squads"}, {"PUT", "/api/phases/quick"},
                {"DELETE", "/api/phases/quick"}, {"PUT", "/api/generators/fast"}, {"DELETE", "/api/generators/fast"}}) {
            assertEquals(403, send(call[0], call[1], helper, "{}").statusCode(), call[1]);
        }
        assertEquals(403, send("GET", "/api/generators", helper, null).statusCode());
        assertEquals(403, send("GET", "/api/phases", helper, null).statusCode());
        verifyNoInteractions(modeService);
        assertTrue(Permission.isKnown("modes.edit"));
        assertFalse(Permission.LOCAL_ONLY.contains("modes.edit"));
        assertTrue(Role.ADMIN_ROLE.has("modes.edit"));
    }

    @Test
    void savingAModeParsesTheBodyMapsCodesAndAudits() throws Exception {
        Login boss = login("boss", "boss-password");
        when(modeService.getModes()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(List.of(
                new com.l299l.newbedwars.api.mode.ModeInfo("squads", null, null, null, null, 4, 4, true, "Compact"))));
        var ok = new com.l299l.newbedwars.api.mode.ModeInfo("Squads", "fast", null, "shop", null, 4, 2, false, "Compact");
        when(modeService.saveMode(ok)).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.OK, List.of(), List.of()));
        when(modeService.saveMode(new com.l299l.newbedwars.api.mode.ModeInfo("Bad", null, null, null, null, 0, 0, false)))
                .thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.INVALID, List.of("no such phase list"), List.of()));

        HttpResponse<String> saved = send("PUT", "/api/modes/Squads", boss,
                "{\"generators\":\"fast\",\"gamePhases\":\"\",\"shopGui\":\"shop\",\"upgradeGui\":null,\"maxInTeam\":4,\"teams\":2}");
        assertEquals(200, saved.statusCode(), saved.body());
        assertEquals("OK", json(saved).get("code").getAsString());
        HttpResponse<String> invalid = send("PUT", "/api/modes/Bad", boss, "{}");
        assertEquals(400, invalid.statusCode());
        assertEquals("INVALID", json(invalid).get("code").getAsString());
        assertEquals("no such phase list", json(invalid).getAsJsonArray("problems").get(0).getAsString());
        assertEquals(400, send("PUT", "/api/modes/Bad", boss, "{\"teams\":-1}").statusCode());
        assertEquals(400, send("PUT", "/api/modes/Bad", boss, "{\"teams\":\"x\"}").statusCode());
        assertEquals(400, send("PUT", "/api/modes/bad%20id", boss, "{}").statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("modes.save") && e.target().equals("Squads") && e.result().equals("OK")));
    }

    @Test
    void deletesMapEveryCodeToAStatus() throws Exception {
        Login boss = login("boss", "boss-password");
        var code = com.l299l.newbedwars.api.mode.EditResult.Code.class;
        when(modeService.deleteMode("a")).thenReturn(edit(code.getEnumConstants()[0], List.of(), List.of()));
        when(modeService.deleteMode("b")).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.NOT_FOUND, List.of(), List.of()));
        when(modeService.deleteMode("c")).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.BUILT_IN, List.of(), List.of()));
        when(modeService.deletePhaseList("d")).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.IN_USE, List.of(), List.of("mode Squads", "arena castle")));
        when(modeService.deleteGeneratorConfig("e")).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.FAILED, List.of("disk"), List.of()));

        assertEquals(200, send("DELETE", "/api/modes/a", boss, null).statusCode());
        assertEquals(404, send("DELETE", "/api/modes/b", boss, null).statusCode());
        assertEquals(409, send("DELETE", "/api/modes/c", boss, null).statusCode());
        HttpResponse<String> inUse = send("DELETE", "/api/phases/d", boss, null);
        assertEquals(409, inUse.statusCode());
        assertEquals("arena castle", json(inUse).getAsJsonArray("usedBy").get(1).getAsString());
        assertEquals(500, send("DELETE", "/api/generators/e", boss, null).statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("phases.delete") && e.result().equals("IN_USE")));
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("generators.delete") && e.result().equals("FAILED")));
        assertEquals(403, send("DELETE", "/api/modes/a", boss, null, false).statusCode());
    }

    @Test
    void phaseListsAreParsedStrictly() throws Exception {
        Login boss = login("boss", "boss-password");
        var list = new com.l299l.newbedwars.api.mode.PhaseListInfo("quick", List.of(
                new com.l299l.newbedwars.api.mode.PhaseInfo("One", 360, List.of("DIAMOND_GENERATOR:2", "BED_DESTRUCTION")),
                new com.l299l.newbedwars.api.mode.PhaseInfo("Two", 0, List.of())));
        when(modeService.savePhaseList(list)).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.OK, List.of(), List.of()));
        when(modeService.getPhaseLists()).thenReturn(CompletableFuture.completedFuture(List.of(list)));

        assertEquals(200, send("PUT", "/api/phases/quick", boss,
                "{\"phases\":[{\"name\":\"One\",\"duration\":360,\"actions\":[\"DIAMOND_GENERATOR:2\",\"BED_DESTRUCTION\"]},{\"name\":\"Two\",\"duration\":0}]}").statusCode());
        assertEquals(400, send("PUT", "/api/phases/quick", boss, "{}").statusCode());
        assertEquals(400, send("PUT", "/api/phases/quick", boss, "{\"phases\":[{\"duration\":5}]}").statusCode());
        assertEquals(400, send("PUT", "/api/phases/quick", boss, "{\"phases\":[{\"name\":\"x\",\"duration\":-5}]}").statusCode());
        assertEquals(400, send("PUT", "/api/phases/quick", boss, "{\"phases\":[{\"name\":\"x\",\"duration\":5,\"actions\":[1]}]}").statusCode());
        JsonObject lists = json(send("GET", "/api/phases", boss, null));
        assertEquals(360, lists.getAsJsonArray("phaseLists").get(0).getAsJsonObject().getAsJsonArray("phases").get(0).getAsJsonObject().get("duration").getAsInt());
    }

    @Test
    void generatorConfigsAreListedReadAndSaved() throws Exception {
        Login boss = login("boss", "boss-password");
        when(modeService.getGeneratorConfigFiles()).thenReturn(CompletableFuture.completedFuture(List.of(
                new com.l299l.newbedwars.api.mode.GeneratorConfigInfo("fast", "fast.yml", true))));
        when(modeService.getGeneratorConfigText("fast")).thenReturn(CompletableFuture.completedFuture("ConfigurationId: fast\n"));
        when(modeService.getGeneratorConfigText("ghost")).thenReturn(CompletableFuture.completedFuture(null));
        when(modeService.saveGeneratorConfig("fast", "ConfigurationId: fast\n")).thenReturn(edit(com.l299l.newbedwars.api.mode.EditResult.Code.OK, List.of(), List.of()));

        JsonObject files = json(send("GET", "/api/generators", boss, null)).getAsJsonArray("generators").get(0).getAsJsonObject();
        assertEquals("fast.yml", files.get("fileName").getAsString());
        assertTrue(files.get("builtIn").getAsBoolean());
        assertEquals("ConfigurationId: fast\n", json(send("GET", "/api/generators/fast", boss, null)).get("yaml").getAsString());
        assertEquals(404, send("GET", "/api/generators/ghost", boss, null).statusCode());
        assertEquals(200, send("PUT", "/api/generators/fast", boss, "{\"yaml\":\"ConfigurationId: fast\\n\"}").statusCode());
        assertEquals(400, send("PUT", "/api/generators/fast", boss, "{}").statusCode());
        String big = "{\"yaml\":\"" + "a".repeat(49 * 1024) + "\"}";
        assertEquals(413, send("PUT", "/api/generators/fast", boss, big).statusCode());
        verify(modeService, never()).saveGeneratorConfig(eq("fast"), org.mockito.ArgumentMatchers.argThat(y -> y.length() > 40_000));
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("generators.save") && e.result().equals("OK")));
    }

    @Test
    void gameActionsAreAuditedAndLogged() throws Exception {
        Login boss = login("boss", "boss-password");
        when(control.forceStart("castle", "Panel: boss")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.OK, "Castle", "boss forced the start", null)));
        when(control.stop("castle", "Panel: boss")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.NOT_READY, "Castle", "not running", null)));
        when(control.broadcast("castle", "Panel: boss", "&eHi")).thenReturn(CompletableFuture.completedFuture(
                new MaintenanceResult(MaintenanceResult.Code.OK, "Castle", "Hi", null)));

        assertTrue(json(send("POST", "/api/arenas/castle/game/forcestart", boss, "{}")).get("ok").getAsBoolean());
        JsonObject refused = json(send("POST", "/api/arenas/castle/game/stop", boss, "{}"));
        assertFalse(refused.get("ok").getAsBoolean());
        assertEquals("NOT_READY", refused.get("code").getAsString());
        assertEquals(404, send("POST", "/api/arenas/castle/game/explode", boss, "{}").statusCode());
        assertTrue(json(send("POST", "/api/arenas/castle/broadcast", boss, "{\"message\":\"&eHi\"}")).get("ok").getAsBoolean());
        assertEquals(400, send("POST", "/api/arenas/castle/broadcast", boss, "{}").statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("game.stop") && e.result().equals("NOT_READY")));

        JsonObject log = json(send("GET", "/api/arenas/CASTLE/log", boss, null));
        assertEquals(2, log.getAsJsonArray("entries").size());
        JsonObject admin = log.getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("ADMIN", admin.get("type").getAsString());
        assertEquals("forcestart", admin.get("target").getAsString());
        long seq = log.get("seq").getAsLong();
        assertEquals(0, json(send("GET", "/api/arenas/castle/log?after=" + seq, boss, null)).getAsJsonArray("entries").size());
        assertEquals(400, send("GET", "/api/arenas/castle/log?after=x", boss, null).statusCode());
    }

    @Test
    void shopItemsAreReadAndSaved() throws Exception {
        Login boss = login("boss", "boss-password");
        Login helper = login("helper", "helper-password");
        var wool = new com.l299l.newbedwars.api.shop.ShopItem("Wool", "WHITE_WOOL", 16, 4, "IRON", "GIVE_ITEM", "NONE", false, "",
                java.util.Map.of("sharpness", 2), List.of("HIDE_ENCHANTS"), List.of("gui:shop"));
        when(shop.getItems()).thenReturn(CompletableFuture.completedFuture(List.of(wool)));
        when(shop.getItem("Wool")).thenReturn(CompletableFuture.completedFuture(Optional.of(wool)));
        when(shop.getItem("Nope")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(shop.getItemTexts("Wool")).thenReturn(CompletableFuture.completedFuture(Optional.of(
                java.util.Map.of("en", new com.l299l.newbedwars.api.shop.ItemTexts("&fWool", "a;b", "")))));
        when(shop.saveItem(any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.INVALID, "material: unknown item X")));
        when(shop.saveItemTexts(eq("Wool"), any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null)));

        assertEquals(403, send("GET", "/api/shop/items", helper, null).statusCode());
        assertEquals("Wool", json(send("GET", "/api/shop/items", boss, null)).getAsJsonArray("items").get(0).getAsJsonObject().get("name").getAsString());
        JsonObject one = json(send("GET", "/api/shop/items/Wool", boss, null));
        assertEquals(2, one.getAsJsonObject("item").getAsJsonObject("enchantments").get("sharpness").getAsInt());
        assertEquals("a;b", one.getAsJsonObject("texts").getAsJsonObject("en").get("iconDescription").getAsString());
        assertEquals(404, send("GET", "/api/shop/items/Nope", boss, null).statusCode());

        JsonObject refused = json(send("POST", "/api/shop/items/Wool", boss,
                "{\"material\":\"X\",\"amount\":1,\"price\":2,\"priceType\":\"IRON\",\"onBuy\":\"GIVE_ITEM\",\"logic\":\"NONE\",\"permanent\":true,\"permission\":\"\",\"enchantments\":{\"sharpness\":3},\"flags\":[\"HIDE_ENCHANTS\"]}"));
        assertEquals("INVALID", refused.get("code").getAsString());
        assertEquals("material: unknown item X", refused.get("detail").getAsString());
        var captor = org.mockito.ArgumentCaptor.forClass(com.l299l.newbedwars.api.shop.ShopItem.class);
        verify(shop).saveItem(captor.capture());
        assertEquals("Wool", captor.getValue().name());
        assertTrue(captor.getValue().permanent());
        assertEquals(3, captor.getValue().enchantments().get("sharpness"));
        assertEquals(400, send("POST", "/api/shop/items/Wool", boss, "{\"amount\":\"many\"}").statusCode());
        assertEquals(400, send("POST", "/api/shop/items/Wool", boss, "{\"amount\":1.5}").statusCode());
        assertEquals(400, send("POST", "/api/shop/items/Wool", boss, "{\"flags\":\"HIDE\"}").statusCode());

        assertTrue(json(send("POST", "/api/shop/items/Wool/texts", boss, "{\"texts\":{\"pl\":{\"name\":\"&fWelna\"}}}")).get("ok").getAsBoolean());
        assertEquals(400, send("POST", "/api/shop/items/Wool/texts", boss, "{}").statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("shop.item.save") && e.result().equals("INVALID")));
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("shop.item.texts") && e.result().equals("OK")));
    }

    @Test
    void shopCommandsNeedTheirOwnPermission() throws Exception {
        Login boss = login("boss", "boss-password");
        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"shopkeepers\",\"permissions\":[\"shop.edit\"]}").statusCode());
        accounts.put(new Account("keeper", hasher.hash("keeper-password".toCharArray()), "shopkeepers", false, 0));
        Login keeper = login("keeper", "keeper-password");
        var reward = new com.l299l.newbedwars.api.shop.ShopItem("Reward", "NETHER_STAR", 1, 2, "IRON", "COMMANDS", "NONE", false, "",
                java.util.Map.of(), List.of(), List.of(), List.of("say hi"), "", "", "");
        when(shop.getItem("Reward")).thenReturn(CompletableFuture.completedFuture(Optional.of(reward)));
        when(shop.getItem("Fresh")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(shop.getItemTexts("Reward")).thenReturn(CompletableFuture.completedFuture(Optional.of(java.util.Map.of())));
        when(shop.saveItem(any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null)));
        String base = "\"material\":\"NETHER_STAR\",\"amount\":1,\"price\":2,\"priceType\":\"IRON\",\"onBuy\":\"COMMANDS\",\"logic\":\"NONE\"";

        HttpResponse<String> refused = send("POST", "/api/shop/items/Reward", keeper, "{" + base + ",\"commands\":[\"op keeper\"]}");
        assertEquals(403, refused.statusCode());
        assertTrue(refused.body().contains("commands_forbidden"));
        assertEquals(403, send("POST", "/api/shop/items/Fresh", keeper, "{" + base + ",\"commands\":[\"op keeper\"]}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Reward", keeper, "{" + base.replace("\"COMMANDS\"", "\"GIVE_PERM\"") + ",\"commands\":[\"say hi\"],\"givePermission\":\"minecraft.command.op\"}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Reward", keeper, "{" + base + ",\"commands\":[\"say hi\"],\"permission\":\"\",\"price\":0}").statusCode());
        verify(shop, times(0)).saveItem(any());

        assertEquals(200, send("POST", "/api/shop/items/Reward", boss, "{" + base + ",\"commands\":[\"say bye\"],\"priceMaterial\":\"\"}").statusCode());
        var captor = org.mockito.ArgumentCaptor.forClass(com.l299l.newbedwars.api.shop.ShopItem.class);
        verify(shop, times(1)).saveItem(captor.capture());
        assertEquals(List.of("say bye"), captor.getValue().commands());
        JsonObject one = json(send("GET", "/api/shop/items/Reward", boss, null));
        assertEquals("say hi", one.getAsJsonObject("item").getAsJsonArray("commands").get(0).getAsString());
    }

    @Test
    void powerItemsAreLockedForShopEditWithoutCommands() throws Exception {
        Login boss = login("boss", "boss-password");
        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"shopkeepers\",\"permissions\":[\"shop.edit\"]}").statusCode());
        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"shopmasters\",\"permissions\":[\"shop.edit\",\"shop.commands\"]}").statusCode());
        accounts.put(new Account("keeper", hasher.hash("keeper-password".toCharArray()), "shopkeepers", false, 0));
        accounts.put(new Account("master", hasher.hash("master-password".toCharArray()), "shopmasters", false, 0));
        Login keeper = login("keeper", "keeper-password");
        Login master = login("master", "master-password");
        var reward = new com.l299l.newbedwars.api.shop.ShopItem("Reward", "NETHER_STAR", 1, 2, "IRON", "COMMANDS", "NONE", false, "vip",
                java.util.Map.of(), List.of(), List.of(), List.of("say hi"), "", "", "");
        var perm = new com.l299l.newbedwars.api.shop.ShopItem("Perm", "NETHER_STAR", 1, 2, "IRON", "GIVE_PERM", "NONE", false, "",
                java.util.Map.of(), List.of(), List.of(), List.of(), "", "some.node", "");
        when(shop.getItems()).thenReturn(CompletableFuture.completedFuture(List.of(reward, perm)));
        when(shop.getItem("Reward")).thenReturn(CompletableFuture.completedFuture(Optional.of(reward)));
        when(shop.getItemTexts("Reward")).thenReturn(CompletableFuture.completedFuture(Optional.of(java.util.Map.of())));
        var ok = new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null);
        when(shop.saveItem(any())).thenReturn(CompletableFuture.completedFuture(ok));
        when(shop.deleteItem(any())).thenReturn(CompletableFuture.completedFuture(ok));
        String body = "{\"material\":\"NETHER_STAR\",\"amount\":1,\"price\":0,\"priceType\":\"IRON\",\"onBuy\":\"COMMANDS\",\"logic\":\"NONE\",\"permission\":\"\",\"commands\":[\"say hi\"]}";

        HttpResponse<String> refused = send("POST", "/api/shop/items/Reward", keeper, body);
        assertEquals(403, refused.statusCode());
        assertTrue(refused.body().contains("commands_forbidden"));
        assertEquals(403, send("POST", "/api/shop/items/Reward/delete", keeper, "{}").statusCode());
        verify(shop, times(0)).saveItem(any());
        verify(shop, times(0)).deleteItem(any());
        assertEquals(200, send("POST", "/api/shop/items/Reward", master, body).statusCode());
        verify(shop, times(1)).saveItem(any());

        JsonObject hidden = json(send("GET", "/api/shop/items", keeper, null));
        for (var el : hidden.getAsJsonArray("items")) {
            JsonObject o = el.getAsJsonObject();
            assertEquals(0, o.getAsJsonArray("commands").size());
            assertEquals("", o.get("givePermission").getAsString());
        }
        JsonObject one = json(send("GET", "/api/shop/items/Reward", keeper, null));
        assertEquals(0, one.getAsJsonObject("item").getAsJsonArray("commands").size());
        JsonObject full = json(send("GET", "/api/shop/items", master, null));
        assertEquals("say hi", full.getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonArray("commands").get(0).getAsString());
        assertEquals("some.node", full.getAsJsonArray("items").get(1).getAsJsonObject().get("givePermission").getAsString());

        when(shop.getGui("shop")).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(shop.saveGui(any())).thenReturn(CompletableFuture.completedFuture(ok));
        String gui = "{\"title\":\"t\",\"size\":54,\"categories\":[{\"id\":\"c\",\"name\":\"C\",\"icon\":\"STONE\",\"description\":\"\",\"items\":[\"Reward\"]}]}";
        assertEquals(403, send("POST", "/api/shop/guis/shop", keeper, gui).statusCode());
        assertEquals(200, send("POST", "/api/shop/guis/shop", master, gui).statusCode());

        var existing = new com.l299l.newbedwars.api.shop.ShopGui("menu", "t", 54, 0, 0, false, null, null, List.of(),
                List.of(new com.l299l.newbedwars.api.shop.ShopCategory("c", "C", "STONE", "", List.of("Reward"))),
                List.of(), List.of(), null,
                java.util.Map.of("Reward", new com.l299l.newbedwars.api.shop.ItemPrice(5, "IRON", 1)));
        when(shop.getGui("menu")).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        String cat = "\"categories\":[{\"id\":\"c\",\"name\":\"C\",\"icon\":\"STONE\",\"description\":\"\",\"items\":[\"Reward\"]}]";
        String freePrice = "{\"title\":\"t\",\"size\":54," + cat + ",\"itemPrices\":{\"Reward\":{\"price\":0,\"currency\":\"IRON\",\"amount\":1}}}";
        String samePrice = "{\"title\":\"renamed\",\"size\":54," + cat + ",\"itemPrices\":{\"Reward\":{\"price\":5,\"currency\":\"IRON\",\"amount\":1}}}";
        String droppedPrice = "{\"title\":\"t\",\"size\":54," + cat + ",\"itemPrices\":{}}";
        assertEquals(403, send("POST", "/api/shop/guis/menu", keeper, freePrice).statusCode());
        assertEquals(403, send("POST", "/api/shop/guis/menu", keeper, droppedPrice).statusCode());
        assertEquals(200, send("POST", "/api/shop/guis/menu", keeper, samePrice).statusCode());
        assertEquals(200, send("POST", "/api/shop/guis/menu", master, freePrice).statusCode());
    }

    @Test
    void whitespaceAroundTheActionDoesNotBypassTheCommandsPermission() throws Exception {
        Login boss = login("boss", "boss-password");
        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"shopkeepers\",\"permissions\":[\"shop.edit\"]}").statusCode());
        accounts.put(new Account("keeper", hasher.hash("keeper-password".toCharArray()), "shopkeepers", false, 0));
        Login keeper = login("keeper", "keeper-password");
        var plain = new com.l299l.newbedwars.api.shop.ShopItem("Plain", "NETHER_STAR", 1, 2, "IRON", "GIVE_ITEM", "NONE", false, "",
                java.util.Map.of(), List.of(), List.of(), List.of(), "", "", "");
        when(shop.getItem("Plain")).thenReturn(CompletableFuture.completedFuture(Optional.of(plain)));
        when(shop.saveItem(any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null)));
        String base = "\"material\":\"NETHER_STAR\",\"amount\":1,\"price\":2,\"priceType\":\"IRON\",\"logic\":\"NONE\",\"commands\":[\"op keeper\"]";

        assertEquals(403, send("POST", "/api/shop/items/Plain", keeper, "{" + base + ",\"onBuy\":\"COMMANDS \"}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Plain", keeper, "{" + base + ",\"onBuy\":\" commands\"}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Plain", keeper, "{" + base + ",\"onBuy\":\"\\tGIVE_PERM\\n\",\"givePermission\":\"op\"}").statusCode());
        verify(shop, times(0)).saveItem(any());
    }

    @Test
    void switchingAnItemToOrFromCommandsNeedsTheCommandsPermission() throws Exception {
        Login boss = login("boss", "boss-password");
        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"shopkeepers\",\"permissions\":[\"shop.edit\"]}").statusCode());
        accounts.put(new Account("keeper", hasher.hash("keeper-password".toCharArray()), "shopkeepers", false, 0));
        Login keeper = login("keeper", "keeper-password");
        var dormant = new com.l299l.newbedwars.api.shop.ShopItem("Dormant", "NETHER_STAR", 1, 2, "IRON", "GIVE_ITEM", "NONE", false, "",
                java.util.Map.of(), List.of(), List.of(), List.of("say hi"), "", "", "");
        var active = new com.l299l.newbedwars.api.shop.ShopItem("Active", "NETHER_STAR", 1, 2, "IRON", "COMMANDS", "NONE", false, "",
                java.util.Map.of(), List.of(), List.of(), List.of("say hi"), "", "", "");
        when(shop.getItem("Dormant")).thenReturn(CompletableFuture.completedFuture(Optional.of(dormant)));
        when(shop.getItem("Active")).thenReturn(CompletableFuture.completedFuture(Optional.of(active)));
        when(shop.saveItem(any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null)));
        String base = "\"material\":\"NETHER_STAR\",\"amount\":1,\"price\":2,\"priceType\":\"IRON\",\"logic\":\"NONE\",\"commands\":[\"say hi\"]";

        assertEquals(403, send("POST", "/api/shop/items/Dormant", keeper, "{" + base + ",\"onBuy\":\"COMMANDS\"}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Active", keeper, "{" + base + ",\"onBuy\":\"GIVE_ITEM\"}").statusCode());
        assertEquals(200, send("POST", "/api/shop/items/Dormant", keeper, "{" + base + ",\"onBuy\":\"GIVE_ITEM\"}").statusCode());
        assertEquals(403, send("POST", "/api/shop/items/Active", keeper, "{" + base + ",\"onBuy\":\"COMMANDS\"}").statusCode());
        assertEquals(200, send("POST", "/api/shop/items/Dormant", boss, "{" + base + ",\"onBuy\":\"COMMANDS\"}").statusCode());
    }

    @Test
    void shopGuisAreCreatedSavedAndArenasChanged() throws Exception {
        Login boss = login("boss", "boss-password");
        var ok = new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null);
        when(shop.createGui(any())).thenReturn(CompletableFuture.completedFuture(ok));
        when(shop.saveGui(any())).thenReturn(CompletableFuture.completedFuture(ok));
        when(shop.deleteGui("shop")).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.IN_USE, "arena:Castle")));
        when(shop.setArenaShops("Castle", "shop", null)).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.BUSY, null)));
        when(shop.getArenaShops()).thenReturn(CompletableFuture.completedFuture(List.of(
                new com.l299l.newbedwars.api.shop.ArenaShops("Castle", false, "shop", "upgrades"))));

        String gui = "{\"id\":\"fresh\",\"title\":\"&aShop\",\"size\":9,\"headerSize\":0,\"footerSize\":0,\"closeOnTransaction\":true,"
                + "\"fill\":\"empty\",\"empty\":\"empty\",\"slots\":[\"cat:a\",\"ci:Wool\",\"empty\",\"empty\",\"empty\",\"empty\",\"empty\",\"empty\",\"fill\"],"
                + "\"categories\":[{\"id\":\"a\",\"name\":\"A\",\"icon\":\"Wool\",\"description\":\"\",\"items\":[\"Wool\",\"upg:Forge\"]}]}";
        assertTrue(json(send("POST", "/api/shop/guis", boss, gui)).get("ok").getAsBoolean());
        var captor = org.mockito.ArgumentCaptor.forClass(com.l299l.newbedwars.api.shop.ShopGui.class);
        verify(shop).createGui(captor.capture());
        assertEquals("fresh", captor.getValue().id());
        assertEquals(9, captor.getValue().slots().size());
        assertEquals(List.of("Wool", "upg:Forge"), captor.getValue().categories().get(0).items());

        assertTrue(json(send("POST", "/api/shop/guis/other", boss, gui)).get("ok").getAsBoolean());
        verify(shop).saveGui(captor.capture());
        assertEquals("other", captor.getValue().id());

        JsonObject refused = json(send("POST", "/api/shop/guis/shop/delete", boss, "{}"));
        assertEquals("IN_USE", refused.get("code").getAsString());
        assertEquals("arena:Castle", refused.get("detail").getAsString());
        assertEquals("BUSY", json(send("POST", "/api/shop/arenas/Castle", boss, "{\"shopGui\":\"shop\"}")).get("code").getAsString());
        assertFalse(json(send("GET", "/api/shop/arenas", boss, null)).getAsJsonArray("arenas").get(0).getAsJsonObject().get("waiting").getAsBoolean());
    }

    @Test
    void upgradePricesAreListedAndSaved() throws Exception {
        Login boss = login("boss", "boss-password");
        Login helper = login("helper", "helper-password");
        when(shop.getUpgradePrices()).thenReturn(CompletableFuture.completedFuture(List.of(
                new com.l299l.newbedwars.api.shop.UpgradePrice("FORGE", List.of(
                        new com.l299l.newbedwars.api.shop.UpgradeTier(2, "DIAMOND")), 8, true))));
        when(shop.saveUpgradePrices(eq("FORGE"), any())).thenReturn(CompletableFuture.completedFuture(
                new com.l299l.newbedwars.api.shop.ShopResult(com.l299l.newbedwars.api.shop.ShopResult.Code.OK, null)));

        assertEquals(403, send("GET", "/api/shop/upgrades", helper, null).statusCode());
        JsonObject list = json(send("GET", "/api/shop/upgrades", boss, null));
        JsonObject forge = list.getAsJsonArray("upgrades").get(0).getAsJsonObject();
        assertEquals(8, forge.get("maxLevel").getAsInt());
        assertEquals("DIAMOND", forge.getAsJsonArray("tiers").get(0).getAsJsonObject().get("currency").getAsString());
        assertEquals(4, list.getAsJsonArray("currencies").size());

        assertTrue(json(send("POST", "/api/shop/upgrades/FORGE", boss,
                "{\"tiers\":[{\"price\":3,\"currency\":\"IRON\"},{\"price\":5,\"currency\":\"EMERALD\"}]}")).get("ok").getAsBoolean());
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<com.l299l.newbedwars.api.shop.UpgradeTier>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(shop).saveUpgradePrices(eq("FORGE"), captor.capture());
        assertEquals(List.of(new com.l299l.newbedwars.api.shop.UpgradeTier(3, "IRON"),
                new com.l299l.newbedwars.api.shop.UpgradeTier(5, "EMERALD")), captor.getValue());
        assertEquals(400, send("POST", "/api/shop/upgrades/FORGE", boss, "{\"tiers\":{}}").statusCode());
        assertEquals(400, send("POST", "/api/shop/upgrades/FORGE", boss, "{\"tiers\":[{\"price\":\"x\"}]}").statusCode());
        assertTrue(audit.recent(10).stream().anyMatch(e -> e.action().equals("shop.upgrade.save") && e.result().equals("OK")));
    }

    @Test
    void guiUpgradePriceOverridesAreParsed() {
        JsonObject body = com.google.gson.JsonParser.parseString("{\"title\":\"T\",\"size\":9,"
                + "\"upgradePrices\":{\"HASTE\":[{\"price\":1,\"currency\":\"GOLD\"}]}}").getAsJsonObject();
        var gui = ShopRoutes.parseGui(body, "g");
        assertEquals(java.util.Map.of("HASTE", List.of(new com.l299l.newbedwars.api.shop.UpgradeTier(1, "GOLD"))), gui.upgradePrices());
        assertNull(ShopRoutes.parseGui(com.google.gson.JsonParser.parseString("{\"size\":9}").getAsJsonObject(), "g").upgradePrices());
        assertEquals(1, ShopRoutes.gui(gui).getAsJsonObject("upgradePrices").getAsJsonArray("HASTE").size());
    }

    @Test
    void guiItemPricesAreParsedAndListed() {
        JsonObject body = com.google.gson.JsonParser.parseString("{\"title\":\"T\",\"size\":9,"
                + "\"itemPrices\":{\"IronSword\":{\"price\":7,\"currency\":\"GOLD\",\"amount\":2},"
                + "\"Wool\":{\"price\":3,\"currency\":null}}}").getAsJsonObject();
        var gui = ShopRoutes.parseGui(body, "g");
        assertEquals(java.util.Map.of("IronSword", new com.l299l.newbedwars.api.shop.ItemPrice(7, "GOLD", 2),
                "Wool", new com.l299l.newbedwars.api.shop.ItemPrice(3, null, 0)), gui.itemPrices());
        var kept = ShopRoutes.parseGui(com.google.gson.JsonParser.parseString("{\"size\":9}").getAsJsonObject(), "g");
        assertNull(kept.itemPrices());
        JsonObject sword = ShopRoutes.gui(gui).getAsJsonObject("itemPrices").getAsJsonObject("IronSword");
        assertEquals(7, sword.get("price").getAsInt());
        assertEquals("GOLD", sword.get("currency").getAsString());
        assertEquals(2, sword.get("amount").getAsInt());
        assertEquals(0, ShopRoutes.gui(kept).getAsJsonObject("itemPrices").size());
    }

    @Test
    void guiItemPricesRejectBadShapes() {
        assertThrows(com.l299l.newbedwars.panel.web.HttpError.class, () -> ShopRoutes.parseGui(
                com.google.gson.JsonParser.parseString("{\"itemPrices\":[]}").getAsJsonObject(), "g"));
        assertThrows(com.l299l.newbedwars.panel.web.HttpError.class, () -> ShopRoutes.parseGui(
                com.google.gson.JsonParser.parseString("{\"itemPrices\":{\"A\":{\"price\":\"x\"}}}").getAsJsonObject(), "g"));
    }

    @Test
    void lockedLoginIsAuditedOncePerLock() throws Exception {
        for (int i = 0; i < 3; i++) assertEquals(401, loginRaw("helper", "nope").statusCode());
        for (int i = 0; i < 5; i++) assertEquals(429, loginRaw("helper", "helper-password").statusCode());
        assertEquals(1, audit.recent(100).stream().filter(e -> "locked".equals(e.result())).count());
        assertEquals(3, audit.recent(100).stream().filter(e -> "failed".equals(e.result())).count());
    }

    @Test
    void deeplyNestedJsonIsABadRequest() throws Exception {
        String body = "{\"name\":" + "[".repeat(30_000) + "]".repeat(30_000) + "}";
        HttpResponse<String> r = send("POST", "/api/login", null, body);
        assertEquals(400, r.statusCode());
        assertEquals("bad_json", json(r).get("error").getAsString());
    }
}
