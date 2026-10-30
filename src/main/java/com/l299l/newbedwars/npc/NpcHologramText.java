package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/** Placeholder values for the hologram above a join NPC. */
public final class NpcHologramText {
    private NpcHologramText() {}

    /**
     * {@code /group/}, {@code /arena/}, {@code /playing/}, {@code /waiting/}, {@code /arenas_free/},
     * plus {@code /players/}, {@code /max/} and {@code /status/} of the arena shown in {@code /arena/}.
     */
    public static HashMap<String, String> replacements(JoinNpcAction action, Collection<IArena> arenas) {
        List<IArena> matched = arenas.stream()
                .filter(a -> a.isEnabled())
                .filter(action::matches)
                .toList();
        int playing = 0;
        int waiting = 0;
        int free = 0;
        for (IArena a : matched) {
            GameStatus status = a.status();
            if (status == GameStatus.playing || status == GameStatus.ending) {
                playing += a.getActivePlayerCount();
            } else if (status == GameStatus.waiting || status == GameStatus.starting) {
                waiting += a.getActivePlayerCount();
                if (ArenaJoinService.freeSlots(a) > 0) free++;
            }
        }
        IArena shown;
        if (action.getType() == JoinNpcAction.Type.ARENA) {
            shown = arenas.stream().filter(action::matches).findFirst().orElse(null);
        } else {
            shown = ArenaJoinService.findBest(matched, a -> true, 1);
        }

        HashMap<String, String> r = new HashMap<>();
        r.put("/group/", action.getTarget() != null ? action.getTarget() : "*");
        r.put("/arena/", shown != null ? shown.getArenaName()
                : action.getType() == JoinNpcAction.Type.ARENA ? action.getTarget() : "-");
        r.put("/playing/", String.valueOf(playing));
        r.put("/waiting/", String.valueOf(waiting));
        r.put("/arenas_free/", String.valueOf(free));
        r.put("/players/", shown != null ? String.valueOf(shown.getActivePlayerCount()) : "0");
        r.put("/max/", shown != null ? String.valueOf(shown.getMaxPlayers()) : "0");
        r.put("/status/", shown != null ? statusName(shown) : "-");
        return r;
    }

    private static String statusName(IArena arena) {
        if (!arena.isEnabled()) return "Disabled";
        String s = arena.status().name();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
