package com.l299l.newbedwars.network;

import com.l299l.newbedwars.api.events.GameEndEvent;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.function.Consumer;

public final class GameServerRestarter implements Listener {
    private final Plugin plugin;
    private final int afterGames;
    private final Runnable onRestarting;
    private final Consumer<Player> sendToLobby;
    private int finishedGames;
    private boolean restarting;
    private boolean shuttingDown;
    private BukkitTask task;

    public GameServerRestarter(Plugin plugin, int afterGames, Runnable onRestarting, Consumer<Player> sendToLobby) {
        this.plugin = plugin;
        this.afterGames = afterGames;
        this.onRestarting = onRestarting;
        this.sendToLobby = sendToLobby;
    }

    public boolean isRestarting() {
        return restarting;
    }

    public void start() {
        if (afterGames <= 0) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 40L, 40L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent e) {
        if (afterGames <= 0 || restarting) return;
        finishedGames++;
        if (finishedGames >= afterGames) {
            restarting = true;
            plugin.getLogger().info("[Network] " + finishedGames + " game(s) finished, restarting once all arenas are empty");
            onRestarting.run();
        }
    }

    private void check() {
        if (!restarting || shuttingDown || !allIdle(ArenaRegistry.all())) return;
        shuttingDown = true;
        for (Player p : Bukkit.getOnlinePlayers()) sendToLobby.accept(p);
        plugin.getLogger().info("[Network] All arenas are empty, restarting the server");
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 40L);
    }

    static boolean allIdle(Collection<IArena> arenas) {
        for (IArena a : arenas) {
            if (!a.getPlayers().isEmpty() || !a.getSpectators().isEmpty()) return false;
            GameStatus s = a.status();
            if (s == GameStatus.playing || s == GameStatus.ending || s == GameStatus.starting) return false;
        }
        return true;
    }
}
