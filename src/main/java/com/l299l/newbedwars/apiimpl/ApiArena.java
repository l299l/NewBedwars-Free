package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.ArenaState;
import com.l299l.newbedwars.api.ArenaView;
import com.l299l.newbedwars.api.ExtraData;
import com.l299l.newbedwars.api.GeneratorView;
import com.l299l.newbedwars.api.GamePlayerView;
import com.l299l.newbedwars.api.TeamView;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.player.GamePlayer;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ApiArena implements ArenaView {
    private final IArena arena;

    public ApiArena(IArena arena) {
        this.arena = arena;
    }

    @Override
    public String getName() {
        return arena.getArenaName();
    }

    @Override
    public ArenaState getState() {
        return switch (arena.status()) {
            case waiting -> ArenaState.WAITING;
            case starting -> ArenaState.STARTING;
            case playing -> ArenaState.PLAYING;
            case ending -> ArenaState.ENDING;
            case restarting -> ArenaState.RESTARTING;
        };
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(arena.isEnabled());
    }

    @Override
    public World getWorld() {
        return arena.getArenaWorld();
    }

    @Override
    public int getMinPlayers() {
        Integer min = arena.getMinPlayers();
        return min == null ? 0 : min;
    }

    @Override
    public int getMaxInTeam() {
        Integer max = arena.getMaxInTeam();
        return max == null ? 0 : max;
    }

    @Override
    public List<Player> getPlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID id : arena.getPlayers()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) result.add(player);
        }
        return result;
    }

    @Override
    public List<Player> getSpectators() {
        return new ArrayList<>(arena.getSpectators());
    }

    @Override
    public List<TeamView> getTeams() {
        List<TeamView> result = new ArrayList<>();
        for (Team team : arena.getTeams().values()) result.add(new ApiTeam(team));
        return result;
    }

    @Override
    public Optional<TeamView> getTeam(String name) {
        Team team = arena.getTeam(name);
        return team == null ? Optional.empty() : Optional.of(new ApiTeam(team));
    }

    @Override
    public Optional<GamePlayerView> getGamePlayer(UUID player) {
        GamePlayer gamePlayer = arena.getPlayer(player);
        return gamePlayer == null ? Optional.empty() : Optional.of(new ApiGamePlayer(gamePlayer));
    }

    @Override
    public String getCurrentPhase() {
        return arena.getCurrentGamePhase();
    }

    @Override
    public int getGameTime() {
        Integer time = arena.getGameTime();
        return time == null ? 0 : time;
    }

    @Override
    public List<GeneratorView> getGenerators() {
        List<GeneratorView> result = new ArrayList<>();
        for (var generator : arena.getGenerators()) result.add(new ApiGenerator(generator));
        return result;
    }

    @Override
    public ExtraData getExtra(org.bukkit.plugin.Plugin owner) {
        return new ApiExtra(arena::getExtra, owner.getName(), () -> ApiPersistence.requestSave(arena));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ApiArena other && other.arena == arena;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(arena);
    }
}
