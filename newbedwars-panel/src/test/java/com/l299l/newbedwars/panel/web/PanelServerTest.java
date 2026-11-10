package com.l299l.newbedwars.panel.web;

import com.l299l.newbedwars.panel.auth.Account;
import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.tls.TlsSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PanelServerTest {
    @TempDir
    Path dir;

    private final HttpClient client = HttpClient.newHttpClient();
    private AccountStore accounts;
    private SessionManager sessions;
    private PanelServer server;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        accounts = AccountStore.open(dir.resolve("accounts.dat"), new byte[32]);
        accounts.put(new Account("boss", "x", "admin", false, 0));
        accounts.put(new Account("helper", "x", "staff", false, 0));
        accounts.put(new Account("fresh", "x", "admin", true, 0));
        sessions = new SessionManager(60_000, 600_000, System::currentTimeMillis);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
    }

    private void startHttp() throws Exception {
        Router router = new Router();
        router.open("GET", "/api/ping", r -> Response.ok());
        router.loggedIn("GET", "/api/me", r -> Response.bytes("text/plain", r.user().getBytes()));
        router.allowed("GET", "/api/view", Set.of(Permission.DASHBOARD_VIEW), r -> Response.ok());
        router.allowed("POST", "/api/arenas/{name}/backup", Set.of(Permission.ARENA_BACKUP),
                r -> Response.bytes("text/plain", r.param("name").getBytes()));
        router.allowed("GET", "/api/fail", Set.of(Permission.DASHBOARD_VIEW), r -> {
            throw new IllegalStateException("boom");
        });
        server = new PanelServer(router, sessions, accounts, new SessionCookie(false, false), false, Logger.getAnonymousLogger());
        server.start(new InetSocketAddress("127.0.0.1", 0), null);
        base = "http://127.0.0.1:" + server.port();
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder get(String path, SessionManager.Session session) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        if (session != null) b.header("Cookie", "other=1; nbw=" + session.id());
        return b;
    }

    private HttpRequest.Builder post(String path, SessionManager.Session session, String csrf) {
        HttpRequest.Builder b = get(path, session).POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (csrf != null) b.header("X-CSRF-Token", csrf);
        return b;
    }

    @Test
    void accessRulesAreCheckedOnTheServer() throws Exception {
        startHttp();
        SessionManager.Session boss = sessions.create("boss");
        SessionManager.Session helper = sessions.create("helper");
        SessionManager.Session fresh = sessions.create("fresh");

        assertEquals(200, send(get("/api/ping", null)).statusCode());
        assertEquals(401, send(get("/api/view", null)).statusCode());
        assertTrue(send(get("/api/view", null)).body().contains("login_required"));
        assertEquals(401, send(get("/api/view", new SessionManager.Session("forged", "boss", "c", 0, 0))).statusCode());

        assertEquals("boss", send(get("/api/me", boss)).body());
        assertEquals(200, send(get("/api/view", helper)).statusCode());

        assertEquals("castle", send(post("/api/arenas/castle/backup", boss, boss.csrf())).body());
        assertEquals(403, send(post("/api/arenas/castle/backup", boss, null)).statusCode());
        assertTrue(send(post("/api/arenas/castle/backup", boss, helper.csrf())).body().contains("csrf"));
        assertTrue(send(post("/api/arenas/castle/backup", helper, helper.csrf())).body().contains("forbidden"));

        HttpResponse<String> mustChange = send(get("/api/view", fresh));
        assertEquals(403, mustChange.statusCode());
        assertTrue(mustChange.body().contains("password_change_required"));
        assertEquals("fresh", send(get("/api/me", fresh)).body());

        accounts.remove("helper");
        assertEquals(401, send(get("/api/view", helper)).statusCode());
        assertTrue(sessions.get(helper.id()).isEmpty());
    }

    @Test
    void routingErrorsAndHeaders() throws Exception {
        startHttp();
        SessionManager.Session boss = sessions.create("boss");
        assertEquals(404, send(get("/api/nothing", boss)).statusCode());
        assertEquals(405, send(post("/api/view", boss, boss.csrf())).statusCode());
        HttpResponse<String> fail = send(get("/api/fail", boss));
        assertEquals(500, fail.statusCode());
        assertFalse(fail.body().contains("boom"));

        HttpResponse<String> bad = send(post("/api/arenas/castle/backup", boss, boss.csrf()).header("Origin", "https://evil.example"));
        assertEquals(403, bad.statusCode());
        assertTrue(bad.body().contains("bad_origin"));
        assertEquals(200, send(post("/api/arenas/castle/backup", boss, boss.csrf())
                .header("Origin", "http://127.0.0.1:" + server.port())).statusCode());

        HttpResponse<String> ping = send(get("/api/ping", null));
        assertTrue(ping.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'self'"));
        assertEquals("DENY", ping.headers().firstValue("X-Frame-Options").orElse(""));
        assertEquals("nosniff", ping.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals("no-store", ping.headers().firstValue("Cache-Control").orElse(""));
        assertTrue(ping.headers().firstValue("Strict-Transport-Security").isEmpty());
    }

    @Test
    void staticFilesStayInsideTheWebFolder() throws Exception {
        startHttp();
        assertEquals(404, send(get("/../plugin.yml", null)).statusCode());
        assertEquals(404, send(get("/%2e%2e/plugin.yml", null)).statusCode());
        assertEquals(404, send(get("/plugin.yml", null)).statusCode());
        assertEquals(404, send(get("/missing.js", null)).statusCode());
        assertEquals(405, send(get("/index.html", null).POST(HttpRequest.BodyPublishers.noBody())).statusCode());
    }

    @Test
    void servesHttpsWithTheSelfSignedCertificate() throws Exception {
        TlsSetup.Result tls = TlsSetup.selfSigned(dir.resolve("selfsigned.p12"), new byte[32], List.of("localhost", "127.0.0.1"), new Date());
        Router router = new Router();
        router.open("GET", "/api/ping", r -> Response.ok());
        server = new PanelServer(router, sessions, accounts, new SessionCookie(true, false), false, Logger.getAnonymousLogger());
        server.start(new InetSocketAddress("127.0.0.1", 0), tls.context());

        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("panel", tls.certificate());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        HttpClient https = HttpClient.newBuilder().sslContext(context).build();
        HttpResponse<String> response = https.send(HttpRequest.newBuilder(URI.create("https://localhost:" + server.port() + "/api/ping")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"ok\":true"));

        assertThrows(Exception.class, () -> client.send(HttpRequest.newBuilder(URI.create("https://localhost:" + server.port() + "/api/ping")).build(),
                HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void csrfComparison() {
        assertTrue(PanelServer.csrfMatches("abc", "abc"));
        assertFalse(PanelServer.csrfMatches("abc", "abd"));
        assertFalse(PanelServer.csrfMatches("abc", null));
        assertFalse(PanelServer.csrfMatches(null, "abc"));
    }
}
