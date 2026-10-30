package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.player.PlayerStateResetter;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Joining the server inside an arena world, and leaving the server while in an arena. */
public class ConnectionListener implements Listener {
    private final NewBedwars plugin;

    public ConnectionListener(NewBedwars plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (ArenaRegistry.byWorld(p.getWorld()) == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            LobbyService.sendToLobby(p, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.JOINED_IN_ARENA_WORLD);
            PlayerStateResetter.reset(p, org.bukkit.GameMode.SURVIVAL);
            PlayerStateResetter.clearEffects(p);
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.getPartyManager() != null) plugin.getPartyManager().cleanupOfflinePlayer(p);
        if(ArenaRegistry.byWorld(p.getWorld()) != null) {
            IArena arena = ArenaRegistry.byWorld(p.getWorld());
            if(arena.isPlayerInArena(p)) {
                arena.leave(p, LeaveReason.DISCONNECT);
            } else if (arena.getSpectators().contains(p)) {
                arena.leave(p, LeaveReason.DISCONNECT);
            }
        }
    }
}
