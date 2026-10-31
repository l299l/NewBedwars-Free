package com.l299l.newbedwars.common.protocol.payload;

import java.util.UUID;

/** A lobby server asks the proxy to send a player (and, if they are its admin, their party) into a game. */
public class JoinRequest {
    public UUID player;
    public TargetKind kind;
    /** Group or arena name; null for {@link TargetKind#ANY}. */
    public String target;
    /** Only this game server (e.g. the winner of a map vote); null = any. */
    public String server;
    /**
     * The players to move together, chosen by a game server (a map vote moves a party or a single player). When
     * set, the proxy moves exactly these (no party-admin rule); when empty, the requester and their party.
     */
    public java.util.List<UUID> players = new java.util.ArrayList<>();

    public JoinRequest() {}

    public JoinRequest(UUID player, TargetKind kind, String target) {
        this.player = player;
        this.kind = kind;
        this.target = target;
    }

    public JoinRequest(UUID player, TargetKind kind, String target, String server, java.util.List<UUID> players) {
        this(player, kind, target);
        this.server = server;
        this.players = players == null ? new java.util.ArrayList<>() : players;
    }
}
