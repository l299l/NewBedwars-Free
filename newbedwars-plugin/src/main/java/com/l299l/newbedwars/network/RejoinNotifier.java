package com.l299l.newbedwars.network;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.common.protocol.payload.RejoinSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LOBBY mode: a player who can still rejoin a running game on a game server gets a title, a sound and a clickable
 * chat button once, and an action bar reminder with the time left until they rejoin or the slot is gone.
 */
public final class RejoinNotifier implements Listener {
    private final Plugin plugin;
    private final RemoteArenaRegistry registry;
    /** Player -> "server/arena" they were last told about. */
    private final Map<UUID, String> announced = new HashMap<>();
    private BukkitTask task;

    public RejoinNotifier(Plugin plugin, RemoteArenaRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        announced.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        announced.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        announced.remove(e.getPlayer().getUniqueId());
    }

    /** Runs every 2 seconds and right after the network state changed. */
    void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            RemoteArena arena = registry.findRejoin(p.getUniqueId(), now);
            if (arena == null) {
                announced.remove(p.getUniqueId());
                continue;
            }
            RejoinSlot slot = arena.info().rejoinSlot(p.getUniqueId(), now);
            String key = arena.getServer() + "/" + arena.getName();
            if (!key.equals(announced.get(p.getUniqueId()))) {
                announced.put(p.getUniqueId(), key);
                announce(p, arena);
            }
            actionBar(p, arena, slot, now);
        }
    }

    private String text(Player p, String key, Map<String, String> r) {
        String t = NewBedwars.plugin.getMessages().getMsg(p, key);
        for (Map.Entry<String, String> e : r.entrySet()) t = t.replace(e.getKey(), e.getValue());
        return t;
    }

    private void announce(Player p, RemoteArena arena) {
        Map<String, String> r = Map.of("/arena/", arena.getName());
        p.sendTitle(text(p, "RejoinAvailableTitle", r), text(p, "RejoinAvailableSubtitle", r), 10, 80, 20);
        p.playSound(p.getLocation(), "block.note_block.pling", 1f, 1.5f);
        p.playSound(p.getLocation(), "entity.experience_orb.pickup", 1f, 1f);
        p.sendMessage(text(p, "RejoinAvailableChat", r));
        Component button = Component.text(text(p, "RejoinButton", r), NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/bw rejoin"))
                .hoverEvent(HoverEvent.showText(Component.text(text(p, "RejoinButtonHover", r))));
        p.sendMessage(button);
    }

    private void actionBar(Player p, RemoteArena arena, RejoinSlot slot, long now) {
        String left = slot == null || slot.expiresAt <= 0 ? "-" : format(Math.max(0, (slot.expiresAt - now) / 1000));
        p.sendActionBar(Component.text(text(p, "RejoinActionBar", Map.of("/arena/", arena.getName(), "/time/", left))));
    }

    static String format(long seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}
