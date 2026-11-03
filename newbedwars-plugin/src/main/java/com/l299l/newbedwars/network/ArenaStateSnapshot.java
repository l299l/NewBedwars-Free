package com.l299l.newbedwars.network;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.ServerState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class ArenaStateSnapshot {
    private ArenaStateSnapshot() {}

    public static ServerState build(String serverId, Collection<IArena> arenas, Function<IArena, String> groupOf,
                                    boolean restarting, long now) {
        List<ArenaInfo> list = new ArrayList<>();
        for (IArena a : arenas) {
            list.add(info(a, groupOf.apply(a)));
        }
        list.sort(Comparator.comparing(i -> i.name, String.CASE_INSENSITIVE_ORDER));
        return new ServerState(serverId, "GAME", restarting, now, list);
    }

    public static ArenaInfo info(IArena a, String group) {
        ArenaInfo info = new ArenaInfo(a.getArenaName(), group == null || group.isEmpty() ? null : group, state(a),
                a.getActivePlayerCount(), a.getMaxPlayers(), a.getMaxInTeam(), a.getTeams().size(),
                a.getGamerules() != null && Boolean.TRUE.equals(a.getGamerules().AllowParties));
        java.util.Map<java.util.UUID, Long> slots = a.getRejoinSlots();
        if (slots != null) {
            for (java.util.Map.Entry<java.util.UUID, Long> e : slots.entrySet()) {
                info.rejoin.add(new com.l299l.newbedwars.common.protocol.payload.RejoinSlot(e.getKey(), e.getValue()));
            }
            info.rejoin.sort(java.util.Comparator.comparing(r -> r.player));
        }
        return info;
    }

    public static ArenaInfo.State state(IArena a) {
        if (!a.isEnabled()) return ArenaInfo.State.DISABLED;
        GameStatus s = a.status();
        if (s == null) return ArenaInfo.State.DISABLED;
        return switch (s) {
            case waiting -> ArenaInfo.State.WAITING;
            case starting -> ArenaInfo.State.STARTING;
            case playing -> ArenaInfo.State.PLAYING;
            case ending -> ArenaInfo.State.ENDING;
            case restarting -> ArenaInfo.State.RESTARTING;
        };
    }
}
