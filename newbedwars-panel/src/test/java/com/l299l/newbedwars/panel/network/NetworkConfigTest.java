package com.l299l.newbedwars.panel.network;

import com.l299l.newbedwars.panel.PanelConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigTest {
    private static final String FP = "AB".repeat(32);

    @Test
    void defaultsAreStandalone() {
        NetworkConfig config = NetworkConfig.read(key -> null);
        assertEquals(NetworkConfig.DEFAULTS, config);
        assertEquals(NetworkRole.STANDALONE, config.role());
        assertFalse(config.hasUsableSecret());
        assertEquals(NetworkConfig.DEFAULTS, PanelConfig.read(key -> null).network());
    }

    @Test
    void readsRolePeersAndRejectsBadOnes() {
        Map<String, Object> values = new HashMap<>();
        values.put("Network.Role", "gateway");
        values.put("Network.Id", "lobby");
        values.put("Network.Secret", " " + "s".repeat(40) + " ");
        values.put("Network.NodeLogin", true);
        Map<String, Object> peers = new LinkedHashMap<>();
        peers.put("game1", Map.of("Url", "https://10.0.0.5:8150/", "Fingerprint", ""));
        peers.put("game2", Map.of("Url", "http://game2.local:8150", "Fingerprint", "system"));
        peers.put("game3", Map.of("Url", "https://10.0.0.7:8150", "Fingerprint", FP.substring(0, 2) + ":" + FP.substring(2)));
        peers.put("bad id", Map.of("Url", "https://10.0.0.8:8150"));
        peers.put("game5", Map.of("Url", "ftp://10.0.0.8"));
        peers.put("game6", Map.of("Url", "https://10.0.0.9:8150/panel"));
        peers.put("game7", Map.of("Url", "https://10.0.0.9:8150", "Fingerprint", "not-a-fingerprint"));
        peers.put("game8", "text");
        peers.put("game.9", Map.of("Url", "https://10.0.0.9:8150"));
        peers.put("game10", Map.of("Url", "HTTPS://10.0.0.10:8150"));
        values.put("Network.Peers", peers);
        NetworkConfig config = NetworkConfig.read(values::get);
        assertEquals(NetworkRole.GATEWAY, config.role());
        assertEquals("lobby", config.id());
        assertTrue(config.hasUsableSecret());
        assertTrue(config.nodeLogin());
        assertEquals(4, config.peers().size());
        assertEquals("https://10.0.0.10:8150", config.peers().get(3).url());
        assertTrue(config.peers().get(3).https());
        assertFalse(config.peers().get(1).https());
        assertEquals("https://10.0.0.5:8150", config.peers().get(0).url());
        assertEquals("", config.peers().get(0).fingerprint());
        assertTrue(config.peers().get(1).systemCertificates());
        assertEquals(FP, config.peers().get(2).fingerprint());
        assertEquals(java.util.List.of("bad id", "game5", "game6", "game7", "game8", "game.9"), config.rejectedPeers());
    }

    @Test
    void unknownRoleAndBadIdFallBack() {
        Map<String, Object> values = new HashMap<>();
        values.put("Network.Role", "boss");
        values.put("Network.Id", "has space");
        NetworkConfig config = NetworkConfig.read(values::get);
        assertEquals(NetworkRole.STANDALONE, config.role());
        assertEquals("", config.id());
    }

    @Test
    void pinsSurviveARestart(@TempDir Path dir) {
        Path file = dir.resolve("peers.json");
        PeerPins pins = new PeerPins(file, Logger.getAnonymousLogger());
        assertNull(pins.get("game1"));
        pins.put("game1", FP);
        assertEquals(FP, new PeerPins(file, Logger.getAnonymousLogger()).get("game1"));
        assertTrue(pins.remove("game1"));
        assertFalse(pins.remove("game1"));
        assertNull(new PeerPins(file, Logger.getAnonymousLogger()).get("game1"));
    }

    @Test
    void brokenPinFileIsMovedAsideAndNotTrustedAgain(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("peers.json");
        Files.writeString(file, "{not json");
        PeerPins pins = new PeerPins(file, Logger.getAnonymousLogger());
        assertNull(pins.get("x"));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.filter(f -> f.getFileName().toString().startsWith("peers.json.corrupt-")).count());
        }
        assertFalse(pins.firstUseAllowed("x"));
        assertTrue(pins.remove("x"));
        assertTrue(pins.firstUseAllowed("x"));
        assertFalse(pins.firstUseAllowed("y"));
    }

    @Test
    void lockSurvivesARestartAndReleasesArePersisted(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("peers.json");
        Files.writeString(file, "{not json");
        new PeerPins(file, Logger.getAnonymousLogger());
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.isRegularFile(dir.resolve("peers.json.locked")));

        PeerPins restarted = new PeerPins(file, Logger.getAnonymousLogger());
        assertFalse(restarted.firstUseAllowed("x"));
        assertTrue(restarted.remove("x"));
        assertTrue(restarted.firstUseAllowed("x"));

        PeerPins again = new PeerPins(file, Logger.getAnonymousLogger());
        assertTrue(again.firstUseAllowed("x"));
        assertFalse(again.firstUseAllowed("y"));
    }

    @Test
    void goodPinsOfAPartlyBrokenFileSurviveARestart(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("peers.json");
        Files.writeString(file, "{\"game1\":\"" + FP + "\",\"game2\":{}}");
        PeerPins pins = new PeerPins(file, Logger.getAnonymousLogger());
        assertEquals(FP, pins.get("game1"));
        assertFalse(pins.firstUseAllowed("game2"));

        PeerPins restarted = new PeerPins(file, Logger.getAnonymousLogger());
        assertEquals(FP, restarted.get("game1"));
        assertFalse(restarted.firstUseAllowed("game2"));
        assertFalse(restarted.firstUseAllowed("game3"));
    }

    @Test
    void deletingTheLockFileClearsTheLock(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("peers.json");
        Files.writeString(file, "{not json");
        new PeerPins(file, Logger.getAnonymousLogger());
        Files.delete(dir.resolve("peers.json.locked"));
        assertTrue(new PeerPins(file, Logger.getAnonymousLogger()).firstUseAllowed("x"));
    }

    @Test
    void anUnreadableLockFileStaysLocked(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("peers.json.locked"), "garbage");
        assertFalse(new PeerPins(dir.resolve("peers.json"), Logger.getAnonymousLogger()).firstUseAllowed("x"));
    }

    @Test
    void oldPinFileWithoutLockAllowsFirstUse(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("peers.json");
        Files.writeString(file, "{\"game1\":\"" + FP + "\"}");
        PeerPins pins = new PeerPins(file, Logger.getAnonymousLogger());
        assertEquals(FP, pins.get("game1"));
        assertTrue(pins.firstUseAllowed("other"));
        assertFalse(Files.exists(dir.resolve("peers.json.locked")));
    }

    @Test
    void healthyPinFileAllowsFirstUse(@TempDir Path dir) {
        assertTrue(new PeerPins(dir.resolve("peers.json"), Logger.getAnonymousLogger()).firstUseAllowed("x"));
    }
}
