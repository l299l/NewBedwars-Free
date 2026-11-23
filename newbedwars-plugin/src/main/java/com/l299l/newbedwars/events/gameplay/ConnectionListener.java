package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.player.PlayerStateResetter;
import com.l299l.newbedwars.api.events.LeaveReason;
import com.l299l.newbedwars.player.LobbyService;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

public class ConnectionListener implements Listener {
    static final String ADMIN_PERMISSION = "newbedwars.bw.admin";
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
        if (plugin.getTablistManager() != null) plugin.getTablistManager().forget(p);
        com.l299l.newbedwars.scoreboard.ScoreboardOverrides.forget(p);
        if (plugin.getAddonBossBars() != null) plugin.getAddonBossBars().removeForPlayer(p);
        for (IArena arena : new ArrayList<>(ArenaRegistry.all())) {
            if (isInArena(arena, p) && !arena.getRejoinSlots().containsKey(p.getUniqueId())) {
                arena.leave(p, LeaveReason.DISCONNECT);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        checkEntered(p);
        IArena arena = ArenaRegistry.byWorld(e.getFrom());
        if (arena == null || p.getWorld() == arena.getArenaWorld()) return;
        GameStatus status = arena.status();
        if (status != GameStatus.waiting && status != GameStatus.starting
                && status != GameStatus.playing && status != GameStatus.ending) return;
        if (!isInArena(arena, p) || arena.getRejoinSlots().containsKey(p.getUniqueId())) return;
        arena.leave(p, LeaveReason.OTHER);
    }

    private void checkEntered(Player p) {
        IArena entered = ArenaRegistry.byWorld(p.getWorld());
        if (entered == null || !isOutsider(entered, p)) return;
        World world = p.getWorld();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || p.getWorld() != world || ArenaRegistry.byWorld(world) != entered) return;
            if (!isOutsider(entered, p)) return;
            if (LobbyService.sendToLobby(p, com.l299l.newbedwars.api.events.PlayerSendToLobbyEvent.Reason.LEFT_ARENA)) {
                plugin.getMessages().send(p, "NotInThisArena");
            }
        });
    }

    static boolean isOutsider(IArena arena, Player p) {
        if (!arena.isEnabled()) return false;
        if (arena.getPlayersOnSetup().contains(p.getUniqueId())) return false;
        if (isInArena(arena, p) && !arena.getRejoinSlots().containsKey(p.getUniqueId())) return false;
        return !p.hasPermission(ADMIN_PERMISSION);
    }

    private static boolean isInArena(IArena arena, Player p) {
        return arena.isPlayerInArena(p) || arena.getSpectators().contains(p);
    }
}
