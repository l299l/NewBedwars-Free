package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import org.bukkit.entity.*;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

/** Player movement in arenas: ground tracking, the void and base traps. */
public class MovementListener implements Listener {
    private final CombatTracker combat;
    private final HashMap<UUID, Set<String>> playersInBases = new HashMap<>();

    public MovementListener(CombatTracker combat) {
        this.combat = combat;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        trackGround(e);
        checkVoid(e);
        checkTraps(e);
    }

    private void trackGround(PlayerMoveEvent e) {
        if (!e.getPlayer().isOnGround()) return;
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing) return;
        if (!arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) return;
        combat.setGround(p.getUniqueId(), e.getTo().clone());
    }

    /** Waiting players and spectators are rescued from the void; players die at the Quick Void Y. */
    private void checkVoid(PlayerMoveEvent e) {
        if (e.getFrom().getBlockY() == e.getTo().getBlockY()) return;
        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null) return;
        GameStatus status = arena.status();
        // Spectators (any phase) and waiting players never die in the void: back to the waiting spawn.
        if (arena.getSpectators().contains(p)
                || (arena.isPlayerInArena(p) && (status == GameStatus.waiting || status == GameStatus.starting))) {
            if (e.getTo().getY() <= safeVoidY(arena, p.getWorld())) rescueFromVoid(p, arena);
            return;
        }
        if (status != GameStatus.playing) return;
        Integer voidY = arena.getQuickVoidY();
        if (voidY == null || e.getTo().getY() > voidY) return;
        if (!arena.isPlayerInArena(p)) return;
        UUID attackerId = combat.lastAttacker(p.getUniqueId());
        if (attackerId != null) {
            Player attacker = Bukkit.getPlayer(attackerId);
            if (attacker != null && attacker.isOnline() && arena.isPlayerInArena(attacker)) {
                p.damage(p.getMaxHealth() * 1000, attacker);
                return;
            }
        }
        combat.markVoidDeath(p.getUniqueId());
        p.damage(p.getMaxHealth() * 1000);
    }

    /** Entering an enemy base triggers its traps. */
    private void checkTraps(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player p = e.getPlayer();
        IArena arena = ArenaRegistry.byWorld(p.getWorld());
        if (arena == null || arena.status() != GameStatus.playing
                || !arena.isPlayerInArena(p) || arena.getSpectators().contains(p)) {
            // a finished game must not leave "already inside this base" state behind
            if (!playersInBases.isEmpty()) playersInBases.remove(p.getUniqueId());
            return;
        }

        Team playerTeam = arena.getTeam(p);
        if (playerTeam == null) return;

        Set<String> currentBases = playersInBases.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());

        for (Team team : arena.getTeams().values()) {
            if (team.equals(playerTeam) || !team.isAlive()) continue;
            boolean wasIn = currentBases.contains(team.getName());
            boolean isIn = team.isInBase(e.getTo());
            if (isIn && !wasIn) {
                currentBases.add(team.getName());
                TeamUpgrades tu = team.getTeamUpgrades();
                if (tu != null) tu.triggerTraps(p, arena);
            } else if (!isIn && wasIn) {
                currentBases.remove(team.getName());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        combat.forget(e.getPlayer().getUniqueId());
        playersInBases.remove(e.getPlayer().getUniqueId());
    }
}
