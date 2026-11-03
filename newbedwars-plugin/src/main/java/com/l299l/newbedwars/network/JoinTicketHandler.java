package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.common.protocol.NetMessage;
import com.l299l.newbedwars.common.protocol.payload.JoinReject;
import com.l299l.newbedwars.common.protocol.payload.JoinTicket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class JoinTicketHandler implements Listener {
    static final String BYPASS_PERMISSION = "newbedwars.network.bypass";
    static final long PARTY_GRACE_MILLIS = 3000L;

    private final Plugin plugin;
    private final Messenger messenger;
    private final String serverId;
    private final long timeoutMillis;
    private final BooleanSupplier restarting;
    private Consumer<Player> sendToLobby = p -> {};
    private boolean localLobby = true;
    private final Map<UUID, Long> awaiting = new HashMap<>();
    private final Map<String, Group> groups = new HashMap<>();
    private final Map<String, Long> handled = new HashMap<>();
    private BukkitTask task;

    public JoinTicketHandler(Plugin plugin, Messenger messenger, String serverId, int timeoutSeconds, BooleanSupplier restarting) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.serverId = serverId;
        this.timeoutMillis = timeoutSeconds * 1000L;
        this.restarting = restarting;
    }

    public void setLocalLobby(boolean localLobby) {
        this.localLobby = localLobby;
    }

    public void setSendToLobby(Consumer<Player> sendToLobby) {
        this.sendToLobby = sendToLobby;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        awaiting.clear();
        groups.clear();
    }

    public void hold(Player p) {
        awaiting.put(p.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
        if (!localLobby) NetworkLobbyHandler.hide(p);
    }

    public boolean isAwaiting(Player p) {
        return awaiting.containsKey(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS_PERMISSION)) return;
        awaiting.put(p.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
        if (!localLobby) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isAwaiting(p)) NetworkLobbyHandler.hide(p);
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        awaiting.remove(e.getPlayer().getUniqueId());
    }

    public void onTicket(Player via, JoinTicket t) {
        Player p = t.player == null ? via : Bukkit.getPlayer(t.player);
        if (p == null || !p.isOnline()) return;
        String handledKey = t.requestId + ":" + p.getUniqueId();
        if (handled.containsKey(handledKey)) return;
        handled.put(handledKey, System.currentTimeMillis());
        plugin.getLogger().info("[Network] Join ticket for " + p.getName() + ": " + (t.arena != null ? "arena " + t.arena
                : t.kind + (t.target != null ? " " + t.target : "")) + (t.party.size() > 1 ? ", party of " + t.party.size() : ""));
        long deadline = deadlineOf(t, System.currentTimeMillis());
        if (deadline < System.currentTimeMillis()) {
            reject(List.of(p), t, null, JoinReject.Reason.EXPIRED);
            return;
        }
        awaiting.put(p.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
        String key = t.requestId + ":" + (t.partyId != null ? t.partyId : p.getUniqueId().toString());
        Group g = groups.computeIfAbsent(key, k -> new Group(t, deadline));
        g.arrived.add(p.getUniqueId());
        if (g.arrived.containsAll(g.expected)) {
            groups.remove(key);
            execute(g);
        }
    }

    static long deadlineOf(JoinTicket t, long arrival) {
        if (t.ttlMillis > 0) return arrival + t.ttlMillis;
        return t.expiresAt > 0 ? t.expiresAt : Long.MAX_VALUE;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Group>> it = groups.entrySet().iterator(); it.hasNext(); ) {
            Group g = it.next().getValue();
            if (now - g.firstAt >= PARTY_GRACE_MILLIS) {
                it.remove();
                execute(g);
            }
        }
        for (Iterator<Map.Entry<UUID, Long>> it = awaiting.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() > now) continue;
            it.remove();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && !NetworkLobbyHandler.inAnyArena(p)) {
                plugin.getLogger().info("[Network] " + p.getName() + " got no join ticket in time, sending them to the lobby");
                NewBedwars.plugin.getMessages().send(p, "NetworkNoTicket");
                sendToLobby.accept(p);
            }
        }
        handled.values().removeIf(time -> now - time > 60_000L);
    }

    private void execute(Group g) {
        List<Player> players = new ArrayList<>();
        for (UUID id : g.arrived) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) players.add(p);
        }
        if (players.isEmpty()) return;
        if (g.deadline < System.currentTimeMillis()) {
            reject(players, g.ticket, g.ticket.arena, JoinReject.Reason.EXPIRED);
            return;
        }
        int n = players.size();
        if (g.ticket.kind == com.l299l.newbedwars.common.protocol.payload.TargetKind.REJOIN) {
            rejoin(players.get(0), g.ticket);
            return;
        }
        if (g.ticket.kind == com.l299l.newbedwars.common.protocol.payload.TargetKind.SPECTATE) {
            spectate(players.get(0), g.ticket);
            return;
        }
        if (restarting.getAsBoolean()) {
            reject(players, g.ticket, g.ticket.arena, JoinReject.Reason.NOT_JOINABLE);
            return;
        }
        IArena arena = TicketJoin.resolve(g.ticket, ArenaRegistry.all(), ArenaRegistry::find, n);
        JoinReject.Reason why = TicketJoin.check(arena, n);
        if (why == null) {
            boolean ok = n == 1 ? arena.join(players.get(0)) : arena.joinParty(players);
            if (!ok) why = JoinReject.Reason.FULL;
        }
        if (why != null) {
            reject(players, g.ticket, arena != null ? arena.getArenaName() : g.ticket.arena, why);
            return;
        }
        for (Player p : players) awaiting.remove(p.getUniqueId());
        plugin.getLogger().info("[Network] " + players.size() + " player(s) joined arena " + arena.getArenaName() + " from a ticket");
    }

    private void spectate(Player p, JoinTicket t) {
        IArena arena = t.arena == null ? null : ArenaRegistry.find(t.arena);
        if (arena != null && arena.joinAsSpectator(p)) {
            awaiting.remove(p.getUniqueId());
            plugin.getLogger().info("[Network] " + p.getName() + " is spectating arena " + arena.getArenaName());
            return;
        }
        NewBedwars.plugin.getMessages().send(p, "SpectateFailed");
        messenger.send(p, NetMessage.of(serverId, new JoinReject(t.requestId, p.getUniqueId(), t.arena, JoinReject.Reason.NOT_JOINABLE)));
        awaiting.put(p.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
    }

    private void rejoin(Player p, JoinTicket t) {
        IArena arena = t.arena == null ? null : ArenaRegistry.find(t.arena);
        if (arena != null && arena.isPlayerInArena(p) && arena.rejoin(p)) {
            awaiting.remove(p.getUniqueId());
            NewBedwars.plugin.getMessages().send(p, "Rejoined");
            plugin.getLogger().info("[Network] " + p.getName() + " rejoined arena " + arena.getArenaName());
            return;
        }
        NewBedwars.plugin.getMessages().send(p, "RejoinFailed");
        plugin.getLogger().info("[Network] Rejoin of " + p.getName() + " into " + t.arena + " failed");
        messenger.send(p, NetMessage.of(serverId, new JoinReject(t.requestId, p.getUniqueId(), t.arena, JoinReject.Reason.OTHER)));
        awaiting.put(p.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
    }

    private void reject(List<Player> players, JoinTicket t, String arena, JoinReject.Reason why) {
        plugin.getLogger().info("[Network] Join ticket " + t.requestId + " rejected (" + why + ", arena " + arena + ")");
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (Player p : players) {
            NewBedwars.plugin.getMessages().send(p, "NetworkJoinRejected");
            messenger.send(p, NetMessage.of(serverId, new JoinReject(t.requestId, p.getUniqueId(), arena, why)));
            awaiting.put(p.getUniqueId(), deadline);
        }
    }

    private static final class Group {
        final JoinTicket ticket;
        final Set<UUID> expected = new LinkedHashSet<>();
        final Set<UUID> arrived = new LinkedHashSet<>();
        final long firstAt = System.currentTimeMillis();
        final long deadline;

        Group(JoinTicket ticket, long deadline) {
            this.ticket = ticket;
            this.deadline = deadline;
            if (ticket.party != null) expected.addAll(ticket.party);
            if (ticket.player != null) expected.add(ticket.player);
        }
    }
}
