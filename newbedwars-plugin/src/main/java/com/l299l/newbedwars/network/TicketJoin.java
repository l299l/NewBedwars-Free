package com.l299l.newbedwars.network;

import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import com.l299l.newbedwars.npc.JoinNpcAction;

import java.util.Collection;
import java.util.function.Function;

/** Which local arena a {@link JoinTicket} goes to, and whether {@code n} players can join it. */
public final class TicketJoin {
    private TicketJoin() {}

    /**
     * The arena for the ticket: the one the proxy picked, else the best one for the ticket's target
     * (like {@code /bw join <group>}). Null when nothing fits.
     */
    public static IArena resolve(JoinTicket t, Collection<IArena> arenas, Function<String, IArena> byName, int n) {
        if (t.arena != null) return byName.apply(t.arena);
        TargetKind kind = t.kind == null ? TargetKind.ANY : t.kind;
        if (kind == TargetKind.ARENA) return t.target == null ? null : byName.apply(t.target);
        if (kind == TargetKind.ANY || t.target == null || t.target.isBlank()) {
            return ArenaJoinService.findBest(arenas, a -> true, n);
        }
        JoinNpcAction group = JoinNpcAction.tryParse("GROUP", t.target);
        return group == null ? null : ArenaJoinService.findBest(arenas, group::matches, n);
    }

    /** Null when {@code n} players can join {@code arena} together, else why not. */
    public static JoinReject.Reason check(IArena arena, int n) {
        if (arena == null) return JoinReject.Reason.NO_ARENA;
        GameStatus s = arena.status();
        if (!arena.isEnabled() || (s != GameStatus.waiting && s != GameStatus.starting)) {
            return JoinReject.Reason.NOT_JOINABLE;
        }
        if (n > 1 && (!Boolean.TRUE.equals(arena.getGamerules().AllowParties) || n > arena.getMaxInTeam())) {
            return JoinReject.Reason.PARTY_NOT_ALLOWED;
        }
        if (ArenaJoinService.freeSlots(arena) < n) return JoinReject.Reason.FULL;
        return null;
    }
}
