package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.config.Language;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlayerSqlRepoTest {

    @Test
    void addStatsIsAnIncrement() throws Exception {
        assertTrue(PlayerSqlRepo.ADD_STATS.contains("wins = wins + VALUES(wins)"));
        assertTrue(PlayerSqlRepo.ADD_STATS.contains("games_played = games_played + VALUES(games_played)"));
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(PlayerSqlRepo.ADD_STATS)).thenReturn(ps);
        PlayerStats delta = PlayerSqlRepo.gameDelta(3, 1, 2, 1, true);
        assertEquals(new PlayerStats(1, 0, 3, 1, 2, 1, 1), delta);
        PlayerSqlRepo.addStats(c, "Leo", delta);
        verify(ps).setString(1, "Leo");
        verify(ps).setInt(2, 1);
        verify(ps).setInt(4, 3);
        verify(ps).setInt(8, 1);
        verify(ps).executeUpdate();
    }

    @Test
    void createTablesAndEmptyCheck() throws Exception {
        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.createStatement()).thenReturn(st);
        when(st.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0, 0);
        PlayerSqlRepo.createTables(c);
        verify(st).executeUpdate(PlayerSqlRepo.CREATE_PLAYERS);
        verify(st).executeUpdate(PlayerSqlRepo.CREATE_STATS);
        assertTrue(PlayerSqlRepo.isEmpty(c));
    }

    @Test
    void loadPlayerAndStats() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("name")).thenReturn("Leo");
        when(rs.getString("uuid")).thenReturn("u");
        when(rs.getString("language")).thenReturn("Polish");
        when(rs.getString("fast_buy")).thenReturn(null);
        PlayerSqlRepo.PlayerRow row = PlayerSqlRepo.loadPlayer(c, "Leo");
        assertEquals("Polish", row.language());
        assertNull(PlayerSqlRepo.loadPlayer(c, "Nobody"));
    }

    @Test
    void topOnlyAcceptsKnownColumns() throws Exception {
        Connection c = mock(Connection.class);
        assertThrows(IllegalArgumentException.class, () -> PlayerSqlRepo.top(c, "wins; DROP TABLE x", 10));
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement("SELECT * FROM nbw_stats ORDER BY final_kills DESC, name ASC LIMIT ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("name")).thenReturn("Leo");
        when(rs.getInt("final_kills")).thenReturn(9);
        List<Map.Entry<String, PlayerStats>> top = PlayerSqlRepo.top(c, "final_kills", 10);
        assertEquals("Leo", top.get(0).getKey());
        assertEquals(9, top.get(0).getValue().finalKills());
        verify(ps).setInt(1, 10);
    }

    @Test
    void profileRowRoundTrip() {
        UUID id = UUID.randomUUID();
        PlayerProfile p = new PlayerProfile(id, "Leo", Language.Polish, Map.of("blocks", List.of("WOOL")));
        PlayerSqlRepo.PlayerRow row = PlayerDataSql.row(p);
        assertEquals(id.toString(), row.uuid());
        assertEquals(Language.Polish, PlayerDataSql.parseLanguage(row.language()));
        assertEquals(Map.of("blocks", List.of("WOOL")), PlayerDataSql.parseFastBuy(row.fastBuyJson()));
        assertNull(PlayerDataSql.parseLanguage("Klingon"));
        assertNull(PlayerDataSql.parseFastBuy("{broken"));
    }

    @Test
    void sharedLeaderboardsReplaceLocalOnes() {
        PlayerManager pm = new PlayerManager();
        pm.addStats("Local", new PlayerStats(50, 0, 0, 0, 0, 0, 50));
        assertEquals("Local", pm.getTopByWins(10).get(0).getKey());
        pm.setSharedLeaderboards(Map.of("wins", List.of(Map.entry("Net1", new PlayerStats(99, 0, 0, 0, 0, 0, 99)),
                Map.entry("Net2", new PlayerStats(80, 0, 0, 0, 0, 0, 80)))));
        assertEquals(List.of("Net1"), pm.getTopByWins(1).stream().map(Map.Entry::getKey).toList());
        assertEquals("Local", pm.getTopByKills(10).get(0).getKey(), "boards the database did not send stay local");
    }
}
