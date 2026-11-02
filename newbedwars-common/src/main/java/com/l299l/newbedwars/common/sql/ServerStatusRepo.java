package com.l299l.newbedwars.common.sql;

import com.l299l.newbedwars.common.protocol.Limits;
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
    static final String SELECT_ALL = "SELECT server_id, state_json, last_heartbeat FROM " + TABLE
            + " WHERE LENGTH(state_json) < " + Limits.MAX_STATE_JSON_LENGTH
            + " ORDER BY last_heartbeat DESC LIMIT " + Limits.MAX_DB_ROWS;

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
     * The newest rows (at most {@link Limits#MAX_DB_ROWS}, state text under {@link Limits#MAX_STATE_JSON_LENGTH}).
     * The heartbeat is the writer's clock, so freshness is not decided here: the reader only compares it with the
     * previous heartbeat of the same server. The returned state's {@code time} is the heartbeat; rows with broken
     * JSON are skipped.
     */
    public static List<ServerState> readAll(Connection c) throws SQLException {
        List<ServerState> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_ALL)) {
            ps.setMaxRows(Limits.MAX_DB_ROWS);
            try (ResultSet rs = ps.executeQuery()) {
                while (out.size() < Limits.MAX_DB_ROWS && rs.next()) {
                    String id = rs.getString("server_id");
                    if (id == null || id.length() > Limits.MAX_NAME_LENGTH) continue;
                    String json = rs.getString("state_json");
                    if (json == null || json.length() >= Limits.MAX_STATE_JSON_LENGTH) continue;
                    ServerState s = NetCodec.payloadFromJson(json, ServerState.class);
                    if (s == null) continue;
                    s.serverId = id;
                    s.time = rs.getLong("last_heartbeat");
                    if (s.arenas == null) s.arenas = new ArrayList<>();
                    out.add(s);
                }
            }
        }
        return out;
    }
}
