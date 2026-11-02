package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.Test;

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
        assertFalse(sql.isLoaded("Leo"));
        sql.apply("Leo", UUID.randomUUID(), null, null);
        assertTrue(sql.isLoaded("Leo"));
    }

    @Test
    void saveIsSkippedBeforeTheLoadSucceeded() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerDataSql sql = create(pm);
        sql.savePlayer("Leo");
        verify(pm, never()).getPlayer("Leo");
    }

    @Test
    void saveReadsTheProfileOnceLoaded() {
        PlayerManager pm = mock(PlayerManager.class);
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Leo", null, PlayerProfile.defaultFastBuy());
        when(pm.getPlayer("Leo")).thenReturn(profile);
        PlayerDataSql sql = create(pm);
        sql.apply("Leo", profile.id(), null, PlayerStats.empty());
        clearInvocations(pm);
        sql.savePlayer("Leo");
        verify(pm).getPlayer("Leo");
    }
}
