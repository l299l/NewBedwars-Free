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

    /** LOBBY mode: the same placeholders, from the arenas of the network. */
    public static HashMap<String, String> remoteReplacements(JoinNpcAction action,
                                                             com.l299l.newbedwars.network.RemoteArenaRegistry registry) {
        com.l299l.newbedwars.common.protocol.payload.TargetKind kind = switch (action.getType()) {
            case ARENA -> com.l299l.newbedwars.common.protocol.payload.TargetKind.ARENA;
            default -> action.getTarget() == null
                    ? com.l299l.newbedwars.common.protocol.payload.TargetKind.ANY
                    : com.l299l.newbedwars.common.protocol.payload.TargetKind.GROUP;
        };
        // ARENA "server:arena" = only that server's arena.
        List<com.l299l.newbedwars.network.RemoteArena> matched = kind == com.l299l.newbedwars.common.protocol.payload.TargetKind.ARENA
                ? registry.resolveEntry(action.getTarget())
                : registry.matching(kind, action.getTarget());
        java.util.Map<String, Integer> totals = com.l299l.newbedwars.network.NetworkManager.totals(matched);
        com.l299l.newbedwars.network.RemoteArena shown = kind == com.l299l.newbedwars.common.protocol.payload.TargetKind.ARENA
                ? (matched.isEmpty() ? null : matched.get(0))
                : com.l299l.newbedwars.network.RemoteArenaRegistry.best(matched, 1);
        HashMap<String, String> r = new HashMap<>();
        r.put("/group/", action.getTarget() != null ? action.getTarget() : "*");
        r.put("/arena/", shown != null ? shown.getName()
                : kind == com.l299l.newbedwars.common.protocol.payload.TargetKind.ARENA ? action.getTarget() : "-");
        r.put("/playing/", String.valueOf(totals.get("playing")));
        r.put("/waiting/", String.valueOf(totals.get("waiting")));
        r.put("/arenas_free/", String.valueOf(totals.get("free")));
        r.put("/players/", shown != null ? String.valueOf(shown.getPlayerCount()) : "0");
        r.put("/max/", shown != null ? String.valueOf(shown.getMaxPlayers()) : "0");
        r.put("/status/", shown == null ? "-" : shown.info().state == null ? "Disabled"
                : Character.toUpperCase(shown.info().state.name().charAt(0)) + shown.info().state.name().substring(1).toLowerCase());
        return r;
    }
}
