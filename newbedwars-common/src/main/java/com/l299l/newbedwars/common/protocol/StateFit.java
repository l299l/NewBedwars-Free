package com.l299l.newbedwars.common.protocol;

import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.ServerState;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StateFit {
    public enum Result { UNCHANGED, REJOIN_DROPPED, ARENAS_TRUNCATED }

    private StateFit() {}

    public static int size(Object payload) {
        return NetCodec.payloadToJson(payload).getBytes(StandardCharsets.UTF_8).length;
    }

    public static Result fit(NetworkState state, int maxBytes) {
        if (size(state) <= maxBytes) return Result.UNCHANGED;
        for (ServerState s : state.servers) for (ArenaInfo a : s.arenas) a.rejoin = new ArrayList<>();
        int total = size(state);
        if (total <= maxBytes) return Result.REJOIN_DROPPED;

        record Ref(ServerState server, ArenaInfo arena, int bytes) {}
        List<Ref> refs = new ArrayList<>();
        for (ServerState s : state.servers) for (ArenaInfo a : s.arenas) refs.add(new Ref(s, a, size(a) + 1));
        refs.sort(Comparator.comparing((Ref r) -> r.arena().isJoinable()).thenComparingInt(r -> r.arena().players));
        for (Ref r : refs) {
            if (total <= maxBytes) break;
            r.server().arenas.remove(r.arena());
            total -= r.bytes();
        }
        while (size(state) > maxBytes && !state.servers.isEmpty()) {
            ServerState last = state.servers.get(state.servers.size() - 1);
            if (last.arenas.isEmpty()) state.servers.remove(state.servers.size() - 1);
            else last.arenas.remove(last.arenas.size() - 1);
        }
        return Result.ARENAS_TRUNCATED;
    }
}
