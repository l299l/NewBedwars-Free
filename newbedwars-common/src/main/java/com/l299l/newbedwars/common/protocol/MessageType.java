package com.l299l.newbedwars.common.protocol;

import com.l299l.newbedwars.common.protocol.payload.*;

/** Every message of the network protocol and the class of its payload. */
public enum MessageType {
    /** backend -> proxy: a server announces itself (sent on the first player join and after reloads). */
    HELLO(Hello.class),
    /** game server -> proxy: the full list of its arenas. */
    SERVER_STATE(ServerState.class),
    /** proxy -> lobby server: the states of all game servers. */
    NETWORK_STATE(NetworkState.class),
    /** lobby server -> proxy: a player (party admin) wants to join a game. */
    JOIN_REQUEST(JoinRequest.class),
    /** proxy -> game server: a player that is connecting should be put into an arena. */
    JOIN_TICKET(JoinTicket.class),
    /** game server -> proxy: a ticket could not be honoured, route the player elsewhere. */
    JOIN_REJECT(JoinReject.class),
    /** proxy -> backend: the party of players on that server (proxy owns parties). */
    PARTY_SYNC(PartySync.class),
    /** backend -> proxy: send these players to a lobby server. */
    SEND_TO_LOBBY(SendToLobby.class),
    /** lobby server -> proxy -> every backend: the network's arena groups (newest revision wins). */
    GROUPS(NetworkGroups.class);

    private final Class<?> payloadClass;

    MessageType(Class<?> payloadClass) {
        this.payloadClass = payloadClass;
    }

    public Class<?> payloadClass() {
        return payloadClass;
    }
}
