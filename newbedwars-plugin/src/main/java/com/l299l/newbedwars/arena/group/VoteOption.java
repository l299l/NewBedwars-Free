package com.l299l.newbedwars.arena.group;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.network.RemoteArena;

import java.util.Locale;

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

    public String key() {
        return local != null ? local.getArenaName() : remote.getServer() + "/" + remote.getName();
    }

    public String arenaName() {
        return local != null ? local.getArenaName() : remote.getName();
    }

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
