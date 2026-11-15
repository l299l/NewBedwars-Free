package com.l299l.newbedwars.panel.network;

import com.l299l.newbedwars.panel.auth.Permission;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class NodeAuthTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef-secret";
    private final AtomicLong now = new AtomicLong(1_000_000);
    private final NodeAuth.Verifier verifier = new NodeAuth.Verifier(SECRET, "game1", now::get);
    private final NodeAuth.Identity identity = new NodeAuth.Identity("alice", Set.of(Permission.ARENA_BACKUP, Permission.DASHBOARD_VIEW), "10.0.0.1", "lobby");

    private Map<String, String> signed(String method, String path, String query, String body) {
        NodeAuth.Call call = new NodeAuth.Call(method, path, query, now.get(), NodeAuth.nonce(), identity, body, "game1");
        return NodeAuth.headers(SECRET, call);
    }

    private NodeAuth.Verdict verify(String method, String path, String query, Map<String, String> headers) {
        return verifier.verify(method, path, query, headers::get);
    }

    @Test
    void acceptsAValidSignatureAndKeepsTheIdentity() {
        Map<String, String> headers = signed("POST", "/api/arenas/x/backup", "a=1", NodeAuth.sha256(new byte[0]));
        NodeAuth.Verdict verdict = verify("POST", "/api/arenas/x/backup", "a=1", headers);
        assertTrue(verdict.ok());
        assertEquals("alice@lobby", verdict.identity().account());
        assertEquals("10.0.0.1", verdict.identity().ip());
        assertEquals(Set.of(Permission.ARENA_BACKUP, Permission.DASHBOARD_VIEW), verdict.identity().permissions());
    }

    @Test
    void anyChangedPartInvalidatesTheSignature() {
        Map<String, String> headers = signed("POST", "/api/arenas/x/backup", "", "h");
        assertEquals(NodeAuth.Failure.SIGNATURE, verify("POST", "/api/arenas/y/backup", "", headers).failure());
        assertEquals(NodeAuth.Failure.SIGNATURE, verify("GET", "/api/arenas/x/backup", "", headers).failure());
        assertEquals(NodeAuth.Failure.SIGNATURE, verify("POST", "/api/arenas/x/backup", "x=1", headers).failure());
        for (String header : new String[]{NodeAuth.ACTOR, NodeAuth.GATEWAY, NodeAuth.IP, NodeAuth.BODY, NodeAuth.TIME, NodeAuth.NONCE, NodeAuth.TARGET}) {
            Map<String, String> copy = new HashMap<>(headers);
            copy.put(header, header.equals(NodeAuth.TIME) ? String.valueOf(now.get() + 1) : copy.get(header) + "0");
            assertFalse(verify("POST", "/api/arenas/x/backup", "", copy).ok(), header);
        }
        Map<String, String> escalated = new HashMap<>(headers);
        escalated.put(NodeAuth.PERMISSIONS, Permission.ARENA_BACKUP + "," + Permission.DASHBOARD_VIEW + "," + Permission.ARENA_DELETE);
        assertFalse(verify("POST", "/api/arenas/x/backup", "", escalated).ok());
    }

    @Test
    void aWrongSecretFails() {
        NodeAuth.Call call = new NodeAuth.Call("GET", "/api/dashboard", "", now.get(), NodeAuth.nonce(), identity, "h", "game1");
        Map<String, String> headers = NodeAuth.headers("another-secret-another-secret-another", call);
        assertEquals(NodeAuth.Failure.SIGNATURE, verify("GET", "/api/dashboard", "", headers).failure());
    }

    @Test
    void rejectsOldAndFutureRequests() {
        Map<String, String> headers = signed("GET", "/api/dashboard", "", "h");
        now.addAndGet(NodeAuth.WINDOW_MILLIS + 1);
        assertEquals(NodeAuth.Failure.CLOCK, verify("GET", "/api/dashboard", "", headers).failure());
        now.set(1_000_000);
        Map<String, String> future = signed("GET", "/api/dashboard", "", "h");
        now.addAndGet(-(NodeAuth.WINDOW_MILLIS + 1));
        assertEquals(NodeAuth.Failure.CLOCK, verify("GET", "/api/dashboard", "", future).failure());
    }

    @Test
    void aSignatureWorksOnlyOnce() {
        Map<String, String> headers = signed("GET", "/api/dashboard", "", "h");
        assertTrue(verify("GET", "/api/dashboard", "", headers).ok());
        assertEquals(NodeAuth.Failure.REPLAY, verify("GET", "/api/dashboard", "", headers).failure());
    }

    @Test
    void rejectsMissingAndMalformedHeadersWithoutLockingOut() {
        assertEquals(NodeAuth.Failure.MALFORMED, verify("GET", "/api/dashboard", "", new HashMap<>()).failure());
        Map<String, String> headers = signed("GET", "/api/dashboard", "", "h");
        headers.put(NodeAuth.ACTOR, "ali\nce");
        assertEquals(NodeAuth.Failure.MALFORMED, verify("GET", "/api/dashboard", "", headers).failure());
        for (int i = 0; i < 50; i++) {
            Map<String, String> junk = signed("GET", "/api/dashboard", "", "h");
            junk.put(NodeAuth.SIGNATURE, "00");
            assertEquals(NodeAuth.Failure.SIGNATURE, verify("GET", "/api/dashboard", "", junk).failure());
        }
        assertTrue(verify("GET", "/api/dashboard", "", signed("GET", "/api/dashboard", "", "h")).ok());
    }

    @Test
    void aRequestForAnotherServerIsRefused() {
        NodeAuth.Call call = new NodeAuth.Call("POST", "/api/arenas/x/delete", "", now.get(), NodeAuth.nonce(), identity, "h", "game2");
        Map<String, String> headers = NodeAuth.headers(SECRET, call);
        NodeAuth.Verdict verdict = verify("POST", "/api/arenas/x/delete", "", headers);
        assertEquals(NodeAuth.Failure.TARGET, verdict.failure());
        assertEquals("game2", verdict.body());
    }

    @Test
    void unknownPermissionsFromANewerGatewayAreIgnored() {
        NodeAuth.Call call = new NodeAuth.Call("GET", "/api/dashboard", "", now.get(), NodeAuth.nonce(), identity, "h", "game1");
        String perms = Permission.DASHBOARD_VIEW + ",future.permission," + Permission.ARENA_BACKUP;
        Map<String, String> headers = new HashMap<>(NodeAuth.headers(SECRET, call));
        headers.put(NodeAuth.PERMISSIONS, perms);
        headers.put(NodeAuth.SIGNATURE, NodeAuth.sign(SECRET, call, perms));
        NodeAuth.Verdict verdict = verify("GET", "/api/dashboard", "", headers);
        assertTrue(verdict.ok());
        assertEquals(Set.of(Permission.ARENA_BACKUP, Permission.DASHBOARD_VIEW), verdict.identity().permissions());
    }

    @Test
    void localOnlyPermissionsAreNeverForwarded() {
        Set<String> forwarded = NodeAuth.forwardable(Set.copyOf(Permission.ALL));
        assertTrue(forwarded.contains(Permission.ARENA_DELETE));
        for (String local : Permission.LOCAL_ONLY) assertFalse(forwarded.contains(local), local);
    }
}
