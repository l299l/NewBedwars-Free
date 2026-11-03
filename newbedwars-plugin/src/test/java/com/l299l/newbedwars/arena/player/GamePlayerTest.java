package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.shops.customitems.CustomItemManager;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.GUIManager;
import com.l299l.newbedwars.player.PlayerManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        GUIManager mockGm = mock(GUIManager.class);
        CustomItemManager mockCim = mock(CustomItemManager.class);

        when(mockPlugin.getPlayerManager()).thenReturn(mockPm);
        when(mockPlugin.getGuiManager()).thenReturn(mockGm);
        when(mockPlugin.getCustomItemManager()).thenReturn(mockCim);

        when(mockGm.getGui(any())).thenReturn(null);

        player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        when(player.getWorld()).thenReturn(mock(World.class));

        team = mock(Team.class);
    }

    @AfterEach
    void tearDown() {
        NewBedwars.plugin = null;
    }

    private GamePlayer createGamePlayer() {
        return new GamePlayer(player, team);
    }

    @Test
    void constructorSetsPlayerAndTeam() {
        GamePlayer gp = createGamePlayer();
        assertSame(player, gp.getPlayer());
        assertSame(team, gp.getTeam());
    }

    @Test
    void constructorFallsBackToDefaultGuiWhenPlayerNotRegisteredInManager() {
        when(mockPm.getPlayer("Alice")).thenReturn(null);

        assertDoesNotThrow(this::createGamePlayer);
    }

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
