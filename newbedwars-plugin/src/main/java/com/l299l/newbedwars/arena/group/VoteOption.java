package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.network.RemoteArena;

import java.util.Locale;

/**
 * A map the players of an ended game can vote for: an arena on this server, or (network mode) an arena of the
 * same group on another game server.
 */
public final class VoteOption {
    private final IArena local;
    private final RemoteArena remote;

    private VoteOption(IArena local, RemoteArena remote) {
        this.local = local;
        this.remote = remote;
    }

    public static VoteOption local(IArena arena) {
        return new VoteOption(arena, null);
    }

    public static VoteOption remote(RemoteArena arena) {
        return new VoteOption(null, arena);
    }

    /** The key votes are counted under: the arena name here, {@code server/arena} for another server. */
    public String key() {
        return local != null ? local.getArenaName() : remote.getServer() + "/" + remote.getName();
    }

    public String arenaName() {
        return local != null ? local.getArenaName() : remote.getName();
    }

    /** The other game server, or null for an arena on this server. */
    public String server() {
        return remote == null ? null : remote.getServer();
    }

    public boolean isRemote() {
        return remote != null;
    }

    public IArena localArena() {
        return local;
    }

    public RemoteArena remoteArena() {
        return remote;
    }

    public int players() {
        return local != null ? local.getActivePlayerCount() : remote.getPlayerCount();
    }

    public int maxPlayers() {
        return local != null ? local.getMaxPlayers() : remote.getMaxPlayers();
    }

    public boolean matches(String key) {
        return key != null && key().toLowerCase(Locale.ROOT).equals(key.toLowerCase(Locale.ROOT));
    }
}
