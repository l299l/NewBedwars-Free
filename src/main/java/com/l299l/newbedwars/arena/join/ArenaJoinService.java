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

/** The checks and routing of {@code /bw join}, shared by the command, join NPCs and other join entry points. */
public final class ArenaJoinService {
    private ArenaJoinService() {}

    /** Joins (or rejoins) {@code arena} after all checks; sends the player the reason when it fails. */
    public static void join(Player p, IArena arena) {
        if (mayJoin(p)) joinArena(p, arena);
    }

    /**
     * Checks that do not depend on the target arena: join permission, only the party admin may join,
     * and the player is not already playing in an arena world.
     */
    public static boolean mayJoin(Player p) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (Properties.RequireJoinPermission && !p.hasPermission("newbedwars.bw.join") && !p.isOp()) {
            msg.send(p, "NoPermissions");
            return false;
        }
        Party party = NewBedwars.plugin.getPartyManager().getParty(p);
        if (party != null && !party.isAdmin(p.getUniqueId())) {
            Player admin = Bukkit.getPlayer(party.getAdmin());
            String adminName = admin != null ? admin.getName() : "?";
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

    /** Arena-specific part of the join; call {@link #mayJoin} first. */
    public static void joinArena(Player p, IArena arena) {
        Messages msg = NewBedwars.plugin.getMessages();
        if (!arena.isEnabled()) {
            msg.send(p, "ArenaNotEnabled", Map.of("/arenaname/", arena.getArenaName()));
            return;
        }
        // Joining the arena you can rejoin rejoins it; joining another one gives that place up (in Arena.join).
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

    /** How many players join together when {@code p} joins: the online party size, or 1. */
    public static int joinSize(Player p) {
        PartyManager pm = NewBedwars.plugin.getPartyManager();
        Party party = pm.getParty(p);
        return party == null ? 1 : Math.max(1, pm.getOnlineMembers(party).size());
    }

    /**
     * The best arena among {@code arenas} that match {@code filter} and still have room for
     * {@code needed} players: the fullest one, ties broken by name. {@code null} when none fits.
     */
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

    /** The arena that still holds {@code p} as a player (in game or waiting for a rejoin), or null. */
    private static IArena pendingArena(Player p) {
        for (IArena existing : ArenaRegistry.all()) {
            if (existing.isPlayerInArena(p)) return existing;
        }
        return null;
    }
}
