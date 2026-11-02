package com.l299l.newbedwars.common.sql;

import com.l299l.newbedwars.common.protocol.Limits;
import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerStatusRepoTest {

    @Test
    void createTableAndUpsert() throws Exception {
        Connection c = mock(Connection.class);
        Statement st = mock(Statement.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(c.createStatement()).thenReturn(st);
        when(c.prepareStatement(ServerStatusRepo.UPSERT)).thenReturn(ps);

        ServerStatusRepo.createTable(c);
        verify(st).executeUpdate(ServerStatusRepo.CREATE);

        ServerState s = new ServerState("game1", "GAME", false, 0,
                List.of(new ArenaInfo("a", null, ArenaInfo.State.WAITING, 1, 8, 1, 8, true)));
        ServerStatusRepo.upsert(c, s, 1234L);
        verify(ps).setString(1, "game1");
        verify(ps).setString(2, "GAME");
        verify(ps).setString(3, NetCodec.payloadToJson(s));
        verify(ps).setLong(4, 1234L);
        verify(ps).executeUpdate();
        verify(ps).close();
    }

    @Test
    void readAllUsesHeartbeatAsTimeAndSkipsBrokenRows() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(ServerStatusRepo.SELECT_ALL)).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        String good = NetCodec.payloadToJson(new ServerState("ignored", "GAME", true, 1, null));
        when(rs.getString("state_json")).thenReturn(good, "{broken");
        when(rs.getString("server_id")).thenReturn("game2");
        when(rs.getLong("last_heartbeat")).thenReturn(5000L);

        List<ServerState> out = ServerStatusRepo.readAll(c);
        verify(ps, never()).setLong(anyInt(), anyLong());
        assertEquals(1, out.size());
        assertEquals("game2", out.get(0).serverId);
        assertEquals(5000L, out.get(0).time);
        assertTrue(out.get(0).restarting);
        assertNotNull(out.get(0).arenas);
    }

    @Test
    void queryIsBoundedAndHasNoClockCutoff() {
        assertFalse(ServerStatusRepo.SELECT_ALL.contains("last_heartbeat >="));
        assertTrue(ServerStatusRepo.SELECT_ALL.contains("LENGTH(state_json) < " + Limits.MAX_STATE_JSON_LENGTH));
        assertTrue(ServerStatusRepo.SELECT_ALL.contains("LIMIT " + Limits.MAX_DB_ROWS));
    }

    @Test
    void readAllStopsAtTheRowLimitAndSkipsOversizedOrNamelessRows() throws Exception {
        Connection c = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(c.prepareStatement(ServerStatusRepo.SELECT_ALL)).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        String good = NetCodec.payloadToJson(new ServerState("x", "GAME", false, 1, null));
        String huge = "{\"mode\":\"" + "x".repeat(Limits.MAX_STATE_JSON_LENGTH) + "\"}";
        when(rs.getString("state_json")).thenReturn(huge, good);
        when(rs.getString("server_id")).thenReturn("s".repeat(Limits.MAX_NAME_LENGTH + 1), null, "game1");

        List<ServerState> out = ServerStatusRepo.readAll(c);
        assertEquals(Limits.MAX_DB_ROWS, out.size());
        verify(ps).setMaxRows(Limits.MAX_DB_ROWS);
    }
}
