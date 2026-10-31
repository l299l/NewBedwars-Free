package com.l299l.newbedwars.common.sql;

import com.l299l.newbedwars.common.protocol.NetCodec;
import com.l299l.newbedwars.common.protocol.payload.ServerState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared status table {@code nbw_servers}: game servers write their {@link ServerState}, the proxy reads it.
 * Plain JDBC (MySQL/MariaDB syntax); the caller owns the connection and the thread.
 */
public final class ServerStatusRepo {
    public static final String TABLE = "nbw_servers";

    static final String CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
            + "server_id VARCHAR(64) NOT NULL PRIMARY KEY, "
            + "mode VARCHAR(16) NOT NULL, "
            + "state_json MEDIUMTEXT NOT NULL, "
            + "last_heartbeat BIGINT NOT NULL)";
    static final String UPSERT = "INSERT INTO " + TABLE + " (server_id, mode, state_json, last_heartbeat) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE mode = VALUES(mode), state_json = VALUES(state_json), last_heartbeat = VALUES(last_heartbeat)";
    static final String SELECT_FRESH = "SELECT server_id, state_json, last_heartbeat FROM " + TABLE + " WHERE last_heartbeat >= ?";

    private ServerStatusRepo() {}

    public static void createTable(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(CREATE);
        }
    }

    /** Stores {@code state} with {@code now} as heartbeat. */
    public static void upsert(Connection c, ServerState state, long now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPSERT)) {
            ps.setString(1, state.serverId);
            ps.setString(2, state.mode == null ? "GAME" : state.mode);
            ps.setString(3, NetCodec.payloadToJson(state));
            ps.setLong(4, now);
            ps.executeUpdate();
        }
    }

    /**
     * States whose heartbeat is not older than {@code staleBefore} (epoch millis). The returned state's
     * {@code time} is the heartbeat. Rows with broken JSON are skipped.
     */
    public static List<ServerState> readFresh(Connection c, long staleBefore) throws SQLException {
        List<ServerState> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_FRESH)) {
            ps.setLong(1, staleBefore);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ServerState s = NetCodec.payloadFromJson(rs.getString("state_json"), ServerState.class);
                    if (s == null) continue;
                    s.serverId = rs.getString("server_id");
                    s.time = rs.getLong("last_heartbeat");
                    if (s.arenas == null) s.arenas = new ArrayList<>();
                    out.add(s);
                }
            }
        }
        return out;
    }
}
