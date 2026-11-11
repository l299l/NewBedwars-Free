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

        assertEquals(200, send("POST", "/api/roles", boss, "{\"name\":\"managers\",\"permissions\":[\"dashboard.view\",\"stats.view\",\"users.manage\"]}").statusCode());
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
}
