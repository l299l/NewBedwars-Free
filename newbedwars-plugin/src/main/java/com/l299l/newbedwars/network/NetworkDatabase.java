package com.l299l.newbedwars.network;

import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.sql.ServerStatusRepo;
import com.l299l.newbedwars.config.data.sql.Database;

import java.util.logging.Logger;

public final class NetworkDatabase {
    private final Logger log;
    private final Database db;
    private boolean tableReady;

    public NetworkDatabase(Logger log) {
        this.log = log;
        this.db = Database.get(log);
    }

    public void publish(ServerState state) {
        db.submitLatest("server-status", "Writing the server status", c -> {
            if (!tableReady) {
                ServerStatusRepo.createTable(c);
                tableReady = true;
                log.info("[Network] Connected to MySQL, writing the server status to " + ServerStatusRepo.TABLE);
            }
            ServerStatusRepo.upsert(c, state, System.currentTimeMillis());
        });
    }

    public void close(ServerState last) {
        if (last != null) publish(last);
    }
}
