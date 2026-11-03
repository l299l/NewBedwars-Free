package com.l299l.newbedwars.common.protocol.payload;

import java.util.UUID;

public class JoinRequest {
    public UUID player;
    public TargetKind kind;
    public String target;
    public String server;
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
