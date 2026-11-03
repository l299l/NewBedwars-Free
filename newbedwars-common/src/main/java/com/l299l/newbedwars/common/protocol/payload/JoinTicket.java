package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JoinTicket {
    public String requestId;
    public UUID player;
    public TargetKind kind;
    public String target;
    public String arena;
    public String partyId;
    public List<UUID> party = new ArrayList<>();
    public long expiresAt;
    public long ttlMillis;

    public JoinTicket() {}

    public JoinTicket(String requestId, UUID player, TargetKind kind, String target, String arena,
                      String partyId, List<UUID> party, long expiresAt) {
        this.requestId = requestId;
        this.player = player;
        this.kind = kind;
        this.target = target;
        this.arena = arena;
        this.partyId = partyId;
        this.party = party == null ? new ArrayList<>() : party;
        this.expiresAt = expiresAt;
    }

    public JoinTicket(String requestId, UUID player, TargetKind kind, String target, String arena,
                      String partyId, List<UUID> party, long expiresAt, long ttlMillis) {
        this(requestId, player, kind, target, arena, partyId, party, expiresAt);
        this.ttlMillis = ttlMillis;
    }
}
