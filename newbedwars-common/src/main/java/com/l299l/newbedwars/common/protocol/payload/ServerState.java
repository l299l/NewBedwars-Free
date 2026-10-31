package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;

/** The arenas of one game server. */
public class ServerState {
    public String serverId;
    public String mode;
    /** The server is about to restart and takes no new players. */
    public boolean restarting;
    /** Sender clock, epoch millis. */
    public long time;
    public List<ArenaInfo> arenas = new ArrayList<>();

    public ServerState() {}

    public ServerState(String serverId, String mode, boolean restarting, long time, List<ArenaInfo> arenas) {
        this.serverId = serverId;
        this.mode = mode;
        this.restarting = restarting;
        this.time = time;
        this.arenas = arenas == null ? new ArrayList<>() : arenas;
    }
}
