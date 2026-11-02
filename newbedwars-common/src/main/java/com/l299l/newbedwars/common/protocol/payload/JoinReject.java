package com.l299l.newbedwars.common.protocol.payload;

import java.util.UUID;

/** A game server could not put a ticketed player into an arena. */
public class JoinReject {
    /** Why the ticket could not be honoured. */
    public enum Reason { FULL, NOT_JOINABLE, NO_ARENA, PARTY_NOT_ALLOWED, EXPIRED, OTHER }

    public String requestId;
    public UUID player;
    public String arena;
    public Reason reason;

    public JoinReject() {}

    public JoinReject(String requestId, UUID player, String arena, Reason reason) {
        this.requestId = requestId;
        this.player = player;
        this.arena = arena;
        this.reason = reason;
    }
}
