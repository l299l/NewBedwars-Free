package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A game server asks the proxy to move players to a lobby server. */
public class SendToLobby {
    public List<UUID> players = new ArrayList<>();

    public SendToLobby() {}

    public SendToLobby(List<UUID> players) {
        this.players = players == null ? new ArrayList<>() : players;
    }
}
