package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.view.NetworkMode;
import com.l299l.newbedwars.apiimpl.ApiEvents;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.Hello;
import com.l299l.newbedwars.common.protocol.payload.JoinRequest;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import com.l299l.newbedwars.common.protocol.payload.NetworkGroups;
import com.l299l.newbedwars.common.protocol.payload.NetworkState;
import com.l299l.newbedwars.common.protocol.payload.PartySync;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Network mode ({@code Network.Mode} GAME or LOBBY). Created on every (re)load; in NONE mode nothing is
 * registered and {@link #get()} stays null, so single-server behaviour is untouched.
 */
public final class NetworkManager implements Listener {
    private static NetworkManager active;

    private final Plugin plugin;
    private final NetworkMode mode;
    private final String serverId;
    private final BukkitMessenger messenger;
    private static final long JOIN_COOLDOWN_MILLIS = 1000L;
    private final Map<UUID, Long> lastJoinRequest = new HashMap<>();
    private NetworkGroupSync groupSync;
    // GAME
    private NetworkLobbyHandler lobbyHandler;
    private JoinTicketHandler ticketHandler;
    private ArenaStatePublisher publisher;
    private GameServerRestarter restarter;
    private NetworkDatabase database;
    // LOBBY
    private final RemoteArenaRegistry remoteArenas = new RemoteArenaRegistry();
    private RejoinNotifier rejoinNotifier;

    private NetworkManager(Plugin plugin, NetworkMode mode, String serverId) {
        this.plugin = plugin;
        this.mode = mode;
        this.serverId = serverId;
        this.messenger = new BukkitMessenger(plugin, this::handle);
    }

    /** The running manager, or null in NONE mode. */
    public static NetworkManager get() {
        return active;
    }

    public static NetworkMode mode() {
        return active == null ? NetworkMode.NONE : active.mode;
    }

    public static boolean isLobby() {
        return mode() == NetworkMode.LOBBY;
    }

    public static boolean isGame() {
        return mode() == NetworkMode.GAME;
    }

    /** Stops the old manager and starts a new one for the current config (no-op for NONE). */
    public static void restart(Plugin plugin) {
        shutdown();
        NetworkMode mode = Properties.NetworkMode;
        if (mode == null || mode == NetworkMode.NONE) return;
        String id = Properties.NetworkServerId;
        if (id == null || id.isEmpty()) {
            id = Bukkit.getServer().getName() + "-" + Bukkit.getPort();
            plugin.getLogger().warning("[Network] Network.ServerId is empty, using '" + id
                    + "'. Set it to this server's name in velocity.toml.");
        }
        active = new NetworkManager(plugin, mode, id);
        active.start();
    }

    public static void shutdown() {
        if (active != null) {
            active.stop();
            active = null;
        }
    }

    private void start() {
        messenger.register();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (mode == NetworkMode.GAME) {
            if (Properties.NetworkMySQLEnabled) database = new NetworkDatabase(plugin.getLogger());
            restarter = new GameServerRestarter(plugin, Properties.NetworkRestartAfterGames,
                    () -> publisher.changed(), p -> lobbyHandler.sendNow(p));
            ticketHandler = new JoinTicketHandler(plugin, messenger, serverId, Properties.NetworkTicketTimeoutSeconds,
                    restarter::isRestarting);
            boolean localLobby = Boolean.TRUE.equals(Properties.NetworkLocalLobby);
            ticketHandler.setLocalLobby(localLobby);
            lobbyHandler = new NetworkLobbyHandler(plugin, messenger, serverId, Properties.NetworkLobbyServer,
                    ticketHandler::isAwaiting, localLobby);
            ticketHandler.setSendToLobby(lobbyHandler::sendNow);
            publisher = new ArenaStatePublisher(plugin, messenger, serverId, restarter::isRestarting, database,
                    Properties.NetworkStatePublishSeconds, Properties.NetworkMySQLHeartbeatSeconds);
            for (Listener l : List.of(lobbyHandler, ticketHandler, publisher, restarter)) {
                Bukkit.getPluginManager().registerEvents(l, plugin);
            }
            ticketHandler.start();
            publisher.start();
            restarter.start();
        }
        groupSync = new NetworkGroupSync(mode, serverId, messenger, plugin.getDataFolder().toPath(), plugin.getLogger(),
                () -> { if (publisher != null) publisher.changed(); });
        groupSync.start();
        if (mode == NetworkMode.LOBBY) {
            rejoinNotifier = new RejoinNotifier(plugin, remoteArenas);
            Bukkit.getPluginManager().registerEvents(rejoinNotifier, plugin);
            rejoinNotifier.start();
        }
        plugin.getLogger().info("[Network] Mode " + mode + " as server '" + serverId + "'");
    }

    private void stop() {
        if (groupSync != null) groupSync.stop();
        if (rejoinNotifier != null) rejoinNotifier.stop();
        if (ticketHandler != null) ticketHandler.stop();
        if (restarter != null) restarter.stop();
        if (publisher != null) {
            publisher.stop();
            if (database != null) {
                var last = publisher.snapshot();
                last.restarting = true;
                database.close(last);
            }
        }
        messenger.unregister();
    }

    public String getServerId() {
        return serverId;
    }

    public RemoteArenaRegistry getRemoteArenas() {
        return remoteArenas;
    }

    // ----- incoming -----

    void handle(Player via, NetMessage msg) {
        switch (msg.type()) {
            case JOIN_TICKET -> {
                if (ticketHandler != null) ticketHandler.onTicket(via, msg.payload(JoinTicket.class));
            }
            case PARTY_SYNC -> {
                if (NewBedwars.plugin.getPartyManager() != null) {
                    NewBedwars.plugin.getPartyManager().applyNetworkSync(msg.payload(PartySync.class));
                }
            }
            case NETWORK_STATE -> {
                // Lobbies list the network's arenas; game servers need them for the map vote across servers.
                if (remoteArenas.update(msg.payload(NetworkState.class)) && mode == NetworkMode.LOBBY) {
                    ApiEvents.networkArenasUpdate(List.copyOf(remoteArenas.all()));
                    if (rejoinNotifier != null) rejoinNotifier.tick();
                }
            }
            case GROUPS -> groupSync.onGroups(msg.payload(NetworkGroups.class));
            default -> {
                // HELLO, SERVER_STATE, JOIN_REQUEST, JOIN_REJECT, SEND_TO_LOBBY go to the proxy only.
            }
        }
    }

    /** Introduce this server to the proxy through each player that joins (it answers with parties, tickets, state). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            messenger.send(p, NetMessage.of(serverId, new Hello(serverId, mode.name(), plugin.getDescription().getVersion())));
            groupSync.sendVia(p);
        }, 5L);
    }

    // ----- LOBBY: joining a game on another server -----

    /**
     * Asks the proxy to send {@code p} (and their party, when they are its admin) to a game.
     * Checks permission and party admin like {@code /bw join} first.
     */
    public boolean requestJoin(Player p, TargetKind kind, String target) {
        return requestJoin(p, kind, target, null);
    }

    /** Like {@link #requestJoin(Player, TargetKind, String)}, only to game server {@code server} (null = any). */
    public boolean requestJoin(Player p, TargetKind kind, String target, String server) {
        if (!ArenaJoinService.mayJoin(p)) return false;
        if (!ApiEvents.networkJoinRequest(p, kind, target)) return false;
        if (!joinCooldownPassed(p.getUniqueId())) return false;
        if (!messenger.send(p, NetMessage.of(serverId, new JoinRequest(p.getUniqueId(), kind, target, server, null)))) {
            NewBedwars.plugin.getMessages().send(p, "NetworkUnavailable");
            return false;
        }
        NewBedwars.plugin.getMessages().send(p, "NetworkJoinSent");
        return true;
    }

    private boolean joinCooldownPassed(UUID player) {
        long now = System.currentTimeMillis();
        Long last = lastJoinRequest.get(player);
        if (last != null && now - last < JOIN_COOLDOWN_MILLIS) return false;
        if (lastJoinRequest.size() > 64) lastJoinRequest.values().removeIf(t -> now - t >= JOIN_COOLDOWN_MILLIS);
        lastJoinRequest.put(player, now);
        return true;
    }

    /** {@code /bw rejoin} on a lobby: back into the game the player left on a game server. */
    public boolean requestRejoin(Player p) {
        if (findRejoin(p) == null) {
            NewBedwars.plugin.getMessages().send(p, "NoArenaToRejoin");
            return false;
        }
        if (!ApiEvents.networkJoinRequest(p, TargetKind.REJOIN, null)) return false;
        if (!messenger.send(p, NetMessage.of(serverId, new JoinRequest(p.getUniqueId(), TargetKind.REJOIN, null)))) {
            NewBedwars.plugin.getMessages().send(p, "NetworkUnavailable");
            return false;
        }
        NewBedwars.plugin.getMessages().send(p, "NetworkRejoinSent");
        return true;
    }

    /** {@code /bw spectate [arena]} on a lobby: watch a running game on a game server. */
    public boolean requestSpectate(Player p, String arena, String server) {
        if (arena == null || arena.isBlank()) {
            p.openInventory(new NetworkArenaSelectGUI(p, remoteArenas.all(), true).getInventory());
            return false;
        }
        if (!ApiEvents.networkJoinRequest(p, TargetKind.SPECTATE, arena)) return false;
        if (!messenger.send(p, NetMessage.of(serverId, new JoinRequest(p.getUniqueId(), TargetKind.SPECTATE, arena, server, null)))) {
            NewBedwars.plugin.getMessages().send(p, "NetworkUnavailable");
            return false;
        }
        NewBedwars.plugin.getMessages().send(p, "NetworkSpectateSent");
        return true;
    }

    /**
     * GAME mode, map vote won by an arena on another server: moves {@code players} (a party or one player) there
     * through the proxy. Sent through the first of them.
     */
    public void moveTo(List<Player> players, String server, String arena) {
        if (players.isEmpty()) return;
        List<java.util.UUID> ids = new java.util.ArrayList<>();
        for (Player p : players) {
            ids.add(p.getUniqueId());
            if (ticketHandler != null) ticketHandler.hold(p);
        }
        Player via = players.get(0);
        if (!messenger.send(via, NetMessage.of(serverId, new JoinRequest(via.getUniqueId(), TargetKind.ARENA, arena, server, ids)))) {
            for (Player p : players) NewBedwars.plugin.getMessages().send(p, "NetworkUnavailable");
        }
    }

    /** The network arena {@code p} can still rejoin, or null. */
    public RemoteArena findRejoin(Player p) {
        return remoteArenas.findRejoin(p.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * {@code /bw join <name>} or the API: {@code server:arena} = that arena on that server, else an arena name of
     * the network (on whichever server has room), else a group.
     */
    public boolean requestJoinByName(Player p, String name) {
        if (name == null || name.isBlank()) {
            return requestJoin(p, TargetKind.ANY, null);
        } else if (name.indexOf(':') > 0) {
            return requestArena(p, name);
        } else if (remoteArenas.byName(name) != null) {
            return requestJoin(p, TargetKind.ARENA, name);
        } else {
            return requestJoin(p, TargetKind.GROUP, name);
        }
    }

    /** An {@code ARENA} target: {@code arena} (any server that has it) or {@code server:arena}. */
    public boolean requestArena(Player p, String target) {
        String server = NetworkGroups.serverOf(target);
        String arena = NetworkGroups.arenaOf(target);
        if (server != null && remoteArenas.resolveEntry(target).isEmpty()) {
            NewBedwars.plugin.getMessages().send(p, "ArenaNotExists");
            return false;
        }
        return requestJoin(p, TargetKind.ARENA, arena, server);
    }

    public void openSelector(Player p, TargetKind kind, String target) {
        p.openInventory(new NetworkArenaSelectGUI(p, remoteArenas.matching(kind, target)).getInventory());
    }

    @EventHandler
    public void onSelectorClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof NetworkArenaSelectGUI gui)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getCurrentItem() == null || e.getCurrentItem().getType().isAir()) return;
        RemoteArena arena = gui.getArena(e.getRawSlot());
        if (arena == null || !(e.getWhoClicked() instanceof Player p)) return;
        p.closeInventory();
        if (gui.isSpectate()) {
            requestSpectate(p, arena.getName(), arena.getServer());
            return;
        }
        if (!arena.info().isJoinable()) {
            NewBedwars.plugin.getMessages().send(p, "ArenaRunningError");
            return;
        }
        requestJoin(p, TargetKind.ARENA, arena.getName(), arena.getServer());
    }

    /** Placeholder values used when a LOBBY server shows network data. */
    public static Map<String, Integer> totals(List<RemoteArena> arenas) {
        int playing = 0, waiting = 0, free = 0, players = 0, max = 0;
        for (RemoteArena a : arenas) {
            var i = a.info();
            if (i.state == com.l299l.newbedwars.common.protocol.payload.ArenaInfo.State.DISABLED) continue;
            players += i.players;
            max += i.maxPlayers;
            if (i.state == com.l299l.newbedwars.common.protocol.payload.ArenaInfo.State.PLAYING
                    || i.state == com.l299l.newbedwars.common.protocol.payload.ArenaInfo.State.ENDING) {
                playing += i.players;
            } else if (i.isJoinable()) {
                waiting += i.players;
                if (i.freeSlots() > 0) free++;
            }
        }
        return Map.of("playing", playing, "waiting", waiting, "free", free, "players", players, "max", max);
    }
}
