package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;

public class NetworkState {
    public List<ServerState> servers = new ArrayList<>();

    public NetworkState() {}

    public NetworkState(List<ServerState> servers) {
        this.servers = servers == null ? new ArrayList<>() : servers;
    }
}
