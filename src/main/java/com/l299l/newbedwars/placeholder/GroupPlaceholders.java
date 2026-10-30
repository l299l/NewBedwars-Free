package com.l299l.newbedwars.placeholder;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.join.ArenaJoinService;

import java.util.List;

/** {@code %newbedwars_group_<name>_<field>%} and {@code %newbedwars_group%}. */
public final class GroupPlaceholders {
    /** Longest first, so {@code _arenas_free} is not read as {@code _arenas} and {@code _max_players} not as {@code _players}. */
    private static final String[] FIELDS = {
            "_arenas_free", "_max_players", "_best_arena", "_players", "_playing", "_waiting", "_arenas"
    };

    private GroupPlaceholders() {}

    /**
     * @param rest the part after {@code group_}, e.g. {@code solo_playing}
     * @return the value, {@code ""} for an unknown group, null for an unknown field
     */
    public static String resolve(String rest) {
        for (String field : FIELDS) {
            if (!rest.endsWith(field)) continue;
            String name = rest.substring(0, rest.length() - field.length());
            if (name.isEmpty()) return null;
            ArenaGroup group = ArenaGroupManager.lookup(name);
            if (group == null) return "";
            return value(group, field);
        }
        return null;
    }

    /** The group of the arena the player is in, or "". */
    public static String groupOf(IArena arena) {
        if (arena == null || ArenaGroupManager.get() == null) return "";
        ArenaGroup group = ArenaGroupManager.get().groupOf(arena);
        return group == null ? "" : group.getName();
    }

    private static String value(ArenaGroup group, String field) {
        List<IArena> arenas = ArenaGroupManager.arenas(group);
        int players = 0, playing = 0, waiting = 0, max = 0, free = 0;
        for (IArena a : arenas) {
            if (!Boolean.TRUE.equals(a.isEnabled())) continue;
            int count = a.getActivePlayerCount();
            players += count;
            max += ArenaJoinService.maxPlayers(a);
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
}
