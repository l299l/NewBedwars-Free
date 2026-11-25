package com.l299l.newbedwars.config.data.sql;

import com.l299l.newbedwars.player.PlayerStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PendingStatsSpillTest {
    @TempDir
    Path tmp;

    private PendingStatsSpill spill() {
        return new PendingStatsSpill(tmp.resolve("data").resolve("pending-stats.jsonl"), Logger.getAnonymousLogger());
    }

    @Test
    void appendedDeltasAreReplayedAndTheFileRemoved() throws Exception {
        PendingStatsSpill spill = spill();
        assertFalse(spill.hasPending());
        assertTrue(spill.append(UUID.randomUUID(), "Leo \"the\" Bed", new PlayerStats(1, 0, 3, 2, 1, 1, 1)));
        assertTrue(spill.append(UUID.randomUUID(), "Mia", PlayerStats.empty()));
        assertTrue(spill.hasPending());
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        assertEquals(2, spill.replay(c));
        verify(ps, times(2)).executeUpdate();
        assertFalse(spill.hasPending());
    }

    @Test
    void failedReplayKeepsOnlyTheUnwrittenEntries() throws Exception {
        PendingStatsSpill spill = spill();
        spill.append(UUID.randomUUID(), "A", PlayerStats.empty());
        spill.append(UUID.randomUUID(), "B", PlayerStats.empty());
        spill.append(UUID.randomUUID(), "C", PlayerStats.empty());
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1).thenThrow(new SQLException("down"));
        assertThrows(SQLException.class, () -> spill.replay(c));
        Path file = tmp.resolve("data").resolve("pending-stats.jsonl");
        var lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"name\":\"B\""));
        assertTrue(lines.get(1).contains("\"name\":\"C\""));
    }
}
