package com.l299l.newbedwars.arena.join;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.parties.Party;
import com.l299l.newbedwars.parties.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class ArenaJoinService {
    private ArenaJoinService() {}

    public static void join(Player p, IArena arena) {
        if (mayJoin(p)) joinArena(p, arena);
    }

    public static boolean mayJoin(Player p) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (Properties.RequireJoinPermission && !p.hasPermission("newbedwars.bw.join") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return false;
        }
        Party party = NewBedwars.plugin.getPartyManager().getParty(p);
        if (party != null && !party.isAdmin(p.getUniqueId())) {
            Player admin = Bukkit.getPlayer(party.getAdmin());

            String adminName = admin != null ? admin.getName()
                    : java.util.Objects.requireNonNullElse(NewBedwars.plugin.getPartyManager().nameOf(party.getAdmin()), "?");
            msg.send(p, "PartyOnlyAdminCanJoin", Map.of("/player/", adminName));
            return false;
        }
        IArena current = pendingArena(p);
        if (current != null && ArenaRegistry.byWorld(p.getWorld()) == current) {
            msg.send(p, "ArenaJoinError");
            return false;
        }
        return true;
    }

    public static void joinArena(Player p, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (!arena.isEnabled()) {
            msg.send(p, "ArenaNotEnabled", Map.of("/arenaname/", arena.getArenaName()));
            return;
        }

        if (pendingArena(p) == arena) {
            if (arena.rejoin(p)) {
                msg.send(p, "Rejoined");
            } else {
                msg.send(p, "RejoinFailed");
            }
            return;
        }
        GameStatus status = arena.status();
        if (status == GameStatus.waiting || status == GameStatus.starting) {
            PartyManager pm = NewBedwars.plugin.getPartyManager();
            Party party = pm.getParty(p);
            if (party != null && arena.getGamerules().AllowParties) {
                List<Player> members = pm.getOnlineMembers(party);
                if (members.size() > arena.getMaxInTeam()) {
                    msg.send(p, "PartyTooBig", Map.of("/maxteam/", String.valueOf(arena.getMaxInTeam())));
                    return;
                }
                if (!arena.joinParty(members)) {
                    msg.send(p, "ArenaIsFullError");
                }
            } else if (party != null) {
                msg.send(p, "PartyNotAllowedInArena");
            } else if (!arena.join(p)) {
                msg.send(p, "ArenaIsFullError");
            }
        } else if (status == GameStatus.playing) {
            msg.send(p, "ArenaRunningError");
        } else if (status == GameStatus.restarting) {
            msg.send(p, "ArenaRestartingError");
        } else if (status == GameStatus.ending) {
            msg.send(p, "ArenaEndingError");
        } else {
            msg.send(p, "ArenaJoinError");
        }
    }

    public static int joinSize(Player p) {
        PartyManager pm = NewBedwars.plugin.getPartyManager();
        Party party = pm.getParty(p);
        return party == null ? 1 : Math.max(1, pm.getOnlineMembers(party).size());
    }

    public static IArena findBest(Collection<IArena> arenas, Predicate<IArena> filter, int needed) {
        return arenas.stream()
                .filter(a -> a.isEnabled())
                .filter(filter)
                .filter(a -> a.status() == GameStatus.waiting || a.status() == GameStatus.starting)
                .filter(a -> freeSlots(a) >= needed)
                .filter(a -> needed <= 1 || (a.getGamerules().AllowParties && needed <= a.getMaxInTeam()))
                .min(Comparator.<IArena>comparingInt(IArena::getActivePlayerCount).reversed()
                        .thenComparing(IArena::getArenaName, String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    public static int freeSlots(IArena arena) {
        return arena.getMaxPlayers() - arena.getActivePlayerCount();
    }

    private static IArena pendingArena(Player p) {
        for (IArena existing : ArenaRegistry.all()) {
            if (existing.isPlayerInArena(p)) return existing;
        }
        return null;
    }
}
