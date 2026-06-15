package com.l299l.newbedwars.player;

import com.l299l.newbedwars.config.Language;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerManagerTest {

    private static PlayerIns ins(String name) {
        return new PlayerIns(UUID.randomUUID(), name, Language.English, "shopGui", "upgradeGui");
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

    // ── updatePlayerShopGui ──────────────────────────────────────────────────

    @Test
    void updatePlayerShopGuiUpdatesExistingEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerIns(UUID.randomUUID(), "Alice", Language.English, "oldShop", "ug"));
        pm.updatePlayerShopGui("Alice", "newShop");
        assertEquals("newShop", pm.getPlayer("Alice").shopGui());
    }

    @Test
    void updatePlayerShopGuiPreservesOtherFields() {
        UUID id = UUID.randomUUID();
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerIns(id, "Alice", Language.Polish, "oldShop", "ug"));
        pm.updatePlayerShopGui("Alice", "newShop");
        PlayerIns updated = pm.getPlayer("Alice");
        assertEquals(id, updated.id());
        assertEquals(Language.Polish, updated.language());
        assertEquals("ug", updated.upgradeGui());
    }

    @Test
    void updatePlayerShopGuiForUnknownPlayerIsNoOp() {
        PlayerManager pm = new PlayerManager();
        assertDoesNotThrow(() -> pm.updatePlayerShopGui("nobody", "sg"));
        assertNull(pm.getPlayer("nobody"));
    }

    // ── updatePlayerUpgradeGui ───────────────────────────────────────────────

    @Test
    void updatePlayerUpgradeGuiUpdatesExistingEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerIns(UUID.randomUUID(), "Alice", Language.English, "sg", "oldUpgrade"));
        pm.updatePlayerUpgradeGui("Alice", "newUpgrade");
        assertEquals("newUpgrade", pm.getPlayer("Alice").upgradeGui());
    }

    @Test
    void updatePlayerUpgradeGuiForUnknownPlayerIsNoOp() {
        PlayerManager pm = new PlayerManager();
        assertDoesNotThrow(() -> pm.updatePlayerUpgradeGui("nobody", "ug"));
        assertNull(pm.getPlayer("nobody"));
    }

    // ── updatePlayerLanguage ─────────────────────────────────────────────────

    @Test
    void updatePlayerLanguageUpdatesExistingEntry() {
        PlayerManager pm = new PlayerManager();
        pm.addPlayer(new PlayerIns(UUID.randomUUID(), "Alice", Language.English, "sg", "ug"));
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
