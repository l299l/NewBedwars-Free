package com.l299l.newbedwars.common.protocol.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tells a game server what to do with a player who is connecting to it. When {@link #arena} is null
 * (the proxy had no fresh state of the server) the game server resolves {@link #kind}/{@link #target} itself.
 */
public class JoinTicket {
    /** Id of the proxy's join request; echoed in a {@link JoinReject}. */
    public String requestId;
    public UUID player;
    public TargetKind kind;
    public String target;
    /** The arena the proxy picked, or null. */
    public String arena;
    /** Party id, or null when the player joins alone. */
    public String partyId;
    /** All party members that were sent together (including {@link #player}). */
    public List<UUID> party = new ArrayList<>();
    /** Epoch millis after which the ticket is void. */
    public long expiresAt;

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
}
