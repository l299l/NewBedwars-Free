package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.player.PlayerIns;
import com.l299l.newbedwars.player.PlayerManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GamePlayerTest {

    private Player player;
    private Team team;
    private PlayerManager mockPm;

    @BeforeEach
    void setUp() {
        NewBedwars mockPlugin = mock(NewBedwars.class);
        NewBedwars.plugin = mockPlugin;

        mockPm = mock(PlayerManager.class);
        GuiManager mockGm = mock(GuiManager.class);
        CustomItemManager mockCim = mock(CustomItemManager.class);

        when(mockPlugin.getPlayerManager()).thenReturn(mockPm);
        when(mockPlugin.getGuiManager()).thenReturn(mockGm);
        when(mockPlugin.getCustomItemManager()).thenReturn(mockCim);

        PlayerIns ins = new PlayerIns(UUID.randomUUID(), "Alice", Language.English, "shopGui1", "upgradeGui1");
        when(mockPm.getPlayer("Alice")).thenReturn(ins);
        when(mockGm.getGui(any())).thenReturn(null);

        player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");

        team = mock(Team.class);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private GamePlayer createGamePlayer() {
        return new GamePlayer(player, team);
    }

    // ── constructor / getters ─────────────────────────────────────────────────

    @Test
    void constructorSetsPlayerAndTeam() {
        GamePlayer gp = createGamePlayer();
        assertSame(player, gp.getPlayer());
        assertSame(team, gp.getTeam());
    }

    @Test
    void constructorFallsBackToDefaultGuiWhenPlayerNotRegisteredInManager() {
        when(mockPm.getPlayer("Alice")).thenReturn(null);
        // Falls back to Properties.DefaultTeamShopGui / DefaultUpgradeShopGui — no NPE
        assertDoesNotThrow(this::createGamePlayer);
    }

    // ── kills ─────────────────────────────────────────────────────────────────

    @Test
    void killsStartAtZero() {
        assertEquals(0, createGamePlayer().getKills());
    }

    @Test
    void addKillIncrementsKills() {
        GamePlayer gp = createGamePlayer();
        gp.addKill();
        gp.addKill();
        assertEquals(2, gp.getKills());
    }

    // ── final kills ───────────────────────────────────────────────────────────

    @Test
    void finalKillsStartAtZero() {
        assertEquals(0, createGamePlayer().getFinalKills());
    }

    @Test
    void addFinalKillIncrementsFinalKills() {
        GamePlayer gp = createGamePlayer();
        gp.addFinalKill();
        assertEquals(1, gp.getFinalKills());
    }

    // ── beds broken ───────────────────────────────────────────────────────────

    @Test
    void bedsBrokenStartAtZero() {
        assertEquals(0, createGamePlayer().getBedsBroken());
    }

    @Test
    void addBedBrokenIncrementsBedsBroken() {
        GamePlayer gp = createGamePlayer();
        gp.addBedBroken();
        gp.addBedBroken();
        gp.addBedBroken();
        assertEquals(3, gp.getBedsBroken());
    }

    // ── stats independence ────────────────────────────────────────────────────

    @Test
    void killFinalKillAndBedCountsAreIndependent() {
        GamePlayer gp = createGamePlayer();
        gp.addKill();
        gp.addFinalKill();
        gp.addFinalKill();
        gp.addBedBroken();
        assertEquals(1, gp.getKills());
        assertEquals(2, gp.getFinalKills());
        assertEquals(1, gp.getBedsBroken());
    }
}
