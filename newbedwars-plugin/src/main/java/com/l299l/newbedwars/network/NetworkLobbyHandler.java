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

public final class NetworkLobbyHandler implements Listener {
    static final long DELAY_TICKS = 20L;

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

            case SETUP_ENDED, RELOAD -> {
                return;
            }

            case SERVER_SHUTDOWN -> {
                sendNow(p);
                return;
            }
            default -> {
            }
        }
        if (!localLobby && !Bukkit.getWorlds().isEmpty()) {

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

    static void hide(Player p) {
        if (p.isOnline()) p.setGameMode(org.bukkit.GameMode.SPECTATOR);
    }

    public void sendNow(Player p) {
        if (!p.isOnline()) return;
        if (!lobbyServer.isEmpty()) {
            messenger.connect(p, lobbyServer);
        } else {
            messenger.send(p, NetMessage.of(serverId, new SendToLobby(List.of(p.getUniqueId()))));
        }
    }

    static boolean inAnyArena(Player p) {
        if (p.getWorld() != null && ArenaRegistry.byWorld(p.getWorld()) != null) return true;
        for (IArena a : ArenaRegistry.all()) {
            if (a.getSpectators().contains(p) || a.getPlayersOnSetup().contains(p.getUniqueId())) return true;
        }
        return false;
    }
}
