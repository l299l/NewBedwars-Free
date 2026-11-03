package com.l299l.newbedwars.proxy.core.group;

import com.l299l.newbedwars.common.protocol.Limits;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.StateFit;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.proxy.core.ProxyPlatform;
import com.l299l.newbedwars.proxy.core.party.PartyService;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class GroupRelay {
    static final long MAX_FUTURE_MILLIS = 24L * 60 * 60 * 1000;

    private final ProxyPlatform platform;
    private NetworkGroups current;

    public GroupRelay(ProxyPlatform platform) {
        this.platform = platform;
    }

    public NetworkGroups current() {
        return current;
    }

    public boolean onGroups(String from, NetworkGroups groups, Set<String> lobbies, Set<String> games, long now) {
        if (groups == null || (current != null && groups.revision <= current.revision)) return false;
        if (groups.revision > now + MAX_FUTURE_MILLIS) return false;
        if (StateFit.size(groups) > Limits.MAX_PAYLOAD_BYTES) return false;
        current = groups;
        Set<String> targets = new LinkedHashSet<>(lobbies);
        targets.addAll(games);
        targets.remove(from);
        for (String server : targets) platform.sendToServer(server, message());
        return true;
    }

    public void onHello(UUID via) {
        if (current != null) platform.sendToPlayerServer(via, message());
    }

    private NetMessage message() {
        return NetMessage.of(PartyService.FROM, current);
    }
}
