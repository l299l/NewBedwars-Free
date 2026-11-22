package com.l299l.newbedwars.arena.join;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.game.JoinResult;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.config.Messages;
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
        if (!p.hasPermission("newbedwars.bw.join") && !p.isOp()) {
            NewBedwars.plugin.getMessages().send(p, "NoPermissions");
            return false;
        }
        return check(p) == null;
    }

    public static JoinResult joinAsAddon(Player p, IArena arena) {
        JoinResult refused = check(p);
        return refused != null ? refused : joinArena(p, arena);
    }

    private static JoinResult check(Player p) {
        Messages msg = NewBedwars.plugin.getMessages();
        Party party = NewBedwars.plugin.getPartyManager().getParty(p);
        if (party != null && !party.isAdmin(p.getUniqueId())) {
            Player admin = Bukkit.getPlayer(party.getAdmin());

            String adminName = admin != null ? admin.getName()
                    : java.util.Objects.requireNonNullElse(NewBedwars.plugin.getPartyManager().nameOf(party.getAdmin()), "?");
            msg.send(p, "PartyOnlyAdminCanJoin", Map.of("/player/", adminName));
            return JoinResult.NOT_PARTY_LEADER;
        }
        IArena current = pendingArena(p);
        if (current != null && ArenaRegistry.byWorld(p.getWorld()) == current) {
            msg.send(p, "ArenaJoinError");
            return JoinResult.ALREADY_IN_ARENA;
        }
        return null;
    }

    public static JoinResult joinArena(Player p, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (!arena.isEnabled()) {
            msg.send(p, "ArenaNotEnabled", Map.of("/arenaname/", arena.getArenaName()));
            return JoinResult.NOT_ENABLED;
        }

        if (pendingArena(p) == arena) {
            if (arena.rejoin(p)) {
                msg.send(p, "Rejoined");
                return JoinResult.REJOINED;
            }
            msg.send(p, "RejoinFailed");
            return JoinResult.FAILED;
        }
        GameStatus status = arena.status();
        if (status == GameStatus.waiting || status == GameStatus.starting) {
            PartyManager pm = NewBedwars.plugin.getPartyManager();
            Party party = pm.getParty(p);
            if (party != null && arena.getGamerules().AllowParties) {
                List<Player> members = pm.getOnlineMembers(party);
                if (members.size() > arena.getMaxInTeam()) {
                    msg.send(p, "PartyTooBig", Map.of("/maxteam/", String.valueOf(arena.getMaxInTeam())));
                    return JoinResult.PARTY_TOO_BIG;
                }
                if (!arena.joinParty(members)) {
                    msg.send(p, "ArenaIsFullError");
                    return freeSlots(arena) >= members.size() ? JoinResult.CANCELLED : JoinResult.FULL;
                }
                return JoinResult.JOINED;
            } else if (party != null) {
                msg.send(p, "PartyNotAllowedInArena");
                return JoinResult.PARTIES_NOT_ALLOWED;
            } else if (!arena.join(p)) {
                msg.send(p, "ArenaIsFullError");
                return freeSlots(arena) > 0 ? JoinResult.CANCELLED : JoinResult.FULL;
            }
            return JoinResult.JOINED;
        } else if (status == GameStatus.playing) {
            msg.send(p, "ArenaRunningError");
            return JoinResult.RUNNING;
        } else if (status == GameStatus.restarting) {
            msg.send(p, "ArenaRestartingError");
            return JoinResult.RESTARTING;
        } else if (status == GameStatus.ending) {
            msg.send(p, "ArenaEndingError");
            return JoinResult.ENDING;
        }
        msg.send(p, "ArenaJoinError");
        return JoinResult.FAILED;
    }

    public static JoinResult spectate(Player p, IArena target) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (!target.isEnabled()) {
            msg.send(p, "ArenaNotEnabled", Map.of("/arenaname/", target.getArenaName()));
            return JoinResult.NOT_ENABLED;
        }
        if (target.joinAsSpectator(p)) return JoinResult.SPECTATING;
        if (target.status() != GameStatus.playing && target.status() != GameStatus.ending) {
            msg.send(p, "ArenaNotRunningForSpectate");
            return JoinResult.NOT_RUNNING;
        }
        if (target.getGamerules() == null || !target.getGamerules().AllowSpectators) {
            msg.send(p, "SpectatingDisabled");
            return JoinResult.SPECTATORS_DISABLED;
        }
        msg.send(p, "SpectateFailed");
        if (target.isPlayerInArena(p) || target.getSpectators().contains(p)) return JoinResult.ALREADY_IN_ARENA;
        for (IArena other : ArenaRegistry.all()) {
            if (other != target && other.isPlayerInArena(p)) return JoinResult.FAILED;
        }
        return JoinResult.CANCELLED;
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
