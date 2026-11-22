package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerDataSqlTest {

    private PlayerDataSql create(PlayerManager pm) {
        Properties.MySQLHost = "localhost";
        Properties.MySQLPort = 3306;
        Properties.MySQLDatabase = "test";
        Properties.MySQLUser = "test";
        Properties.MySQLPassword = "test";
        NewBedwars plugin = mock(NewBedwars.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getPlayerManager()).thenReturn(pm);
        return new PlayerDataSql(plugin);
    }

    @Test
    void playerIsNotLoadedUntilApplied() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerDataSql sql = create(pm);
        UUID id = UUID.randomUUID();
        assertFalse(sql.isLoaded(id));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(mock(Player.class));
            sql.apply("Leo", id, null, null);
        }
        assertTrue(sql.isLoaded(id));
        verify(pm).identify(id, "Leo");
    }

    @Test
    void saveIsSkippedBeforeTheLoadSucceeded() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerDataSql sql = create(pm);
        when(pm.idOf("Leo")).thenReturn(UUID.randomUUID());
        sql.savePlayer("Leo");
        verify(pm, never()).getPlayer(any(UUID.class));
    }

    @Test
    void saveReadsTheProfileOnceLoaded() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Leo", null, PlayerProfile.defaultFastBuy());
        when(pm.idOf("Leo")).thenReturn(profile.id());
        when(pm.getPlayer(profile.id())).thenReturn(profile);
        PlayerDataSql sql = create(pm);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(profile.id())).thenReturn(mock(Player.class));
            sql.apply("Leo", profile.id(), null, PlayerStats.empty());
        }
        clearInvocations(pm);
        sql.savePlayer("Leo");
        verify(pm).getPlayer(profile.id());
    }

    @Test
    void applyForAPlayerWhoAlreadyLeftDoesNothing() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerDataSql sql = create(pm);
        UUID id = UUID.randomUUID();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(null);
            sql.apply("Leo", id, null, PlayerStats.empty());
            sql.applyStats(id, "Leo", PlayerStats.empty());
        }
        assertFalse(sql.isLoaded(id));
        verify(pm, never()).identify(any(), any());
        verify(pm, never()).addStats(any(), any(), any());
    }

    @Test
    void refreshedStatsAreStoredForOnlinePlayers() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerDataSql sql = create(pm);
        UUID id = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(1, 0, 0, 0, 0, 0, 1);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(mock(Player.class));
            sql.applyStats(id, "Leo", stats);
        }
        verify(pm).addStats(id, "Leo", stats);
    }

    @Test
    void gameResultOfAnOfflinePlayerIsSkippedOnlyWhileSqlIsActive() {
        PlayerManager pm = new PlayerManager();
        UUID id = UUID.randomUUID();
        com.l299l.newbedwars.arena.player.GamePlayer gp = mock(com.l299l.newbedwars.arena.player.GamePlayer.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(null);
            pm.setDropOfflineGameResults(true);
            pm.addGameResult(id, "Leo", gp, true);
            assertTrue(pm.getAllStats().isEmpty());
            assertNull(pm.idOf("Leo"));
            pm.setDropOfflineGameResults(false);
            pm.addGameResult(id, "Leo", gp, true);
            assertEquals(1, pm.getStats(id).wins());
        }
    }
}
