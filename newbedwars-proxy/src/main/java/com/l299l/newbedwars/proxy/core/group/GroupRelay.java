package com.l299l.newbedwars.proxy.core.group;

import com.l299l.newbedwars.common.protocol.NetMessage;
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
    private final ProxyPlatform platform;
    private NetworkGroups current;

    public GroupRelay(ProxyPlatform platform) {
        this.platform = platform;
    }

    /** The newest groups seen, or null. */
    public NetworkGroups current() {
        return current;
    }

    /** Groups sent by lobby {@code from}; older or equal revisions are ignored. @return whether they were taken */
    public boolean onGroups(String from, NetworkGroups groups, Set<String> lobbies, Set<String> games) {
        if (groups == null || (current != null && groups.revision <= current.revision)) return false;
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
