package com.l299l.newbedwars.panel.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l299l.newbedwars.api.maintenance.ArenaMaintenance;
import com.l299l.newbedwars.api.maintenance.MaintenanceResult;
import com.l299l.newbedwars.panel.AuditLog;
import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.LoginLimiter;
import com.l299l.newbedwars.panel.auth.PasswordHasher;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.Role;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.command.UserCommands;
import com.l299l.newbedwars.panel.network.NetworkConfig;
import com.l299l.newbedwars.panel.network.NetworkRole;
import com.l299l.newbedwars.panel.network.NetworkService;
import com.l299l.newbedwars.panel.network.NodeAuth;
import com.l299l.newbedwars.panel.network.PeerClient;
import com.l299l.newbedwars.panel.network.PeerPins;
import com.l299l.newbedwars.panel.web.PanelServer;
import com.l299l.newbedwars.panel.web.Response;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NetworkRoutesTest {
    private static final String SECRET = "network-secret-network-secret-0123456789";

    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private final PasswordHasher hasher = new PasswordHasher(1);
    private ArenaMaintenance nodeMaintenance;
    private ArenaMaintenance gatewayMaintenance;
    private AuditLog nodeAudit;
    private PanelServer node;
    private PanelServer gateway;
    private NetworkService network;
    private String nodeBase;
    private String gatewayBase;

    record Login(String cookie, String csrf) {}

    private static MaintenanceResult ok(String arena, String detail) {
        return new MaintenanceResult(MaintenanceResult.Code.OK, arena, "done", detail);
    }

    @BeforeEach
    void setUp() throws Exception {
        nodeMaintenance = mock(ArenaMaintenance.class);
        when(nodeMaintenance.getExportFolder()).thenReturn(dir.resolve("node-exports"));
        when(nodeMaintenance.backup(anyString())).thenAnswer(i -> CompletableFuture.completedFuture(ok(i.getArgument(0), "20261112-120000")));
        when(nodeMaintenance.importArena(anyString())).thenAnswer(i -> CompletableFuture.completedFuture(ok("castle", null)));
        nodeAudit = new AuditLog(dir.resolve("node-audit.log"), Logger.getAnonymousLogger(), System::currentTimeMillis);
        AccountStore nodeAccounts = AccountStore.open(dir.resolve("node-accounts.dat"), new byte[32]);
        SessionManager nodeSessions = new SessionManager(60_000, 600_000, System::currentTimeMillis);
        SessionCookie cookies = new SessionCookie(false, false);
        Router nodeRouter = new Router();
        new AuthRoutes(nodeAccounts, nodeSessions, new LoginLimiter(3, 60_000, System::currentTimeMillis), hasher, cookies, nodeAudit, "en",
                new AuthRoutes.NetworkInfo(NetworkRole.NODE, null, false)).register(nodeRouter);
        new ArenaRoutes(nodeMaintenance, nodeAudit, 1024 * 1024).register(nodeRouter);
        new AdminRoutes(nodeAccounts, new UserCommands(nodeAccounts, nodeSessions, hasher, nodeAudit, System::currentTimeMillis), nodeAudit).register(nodeRouter);
        nodeRouter.allowed("GET", "/api/dashboard", Set.of(Permission.DASHBOARD_VIEW), r -> {
            if ("7".equals(r.query("rev"))) return Response.noContent();
            JsonObject server = new JsonObject();
            server.addProperty("online", 2);
            server.addProperty("maxPlayers", 20);
            server.addProperty("plugin", "2.7-beta");
            server.add("tps", new JsonArray());
            JsonObject arena = new JsonObject();
            arena.addProperty("name", "castle");
            JsonObject data = new JsonObject();
            data.add("server", server);
            data.add("arenas", array(arena));
            data.add("players", new JsonArray());
            data.add("network", new JsonArray());
            JsonObject body = new JsonObject();
            body.addProperty("rev", 7);
            body.add("data", data);
            return Response.ok(body);
        });
        node = new PanelServer(nodeRouter, nodeSessions, nodeAccounts, cookies, false, Logger.getAnonymousLogger(),
                new NodeAuth.Verifier(SECRET, "game1", System::currentTimeMillis));
        node.start(new InetSocketAddress("127.0.0.1", 0), null);
        nodeBase = "http://127.0.0.1:" + node.port();

        gatewayMaintenance = mock(ArenaMaintenance.class);
        when(gatewayMaintenance.getExportFolder()).thenReturn(dir.resolve("gateway-exports"));
        AccountStore accounts = AccountStore.open(dir.resolve("accounts.dat"), new byte[32]);
        accounts.putRole(Role.custom("netops", Set.of(Permission.DASHBOARD_VIEW, Permission.ARENA_BACKUP, Permission.NETWORK_SERVERS)));
        accounts.put(new Account("boss", hasher.hash("boss-password".toCharArray()), "admin", false, 0));
        accounts.put(new Account("keeper", hasher.hash("keeper-password".toCharArray()), "netops", false, 0));
        accounts.putRole(Role.custom("localonly", Set.of(Permission.DASHBOARD_VIEW)));
        accounts.put(new Account("helper", hasher.hash("helper-password".toCharArray()), "staff", false, 0));
        accounts.put(new Account("localguy", hasher.hash("localguy-password".toCharArray()), "localonly", false, 0));
        SessionManager sessions = new SessionManager(60_000, 600_000, System::currentTimeMillis);
        AuditLog audit = new AuditLog(dir.resolve("audit.log"), Logger.getAnonymousLogger(), System::currentTimeMillis);
        Router router = new Router();
        new AuthRoutes(accounts, sessions, new LoginLimiter(3, 60_000, System::currentTimeMillis), hasher, cookies, audit, "en",
                new AuthRoutes.NetworkInfo(NetworkRole.GATEWAY, "lobby", false)).register(router);
        new ArenaRoutes(gatewayMaintenance, audit, 1024 * 1024).register(router);
        new AdminRoutes(accounts, new UserCommands(accounts, sessions, hasher, audit, System::currentTimeMillis), audit).register(router);
        PeerPins pins = new PeerPins(dir.resolve("peers.json"), Logger.getAnonymousLogger());
        Logger logger = Logger.getAnonymousLogger();
        network = new NetworkService("lobby", List.of(
                new PeerClient(new NetworkConfig.Peer("game1", nodeBase, ""), SECRET, pins, logger),
                new PeerClient(new NetworkConfig.Peer("wrongsecret", nodeBase, ""), SECRET + "x", pins, logger),
                new PeerClient(new NetworkConfig.Peer("down", "http://127.0.0.1:1", ""), SECRET, pins, logger)),
                System::currentTimeMillis, logger);
        new NetworkRoutes(router, network, () -> {
            JsonObject lobby = new JsonObject();
            JsonObject arena = new JsonObject();
            arena.addProperty("server", "game9");
            arena.addProperty("name", "tower");
            lobby.add("network", array(arena));
            return lobby;
        }).register(router);
        gateway = new PanelServer(router, sessions, accounts, cookies, false, Logger.getAnonymousLogger());
        gateway.start(new InetSocketAddress("127.0.0.1", 0), null);
        gatewayBase = "http://127.0.0.1:" + gateway.port();
    }

    @AfterEach
    void tearDown() {
        gateway.stop();
        node.stop();
        network.stop();
    }

    private static JsonArray array(JsonObject o) {
        JsonArray a = new JsonArray();
        a.add(o);
        return a;
    }

    private static JsonObject json(HttpResponse<String> r) {
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private HttpResponse<String> send(String base, String method, String path, Login login, boolean csrf, HttpRequest.BodyPublisher body,
                                      Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).method(method, body);
        if (login != null) {
            b.header("Cookie", login.cookie());
            if (csrf) b.header("X-CSRF-Token", login.csrf());
        }
        headers.forEach(b::header);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> gateway(String method, String path, Login login, String body) throws Exception {
        return send(gatewayBase, method, path, login, true,
                body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body), Map.of());
    }

    private Login login(String name, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("password", password);
        HttpResponse<String> r = gateway("POST", "/api/login", null, body.toString());
        assertEquals(200, r.statusCode(), r.body());
        String cookie = r.headers().firstValue("Set-Cookie").orElseThrow();
        return new Login(cookie.substring(0, cookie.indexOf(';')), json(r).get("csrf").getAsString());
    }

    private JsonObject server(JsonObject overview, String id) {
        for (var e : overview.getAsJsonArray("servers")) {
            if (e.getAsJsonObject().get("id").getAsString().equals(id)) return e.getAsJsonObject();
        }
        return fail("no server " + id);
    }

    @Test
    void infoTellsTheRole() throws Exception {
        JsonObject info = json(gateway("GET", "/api/info", null, null));
        assertEquals("GATEWAY", info.get("networkRole").getAsString());
        assertEquals("lobby", info.get("serverId").getAsString());
        JsonObject nodeInfo = json(send(nodeBase, "GET", "/api/info", null, false, HttpRequest.BodyPublishers.noBody(), Map.of()));
        assertEquals("NODE", nodeInfo.get("networkRole").getAsString());
        assertFalse(nodeInfo.get("nodeLogin").getAsBoolean());
    }

    @Test
    void overviewListsTheLocalServerPeersAndServersWithoutAPeer() throws Exception {
        Login keeper = login("keeper", "keeper-password");
        HttpResponse<String> response = gateway("GET", "/api/network", keeper, null);
        assertEquals(200, response.statusCode(), response.body());
        JsonObject overview = json(response);
        assertEquals("lobby", overview.get("self").getAsString());
        assertTrue(server(overview, "lobby").get("local").getAsBoolean());
        JsonObject game1 = server(overview, "game1");
        assertEquals("ONLINE", game1.get("status").getAsString());
        assertEquals("castle", game1.getAsJsonObject("data").getAsJsonArray("arenas").get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("REJECTED", server(overview, "wrongsecret").get("status").getAsString());
        assertEquals("OFFLINE", server(overview, "down").get("status").getAsString());
        JsonObject lost = server(overview, "game9");
        assertEquals("NOT_CONNECTED", lost.get("status").getAsString());
        assertEquals("tower", lost.getAsJsonArray("networkArenas").get(0).getAsJsonObject().get("name").getAsString());

        assertEquals(200, gateway("GET", "/api/network", login("helper", "helper-password"), null).statusCode());
        assertEquals(403, gateway("GET", "/api/network", login("localguy", "localguy-password"), null).statusCode());
        assertEquals(401, gateway("GET", "/api/network", null, null).statusCode());
    }

    @Test
    void forwardsAnActionAsTheGatewayUser() throws Exception {
        Login keeper = login("keeper", "keeper-password");
        HttpResponse<String> response = gateway("POST", "/api/s/game1/arenas/castle/backup", keeper, "{}");
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("20261112-120000", json(response).get("detail").getAsString());
        verify(nodeMaintenance).backup("castle");
        verifyNoInteractions(gatewayMaintenance);
        AuditLog.Entry entry = nodeAudit.recent(5).stream().filter(e -> e.action().equals("arena.backup")).findFirst().orElseThrow();
        assertEquals("keeper@lobby", entry.user());
        assertEquals("127.0.0.1", entry.ip());
        assertEquals("castle", entry.target());
    }

    @Test
    void theGatewayChecksEveryPermissionBeforeForwarding() throws Exception {
        Login keeper = login("keeper", "keeper-password");
        assertEquals(403, gateway("POST", "/api/s/game1/arenas/castle/delete", keeper, "{}").statusCode());
        assertEquals(403, gateway("GET", "/api/s/game1/users", login("boss", "boss-password"), null).statusCode());
        assertEquals(403, gateway("GET", "/api/s/game1/audit", login("boss", "boss-password"), null).statusCode());
        assertEquals(403, gateway("POST", "/api/s/game1/arenas/castle/backup", login("helper", "helper-password"), "{}").statusCode());
        assertEquals(404, gateway("POST", "/api/s/game1/nothing/here", keeper, "{}").statusCode());
        HttpResponse<String> unknown = gateway("POST", "/api/s/nope/arenas/castle/backup", keeper, "{}");
        assertEquals(404, unknown.statusCode());
        assertEquals("unknown_server", json(unknown).get("error").getAsString());
        assertEquals("local_server", json(gateway("POST", "/api/s/lobby/arenas/castle/backup", keeper, "{}")).get("error").getAsString());
        HttpResponse<String> noCsrf = send(gatewayBase, "POST", "/api/s/game1/arenas/castle/backup", keeper, false,
                HttpRequest.BodyPublishers.ofString("{}"), Map.of());
        assertEquals(403, noCsrf.statusCode());
        assertEquals(401, gateway("POST", "/api/s/game1/arenas/castle/backup", null, "{}").statusCode());
        verifyNoInteractions(nodeMaintenance);
    }

    @Test
    void unreachableAndRejectedServersAreBadGateways() throws Exception {
        Login boss = login("boss", "boss-password");
        HttpResponse<String> down = gateway("POST", "/api/s/down/arenas/castle/backup", boss, "{}");
        assertEquals(502, down.statusCode());
        assertEquals("server_unreachable", json(down).get("error").getAsString());
        HttpResponse<String> rejected = gateway("POST", "/api/s/wrongsecret/arenas/castle/backup", boss, "{}");
        assertEquals(502, rejected.statusCode());
        assertEquals("server_rejected", json(rejected).get("error").getAsString());
    }

    @Test
    void uploadsAndDownloadsStreamThroughTheGateway() throws Exception {
        Login boss = login("boss", "boss-password");
        HttpResponse<String> upload = gateway("POST", "/api/s/game1/import?name=castle.zip", boss, "PKzip-bytes");
        assertEquals(200, upload.statusCode(), upload.body());
        verify(nodeMaintenance).importArena("castle.zip");
        assertEquals("PKzip-bytes", Files.readString(dir.resolve("node-exports/castle.zip")));
        assertFalse(Files.exists(dir.resolve("gateway-exports")));

        HttpResponse<String> download = gateway("GET", "/api/s/game1/exports/castle.zip", boss, null);
        assertEquals(200, download.statusCode());
        assertEquals("PKzip-bytes", download.body());
        assertTrue(download.headers().firstValue("Content-Disposition").orElseThrow().contains("castle.zip"));
        assertEquals(400, gateway("GET", "/api/s/game1/exports/..%2Fnode-accounts.dat", boss, null).statusCode());
    }

    @Test
    void theNodeOnlyAnswersSignedRequests() throws Exception {
        HttpResponse<String> plain = send(nodeBase, "POST", "/api/arenas/castle/backup", null, false, HttpRequest.BodyPublishers.ofString("{}"), Map.of());
        assertEquals(401, plain.statusCode());
        assertEquals("login_required", json(plain).get("error").getAsString());

        NodeAuth.Identity admin = new NodeAuth.Identity("keeper", NodeAuth.forwardable(Set.copyOf(Permission.ALL)), "10.1.1.1", "lobby");
        String emptyHash = NodeAuth.sha256(new byte[0]);
        Map<String, String> good = NodeAuth.headers(SECRET, new NodeAuth.Call("POST", "/api/arenas/castle/backup", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, emptyHash, "game1"));
        HttpResponse<String> first = send(nodeBase, "POST", "/api/arenas/castle/backup", null, false, HttpRequest.BodyPublishers.noBody(), good);
        assertEquals(200, first.statusCode(), first.body());
        assertEquals("10.1.1.1", nodeAudit.recent(1).get(0).ip());
        assertEquals(401, send(nodeBase, "POST", "/api/arenas/castle/backup", null, false, HttpRequest.BodyPublishers.noBody(), good).statusCode());

        Map<String, String> otherPath = NodeAuth.headers(SECRET, new NodeAuth.Call("POST", "/api/arenas/castle/backup", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, emptyHash, "game1"));
        assertEquals(401, send(nodeBase, "POST", "/api/arenas/other/backup", null, false, HttpRequest.BodyPublishers.noBody(), otherPath).statusCode());

        Map<String, String> tamperedBody = NodeAuth.headers(SECRET, new NodeAuth.Call("POST", "/api/arenas/castle/restore", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, NodeAuth.sha256("{\"backup\":\"a\"}".getBytes()), "game1"));
        assertEquals(401, send(nodeBase, "POST", "/api/arenas/castle/restore", null, false, HttpRequest.BodyPublishers.ofString("{\"backup\":\"b\"}"), tamperedBody).statusCode());

        Map<String, String> unsigned = NodeAuth.headers(SECRET, new NodeAuth.Call("POST", "/api/arenas/castle/restore", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, NodeAuth.UNSIGNED, "game1"));
        assertEquals(401, send(nodeBase, "POST", "/api/arenas/castle/restore", null, false, HttpRequest.BodyPublishers.ofString("{}"), unsigned).statusCode());

        Map<String, String> users = NodeAuth.headers(SECRET, new NodeAuth.Call("GET", "/api/users", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, emptyHash, "game1"));
        HttpResponse<String> local = send(nodeBase, "GET", "/api/users", null, false, HttpRequest.BodyPublishers.noBody(), users);
        assertEquals(403, local.statusCode());
        assertEquals("node_forbidden", json(local).get("error").getAsString());

        Map<String, String> me = NodeAuth.headers(SECRET, new NodeAuth.Call("GET", "/api/me", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, emptyHash, "game1"));
        assertEquals(403, send(nodeBase, "GET", "/api/me", null, false, HttpRequest.BodyPublishers.noBody(), me).statusCode());

        JsonObject login = new JsonObject();
        login.addProperty("name", "someone");
        login.addProperty("password", "whatever-password");
        HttpResponse<String> denied = send(nodeBase, "POST", "/api/login", null, false, HttpRequest.BodyPublishers.ofString(login.toString()), Map.of());
        assertEquals(403, denied.statusCode());
        assertEquals("node_login_disabled", json(denied).get("error").getAsString());
    }

    @Test
    void aPanelThatIsNoNodeIgnoresSignedRequests() throws Exception {
        NodeAuth.Identity admin = new NodeAuth.Identity("keeper", Set.of(Permission.DASHBOARD_VIEW), "10.1.1.1", "lobby");
        Map<String, String> headers = NodeAuth.headers(SECRET, new NodeAuth.Call("GET", "/api/network", "", System.currentTimeMillis(), NodeAuth.nonce(), admin, NodeAuth.sha256(new byte[0]), "game1"));
        HttpResponse<String> response = send(gatewayBase, "GET", "/api/network", null, false, HttpRequest.BodyPublishers.noBody(), headers);
        assertEquals(403, response.statusCode());
        assertEquals("not_a_node", json(response).get("error").getAsString());
    }

    @Test
    void theRouterMatchesTheTailOfAPath() {
        Router router = new Router();
        router.open("GET", "/api/s/{server}/{*rest}", r -> Response.ok());
        Router.Match match = router.match("GET", "/api/s/game%201/arenas/a%2Fb/backups");
        assertNotNull(match);
        assertEquals("game 1", match.params().get("server"));
        assertEquals("arenas/a%2Fb/backups", match.params().get("rest"));
        assertNull(router.match("GET", "/api/s/game1"));
        assertNull(router.match("GET", "/api/s/game1/"));
        assertEquals("x", router.match("GET", "/api/s/g/x").params().get("rest"));
    }
}
