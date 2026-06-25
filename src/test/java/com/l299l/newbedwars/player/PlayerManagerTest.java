package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerManagerTest {

    private static PlayerIns ins(String name) {
        return new PlayerIns(UUID.randomUUID(), name, Language.English, PlayerIns.defaultFastBuy());
    }

    // ── add / get ────────────────────────────────────────────────────────────

    @Test
    void addAndGetPlayerRoundTrip() {
        PlayerManager pm = new PlayerManager();
        PlayerIns p = ins("Alice");
        pm.addPlayer(p);
        assertSame(p, pm.getPlayer("Alice"));
    }

    @Test
    void getPlayerReturnsNullForUnknownName() {
        PlayerManager pm = new PlayerManager();
        assertNull(pm.getPlayer("nobody"));
    }

    // ── remove ───────────────────────────────────────────────────────────────

    @Test
    void removePlayerDeletesEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(ins("Alice"));
        pm.removePlayer("Alice");
        assertNull(pm.getPlayer("Alice"));
    }

    @Test
    void removePlayerForUnknownNameIsNoOp() {
        PlayerManager pm = new PlayerManager();
        assertDoesNotThrow(() -> pm.removePlayer("nobody"));
    }

    // ── updatePlayerLanguage ─────────────────────────────────────────────────

    @Test
    void updatePlayerLanguageUpdatesExistingEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerIns(UUID.randomUUID(), "Alice", Language.English, PlayerIns.defaultFastBuy()));
        pm.updatePlayerLanguage("Alice", Language.Polish);
        assertEquals(Language.Polish, pm.getPlayer("Alice").language());
    }

    // ── getPlayers ───────────────────────────────────────────────────────────

    @Test
    void getPlayersReturnsLiveMap() {
        PlayerManager pm = new PlayerManager();
        assertTrue(pm.getPlayers().isEmpty());
        pm.addPlayer(ins("Alice"));
        assertEquals(1, pm.getPlayers().size());
    }
}
