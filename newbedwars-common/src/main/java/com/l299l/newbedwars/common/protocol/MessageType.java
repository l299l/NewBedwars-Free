package com.l299l.newbedwars.common.protocol;

import com.l299l.newbedwars.common.protocol.payload.*;

public enum MessageType {
    HELLO(Hello.class),
    SERVER_STATE(ServerState.class),
    NETWORK_STATE(NetworkState.class),
    JOIN_REQUEST(JoinRequest.class),
    JOIN_TICKET(JoinTicket.class),
    JOIN_REJECT(JoinReject.class),
    PARTY_SYNC(PartySync.class),
    SEND_TO_LOBBY(SendToLobby.class),
    GROUPS(NetworkGroups.class);

    private final Class<?> payloadClass;

    MessageType(Class<?> payloadClass) {
        this.payloadClass = payloadClass;
    }

    public Class<?> payloadClass() {
        return payloadClass;
    }
}
