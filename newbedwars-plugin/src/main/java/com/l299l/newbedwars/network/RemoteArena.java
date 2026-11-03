package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.view.ArenaState;
import com.l299l.newbedwars.api.view.NetworkArenaView;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;

import java.util.Optional;

public final class RemoteArena implements NetworkArenaView {
    private final String server;
    private final ArenaInfo info;

    public RemoteArena(String server, ArenaInfo info) {
        this.server = server;
        this.info = info;
    }

    public ArenaInfo info() {
        return info;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getName() {
        return info.name;
    }

    @Override
    public Optional<String> getGroup() {
        return Optional.ofNullable(info.group);
    }

    @Override
    public Optional<ArenaState> getState() {
        if (info.state == null) return Optional.empty();
        return switch (info.state) {
            case WAITING -> Optional.of(ArenaState.WAITING);
            case STARTING -> Optional.of(ArenaState.STARTING);
            case PLAYING -> Optional.of(ArenaState.PLAYING);
            case ENDING -> Optional.of(ArenaState.ENDING);
            case RESTARTING -> Optional.of(ArenaState.RESTARTING);
            case DISABLED -> Optional.empty();
        };
    }

    @Override
    public int getPlayerCount() {
        return info.players;
    }

    @Override
    public int getMaxPlayers() {
        return info.maxPlayers;
    }

    @Override
    public int getMaxInTeam() {
        return info.maxInTeam;
    }

    @Override
    public int getTeamCount() {
        return info.teams;
    }
}
