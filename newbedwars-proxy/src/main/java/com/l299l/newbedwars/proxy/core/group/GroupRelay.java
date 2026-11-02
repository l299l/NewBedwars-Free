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

/**
 * Arena groups are edited on lobby servers. The relay keeps the newest set (by revision) and hands it to every
 * backend: right away to servers with players, and to any server when a player arrives there (HELLO).
 * Not thread-safe.
 */
public final class GroupRelay {
    /** A revision this far ahead of the proxy clock is not a real edit time. */
    static final long MAX_FUTURE_MILLIS = 24L * 60 * 60 * 1000;

    private final ProxyPlatform platform;
    private NetworkGroups current;

    public GroupRelay(ProxyPlatform platform) {
        this.platform = platform;
    }

    /** The newest groups seen, or null. */
    public NetworkGroups current() {
        return current;
    }

    /** Groups sent by lobby {@code from}; older or equal revisions, revisions more than a day ahead of
     * {@code now} and sets too big for one message are ignored. @return whether they were taken */
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

    /** A player arrived on a backend: give that server the newest groups (it ignores ones it already has). */
    public void onHello(UUID via) {
        if (current != null) platform.sendToPlayerServer(via, message());
    }

    private NetMessage message() {
        return NetMessage.of(PartyService.FROM, current);
    }
}
