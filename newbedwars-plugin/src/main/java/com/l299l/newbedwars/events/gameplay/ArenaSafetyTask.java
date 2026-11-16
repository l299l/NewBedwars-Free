package com.l299l.newbedwars.events.gameplay;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

import static com.l299l.newbedwars.events.gameplay.GameplayUtil.*;

public class ArenaSafetyTask implements Runnable {
    public static final long PERIOD_TICKS = 10L;

    private final CombatListener deaths;

    public ArenaSafetyTask(CombatListener deaths) {
        this.deaths = deaths;
    }

    @Override
    public void run() {
        for (IArena arena : new ArrayList<>(ArenaRegistry.all())) {
            GameStatus status = arena.status();
            if (status == GameStatus.playing) checkPlaying(arena);
            else if (status == GameStatus.waiting || status == GameStatus.starting) checkWaiting(arena);
        }
    }

    private void checkPlaying(IArena arena) {
        for (Player p : new ArrayList<>(arena.getSpectators())) {
            if (!inArenaWorld(p, arena)) continue;
            arena.ensureRespawning(p);
            if (p.getLocation().getY() <= safeVoidY(arena, p.getWorld())) rescueFromVoid(p, arena);
        }
        for (UUID id : new ArrayList<>(arena.getPlayers())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !inArenaWorld(p, arena) || arena.getSpectators().contains(p)) continue;
            if (p.getLocation().getY() <= safeVoidY(arena, p.getWorld())) deaths.voidDeath(p, arena);
        }
    }

    private void checkWaiting(IArena arena) {
        for (UUID id : new ArrayList<>(arena.getPlayers())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !inArenaWorld(p, arena)) continue;
            if (p.getLocation().getY() <= safeVoidY(arena, p.getWorld())) rescueFromVoid(p, arena);
        }
    }

    private static boolean inArenaWorld(Player p, IArena arena) {
        return p.isOnline() && ArenaRegistry.byWorld(p.getWorld()) == arena;
    }
}
