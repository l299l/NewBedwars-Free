package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;

/** All known game servers, sent by the proxy to lobby servers. */
public class NetworkState {
    public List<ServerState> servers = new ArrayList<>();

    public NetworkState() {}

    public NetworkState(List<ServerState> servers) {
        this.servers = servers == null ? new ArrayList<>() : servers;
    }
}
