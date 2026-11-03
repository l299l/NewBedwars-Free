package com.l299l.newbedwars.common.protocol.payload;

import java.util.UUID;

public class RejoinSlot {
    public UUID player;
    public long expiresAt;

    public RejoinSlot() {}

    public RejoinSlot(UUID player, long expiresAt) {
        this.player = player;
        this.expiresAt = expiresAt;
    }
}
