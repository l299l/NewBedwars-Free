package com.l299l.newbedwars.network;

import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.common.routing.ServerSelector;
import com.l299l.newbedwars.common.protocol.payload.ArenaInfo;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.ServerState;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class RemoteArenaRegistry {
    private volatile List<RemoteArena> arenas = List.of();

    public boolean update(NetworkState state) {
        List<RemoteArena> list = new ArrayList<>();
        if (state != null && state.servers != null) {
            for (ServerState s : state.servers) {
                if (s == null || s.serverId == null || s.arenas == null) continue;
                for (ArenaInfo a : s.arenas) {
                    if (a != null && a.name != null) list.add(new RemoteArena(s.serverId, a));
                }
            }
        }
        list.sort(Comparator.comparing(RemoteArena::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RemoteArena::getServer));
        boolean changed = !sameContent(arenas, list);
        arenas = Collections.unmodifiableList(list);
        return changed;
    }

    public List<RemoteArena> all() {
        return arenas;
    }

    public List<RemoteArena> matching(TargetKind kind, String target) {
        List<RemoteArena> out = new ArrayList<>();
        for (RemoteArena a : arenas) {
            if (ServerSelector.matchesTarget(a.info(), kind, target)) out.add(a);
        }
        return out;
    }

    public List<RemoteArena> resolveEntry(String entry) {
        List<RemoteArena> out = new ArrayList<>();
        if (entry == null || entry.isBlank()) return out;
        String arena = NetworkGroups.arenaOf(entry);
        for (RemoteArena a : arenas) {
            if (a.getName().equalsIgnoreCase(arena)
                    && NetworkGroups.covers(entry, a.getServer(), a.getName())) {
                out.add(a);
            }
        }
        return out;
    }

    public List<ArenaGroup.Member> members(String entry) {
        List<ArenaGroup.Member> out = new ArrayList<>();
        for (RemoteArena a : resolveEntry(entry)) {
            out.add(new ArenaGroup.Member(a.getServer() + ":" + a.getName(),
                    a.getTeamCount(), a.getMaxInTeam()));
        }
        return out;
    }

    public RemoteArena byName(String name) {
        if (name == null) return null;
        for (RemoteArena a : arenas) {
            if (a.getName().equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    public RemoteArena findRejoin(java.util.UUID player, long now) {
        for (RemoteArena a : arenas) {
            if (a.info().rejoinSlot(player, now) != null) return a;
        }
        return null;
    }

    public static RemoteArena best(List<RemoteArena> list, int needed) {
        RemoteArena best = null;
        for (RemoteArena a : list) {
            if (!a.info().fits(Math.max(1, needed))) continue;
            if (best == null || a.getPlayerCount() > best.getPlayerCount()
                    || (a.getPlayerCount() == best.getPlayerCount() && a.getName().compareToIgnoreCase(best.getName()) < 0)) {
                best = a;
            }
        }
        return best;
    }

    private static boolean sameContent(List<RemoteArena> a, List<RemoteArena> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            ArenaInfo x = a.get(i).info();
            ArenaInfo y = b.get(i).info();
            if (!a.get(i).getServer().equals(b.get(i).getServer()) || !x.name.equals(y.name) || x.state != y.state
                    || x.players != y.players || x.maxPlayers != y.maxPlayers
                    || !java.util.Objects.equals(x.group, y.group)
                    || (x.rejoin == null ? 0 : x.rejoin.size()) != (y.rejoin == null ? 0 : y.rejoin.size())) {
                return false;
            }
        }
        return true;
    }
}
