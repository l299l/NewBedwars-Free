package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;

public class ServerState {
    public String serverId;
    public String mode;

    public boolean restarting;

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
