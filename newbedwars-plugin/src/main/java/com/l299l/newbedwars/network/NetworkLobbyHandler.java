package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.SendToLobby;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Predicate;

/**
 * GAME mode: "send to lobby" means the lobby server. The local teleport still happens (so the arena code
 * keeps working as on a single server, e.g. the map vote moves players on right after it), and one second
 * later (2 ticks without a local lobby) the player goes to the lobby server unless they are in an arena again by then.
 */
public final class NetworkLobbyHandler implements Listener {
    static final long DELAY_TICKS = 20L;
    /** Without a local lobby the player waits invisibly, so they can move on almost at once. */
    static final long SHORT_DELAY_TICKS = 2L;

    private final Plugin plugin;
    private final Messenger messenger;
    private final String serverId;
    private final String lobbyServer;
    private final Predicate<Player> waitingForTicket;
    private final boolean localLobby;

    public NetworkLobbyHandler(Plugin plugin, Messenger messenger, String serverId, String lobbyServer,
                               Predicate<Player> waitingForTicket) {
        this(plugin, messenger, serverId, lobbyServer, waitingForTicket, true);
    }

    /** @param localLobby true = teleport to this server's lobby for the moment; false = wait invisibly at the world spawn */
    public NetworkLobbyHandler(Plugin plugin, Messenger messenger, String serverId, String lobbyServer,
                               Predicate<Player> waitingForTicket, boolean localLobby) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.serverId = serverId;
        this.lobbyServer = lobbyServer == null ? "" : lobbyServer;
        this.waitingForTicket = waitingForTicket;
        this.localLobby = localLobby;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSendToLobby(PlayerSendToLobbyEvent e) {
        Player p = e.getPlayer();
        switch (e.getReason()) {
            // Admin actions on this server: stay here.
            case SETUP_ENDED, RELOAD -> {
                return;
            }
            // Scheduled tasks do not run any more: move the player right away.
            case SERVER_SHUTDOWN -> {
                sendNow(p);
                return;
            }
            default -> {
            }
        }
        if (!localLobby && !Bukkit.getWorlds().isEmpty()) {
            // No lobby on game servers: the world spawn is only a waiting spot, never seen (spectator mode).
            e.setTarget(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!shouldLeave(p)) return;
            if (!localLobby) hide(p);
            sendNow(p);
        }, localLobby ? DELAY_TICKS : SHORT_DELAY_TICKS);
    }

    boolean shouldLeave(Player p) {
        return p.isOnline() && !inAnyArena(p) && !waitingForTicket.test(p);
    }

    /** Waiting spot without a lobby: spectator mode, so nobody sees the player and they cannot do anything. */
    static void hide(Player p) {
        if (p.isOnline()) p.setGameMode(org.bukkit.GameMode.SPECTATOR);
    }

    /** Sends {@code p} to the lobby server now: the configured one, else the proxy picks one. */
    public void sendNow(Player p) {
        if (!p.isOnline()) return;
        if (!lobbyServer.isEmpty()) {
            messenger.connect(p, lobbyServer);
        } else {
            messenger.send(p, NetMessage.of(serverId, new SendToLobby(List.of(p.getUniqueId()))));
        }
    }

    /**
     * Whether the player is really in an arena here: in an arena world (playing, waiting or moved on by the map vote),
     * spectating or in setup. A player with only a rejoin slot (left a running game) is not.
     */
    static boolean inAnyArena(Player p) {
        if (p.getWorld() != null && ArenaRegistry.byWorld(p.getWorld()) != null) return true;
        for (IArena a : ArenaRegistry.all()) {
            if (a.getSpectators().contains(p) || a.getPlayersOnSetup().contains(p.getUniqueId())) return true;
        }
        return false;
    }
}
