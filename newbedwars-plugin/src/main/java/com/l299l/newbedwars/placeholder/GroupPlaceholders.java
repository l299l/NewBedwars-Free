package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.join.ArenaJoinService;

import java.util.List;

public final class GroupPlaceholders {
    private static final String[] FIELDS = {
            "_arenas_free", "_max_players", "_best_arena", "_players", "_playing", "_waiting", "_arenas"
    };

    private GroupPlaceholders() {}

    public static String resolve(String rest) {
        for (String field : FIELDS) {
            if (!rest.endsWith(field)) continue;
            String name = rest.substring(0, rest.length() - field.length());
            if (name.isEmpty()) return null;
            com.l299l.newbedwars.network.NetworkManager network = com.l299l.newbedwars.network.NetworkManager.get();
            if (network != null && com.l299l.newbedwars.network.NetworkManager.isLobby()) {
                return remoteValue(network.getRemoteArenas(), name, field);
            }
            ArenaGroup group = ArenaGroupManager.lookup(name);
            if (group == null) return "";
            return value(group, field);
        }
        return null;
    }

    public static String groupOf(IArena arena) {
        if (arena == null || ArenaGroupManager.get() == null) return "";
        ArenaGroup group = ArenaGroupManager.get().groupOf(arena);
        return group == null ? "" : group.getName();
    }

    private static String value(ArenaGroup group, String field) {
        List<IArena> arenas = ArenaGroupManager.arenas(group);
        int players = 0, playing = 0, waiting = 0, max = 0, free = 0;
        for (IArena a : arenas) {
            if (!a.isEnabled()) continue;
            int count = a.getActivePlayerCount();
            players += count;
            max += a.getMaxPlayers();
            GameStatus status = a.status();
            if (status == GameStatus.playing || status == GameStatus.ending) {
                playing += count;
            } else if (status == GameStatus.waiting || status == GameStatus.starting) {
                waiting += count;
                if (ArenaJoinService.freeSlots(a) > 0) free++;
            }
        }
        return switch (field) {
            case "_players" -> String.valueOf(players);
            case "_playing" -> String.valueOf(playing);
            case "_waiting" -> String.valueOf(waiting);
            case "_max_players" -> String.valueOf(max);
            case "_arenas" -> String.valueOf(arenas.size());
            case "_arenas_free" -> String.valueOf(free);
            case "_best_arena" -> {
                IArena best = ArenaJoinService.findBest(arenas, a -> true, 1);
                yield best == null ? "" : best.getArenaName();
            }
            default -> null;
        };
    }

    static String remoteValue(com.l299l.newbedwars.network.RemoteArenaRegistry registry, String name, String field) {
        List<com.l299l.newbedwars.network.RemoteArena> arenas =
                registry.matching(com.l299l.newbedwars.common.protocol.payload.TargetKind.GROUP, name);
        if (arenas.isEmpty()) return "";
        java.util.Map<String, Integer> t = com.l299l.newbedwars.network.NetworkManager.totals(arenas);
        return switch (field) {
            case "_players" -> String.valueOf(t.get("players"));
            case "_playing" -> String.valueOf(t.get("playing"));
            case "_waiting" -> String.valueOf(t.get("waiting"));
            case "_max_players" -> String.valueOf(t.get("max"));
            case "_arenas" -> String.valueOf(arenas.size());
            case "_arenas_free" -> String.valueOf(t.get("free"));
            case "_best_arena" -> {
                com.l299l.newbedwars.network.RemoteArena best = com.l299l.newbedwars.network.RemoteArenaRegistry.best(arenas, 1);
                yield best == null ? "" : best.getName();
            }
            default -> null;
        };
    }
}
