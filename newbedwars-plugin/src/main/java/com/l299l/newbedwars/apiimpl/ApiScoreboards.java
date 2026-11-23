package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.api.scoreboard.ScoreboardService;
import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.scoreboard.ScoreboardManager;
import com.l299l.newbedwars.scoreboard.ScoreboardOverrides;
import org.bukkit.entity.Player;

import java.util.List;

public final class ApiScoreboards implements ScoreboardService {
    private static ScoreboardManager manager() {
        return NewBedwars.plugin == null ? null : NewBedwars.plugin.getScoreboardManager();
    }

    private static IArena membership(Player player) {
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        if (arena == null) return null;
        return arena.getSpectators().contains(player) || arena.getPlayers().contains(player.getUniqueId()) ? arena : null;
    }

    private static void refreshNow(Player player) {
        IArena arena = membership(player);
        if (arena != null) arena.refreshScoreboard(player);
    }

    @Override
    public List<String> getScoreboards() {
        ScoreboardManager manager = manager();
        return manager == null ? List.of() : manager.getIds();
    }

    @Override
    public boolean isEnabled(String id) {
        ScoreboardManager manager = manager();
        return manager != null && manager.isEnabled(id);
    }

    @Override
    public String getCurrentScoreboard(Player player) {
        if (player == null) return null;
        IArena arena = membership(player);
        return arena == null ? null : arena.getCurrentScoreboardId(player);
    }

    @Override
    public void setPlayerScoreboard(Player player, String id) {
        if (player == null) throw new IllegalArgumentException("player is null");
        ScoreboardManager manager = manager();
        String found = manager == null ? null : manager.find(id);
        if (found == null) throw new IllegalArgumentException("there is no scoreboard " + id);
        IArena arena = membership(player);
        ScoreboardOverrides.set(player, found, arena == null ? null : arena.getArenaName());
        refreshNow(player);
    }

    @Override
    public void clearPlayerScoreboard(Player player) {
        if (player != null && ScoreboardOverrides.clear(player)) refreshNow(player);
    }

    @Override
    public void hide(Player player) {
        if (player == null) return;
        ScoreboardOverrides.hide(player);
        refreshNow(player);
    }

    @Override
    public void show(Player player) {
        if (player != null && ScoreboardOverrides.unhide(player)) refreshNow(player);
    }

    @Override
    public void refresh(Player player) {
        if (player != null) refreshNow(player);
    }
}
