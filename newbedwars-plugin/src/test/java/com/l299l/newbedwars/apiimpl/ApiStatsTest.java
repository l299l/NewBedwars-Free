package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.view.LeaderboardEntry;
import com.l299l.newbedwars.api.view.StatType;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class ApiStatsTest {
    private final PlayerManager players = new PlayerManager();

    private ApiStats json() {
        return new ApiStats(() -> players, () -> false, task -> fail("no SQL in JSON mode"), Runnable::run);
    }

    @Test
    void jsonLeaderboardSortsByStatThenName() throws Exception {
        players.addStats("bob", new PlayerStats(5, 1, 10, 2, 3, 1, 6));
        players.addStats("Alice", new PlayerStats(5, 0, 20, 1, 1, 2, 5));
        players.addStats("carl", new PlayerStats(1, 4, 30, 9, 0, 0, 5));
        List<LeaderboardEntry> wins = json().leaderboard(StatType.WINS, 10).get();
        assertEquals(List.of("Alice", "bob", "carl"), wins.stream().map(LeaderboardEntry::getName).toList());
        List<LeaderboardEntry> kills = json().leaderboard(StatType.KILLS, 2).get();
        assertEquals(List.of("carl", "Alice"), kills.stream().map(LeaderboardEntry::getName).toList());
        assertEquals(30, kills.get(0).getStats().getKills());
        assertEquals(3, json().leaderboard(StatType.DEATHS, 0).get().size() + 2);
        assertTrue(json().leaderboard(null, 5).isCompletedExceptionally());
    }

    @Test
    void jsonLoadIsCaseInsensitiveAndEmptyForUnknown() throws Exception {
        players.addStats("Alice", new PlayerStats(5, 0, 20, 1, 1, 2, 5));
        assertEquals(20, json().load("alice").get().orElseThrow().getKills());
        assertTrue(json().load("ghost").get().isEmpty());
        assertTrue(json().load("  ").get().isEmpty());
    }

    @Test
    void jsonFindByPrefix() throws Exception {
        players.addStats("Alice", PlayerStats.empty());
        players.addStats("alex", PlayerStats.empty());
        players.addStats("Bob", PlayerStats.empty());
        assertEquals(List.of("alex", "Alice"), json().find("AL", 10).get());
        assertEquals(1, json().find("a", 1).get().size());
    }

    @Test
    void statColumnsAreWhitelisted() {
        for (StatType stat : StatType.values()) {
            assertTrue(com.l299l.newbedwars.config.data.sql.PlayerSqlRepo.STAT_COLUMNS.contains(ApiStats.column(stat)), stat.name());
        }
    }

    private ApiStats sql(Connection c) {
        return new ApiStats(() -> players, () -> true, task -> {
            try {
                task.run(c);
            } catch (SQLException ignored) {
            }
        }, r -> fail("no main thread in SQL mode"));
    }

    @Test
    void sqlLeaderboardAndLoad() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("name")).thenReturn("Net1");
        when(rs.getInt("beds_broken")).thenReturn(7);
        List<LeaderboardEntry> top = sql(c).leaderboard(StatType.BEDS_BROKEN, 500).get(1, TimeUnit.SECONDS);
        assertEquals("Net1", top.get(0).getName());
        assertEquals(7, top.get(0).getStats().getBedsBroken());
        verify(c).prepareStatement(contains("ORDER BY beds_broken DESC"));
        verify(ps).setInt(1, ApiStats.MAX_LIMIT);

        when(rs.next()).thenReturn(false);
        assertTrue(sql(c).load("ghost").get(1, TimeUnit.SECONDS).isEmpty());
    }

    @Test
    void sqlErrorFailsTheFuture() throws Exception {
        Connection c = mock(Connection.class);
        when(c.prepareStatement(anyString())).thenThrow(new SQLException("down"));
        ExecutionException e = assertThrows(ExecutionException.class, () -> sql(c).find("a", 5).get(1, TimeUnit.SECONDS));
        assertInstanceOf(SQLException.class, e.getCause());
    }
}
