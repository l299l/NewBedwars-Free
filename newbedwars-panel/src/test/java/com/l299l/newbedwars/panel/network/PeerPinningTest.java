package com.l299l.newbedwars.panel.network;

import com.l299l.newbedwars.panel.auth.AccountStore;
import com.l299l.newbedwars.panel.auth.Permission;
import com.l299l.newbedwars.panel.auth.SessionManager;
import com.l299l.newbedwars.panel.tls.TlsSetup;
import com.l299l.newbedwars.panel.web.PanelServer;
import com.l299l.newbedwars.panel.web.Response;
import com.l299l.newbedwars.panel.web.Router;
import com.l299l.newbedwars.panel.web.SessionCookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PeerPinningTest {
    private static final String SECRET = "pinning-secret-pinning-secret-0123456789";

    @TempDir
    Path dir;

    private final List<PanelServer> servers = new ArrayList<>();
    private final Logger logger = Logger.getAnonymousLogger();
    private final NodeAuth.Identity monitor = new NodeAuth.Identity("gateway", Set.of(Permission.DASHBOARD_VIEW), "", "lobby");

    @AfterEach
    void stop() {
        servers.forEach(PanelServer::stop);
    }

    private record Node(int port, String fingerprint) {}

    private Node startNode(String name) throws Exception {
        TlsSetup.Result tls = TlsSetup.selfSigned(dir.resolve(name + ".p12"), new byte[32], List.of("127.0.0.1"), new Date());
        Router router = new Router();
        router.allowed("GET", "/api/dashboard", Set.of(Permission.DASHBOARD_VIEW), r -> Response.ok());
        PanelServer server = new PanelServer(router, new SessionManager(60_000, 600_000, System::currentTimeMillis),
                AccountStore.open(dir.resolve(name + ".dat"), new byte[32]), new SessionCookie(true, false), false, logger,
                new NodeAuth.Verifier(SECRET, "game1", System::currentTimeMillis));
        server.start(new InetSocketAddress("127.0.0.1", 0), tls.context());
        servers.add(server);
        return new Node(server.port(), tls.fingerprint().replace(":", ""));
    }

    private PeerClient.Reply call(PeerClient client) throws Exception {
        PeerClient.Reply reply = client.send("GET", "/api/dashboard", "", monitor, NodeAuth.sha256(new byte[0]),
                new PeerClient.Payload.None(), Duration.ofSeconds(5));
        reply.body().close();
        return reply;
    }

    @Test
    void trustsTheFirstCertificateAndRefusesAnotherOne() throws Exception {
        Node first = startNode("first");
        Node second = startNode("second");
        assertNotEquals(first.fingerprint(), second.fingerprint());
        PeerPins pins = new PeerPins(dir.resolve("peers.json"), logger);

        PeerClient client = new PeerClient(new NetworkConfig.Peer("game1", "https://127.0.0.1:" + first.port(), ""), SECRET, pins, logger);
        assertEquals("none", client.pinSource());
        assertEquals(200, call(client).status());
        assertEquals(first.fingerprint(), pins.get("game1"));
        assertEquals("first-use", client.pinSource());
        assertEquals(200, call(client).status());

        PeerClient swapped = new PeerClient(new NetworkConfig.Peer("game1", "https://127.0.0.1:" + second.port(), ""), SECRET, pins, logger);
        PeerException e = assertThrows(PeerException.class, () -> call(swapped));
        assertEquals(PeerException.Kind.UNTRUSTED, e.kind());
        assertEquals(first.fingerprint(), pins.get("game1"));

        assertTrue(pins.remove("game1"));
        assertEquals(200, call(swapped).status());
        assertEquals(second.fingerprint(), pins.get("game1"));
    }

    @Test
    void aConfiguredFingerprintMustMatch() throws Exception {
        Node node = startNode("node");
        PeerPins pins = new PeerPins(dir.resolve("peers.json"), logger);
        String url = "https://127.0.0.1:" + node.port();
        PeerClient right = new PeerClient(new NetworkConfig.Peer("game1", url, node.fingerprint()), SECRET, pins, logger);
        assertEquals("config", right.pinSource());
        assertEquals(200, call(right).status());
        assertNull(pins.get("game1"));

        PeerClient wrong = new PeerClient(new NetworkConfig.Peer("game1", url, "AB".repeat(32)), SECRET, pins, logger);
        assertEquals(PeerException.Kind.UNTRUSTED, assertThrows(PeerException.class, () -> call(wrong)).kind());
    }

    @Test
    void aSelfSignedCertificateIsNotTrustedWhenTheSystemStoreIsRequested() throws Exception {
        Node node = startNode("node");
        PeerClient system = new PeerClient(new NetworkConfig.Peer("game1", "https://127.0.0.1:" + node.port(), "system"), SECRET,
                new PeerPins(dir.resolve("peers.json"), logger), logger);
        assertEquals("system", system.pinSource());
        assertThrows(PeerException.class, () -> call(system));
    }
}
