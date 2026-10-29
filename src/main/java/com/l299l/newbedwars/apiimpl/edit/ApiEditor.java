package com.l299l.newbedwars.apiimpl.edit;

import com.l299l.newbedwars.apiimpl.ApiPersistence;
import com.l299l.newbedwars.api.edit.ArenaEditor;
import com.l299l.newbedwars.api.view.GeneratorKind;
import com.l299l.newbedwars.api.edit.TeamEditor;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.DiamondGenerator;
import com.l299l.newbedwars.arena.generators.EmeraldGenerator;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class ApiEditor implements ArenaEditor {
    private final IArena arena;

    public ApiEditor(IArena arena) {
        this.arena = arena;
        requireEditable();
    }

    void requireEditable() {
        if (arena.status() != GameStatus.waiting) {
            throw new IllegalStateException("arena " + arena.getArenaName() + " can only be edited while it is waiting (is " + arena.status() + ")");
        }
    }

    void requireInArenaWorld(Location location) {
        World world = arena.getArenaWorld();
        if (location == null || location.getWorld() == null || !location.getWorld().equals(world)) {
            throw new IllegalArgumentException("the location must be in the arena world");
        }
    }

    void changed() {
        ApiPersistence.requestSave(arena);
    }

    IArena arena() {
        return arena;
    }

    @Override
    public void setMinPlayers(int minPlayers) {
        requireEditable();
        if (minPlayers < 1) throw new IllegalArgumentException("minPlayers must be at least 1");
        arena.setMinPlayers(minPlayers);
        changed();
    }

    @Override
    public void setMaxInTeam(int maxInTeam) {
        requireEditable();
        if (maxInTeam < 1) throw new IllegalArgumentException("maxInTeam must be at least 1");
        arena.setMaxInTeam(maxInTeam);
        changed();
    }

    @Override
    public void setWaitingSpawn(Location location) {
        requireEditable();
        requireInArenaWorld(location);
        arena.setWaitingSpawn(location.clone());
        changed();
    }

    @Override
    public TeamEditor createTeam(String name, ChatColor color) {
        requireEditable();
        if (name == null || name.isBlank()) throw new IllegalArgumentException("the team needs a name");
        if (color == null || !color.isColor()) throw new IllegalArgumentException("not a color: " + color);
        if (arena.getTeam(name) != null) throw new IllegalArgumentException("team " + name + " already exists");
        arena.createTeam(name, color);
        changed();
        return new ApiTeamEditor(this, arena.getTeam(name));
    }

    @Override
    public Optional<TeamEditor> getTeam(String name) {
        Team team = arena.getTeam(name);
        return team == null ? Optional.empty() : Optional.of(new ApiTeamEditor(this, team));
    }

    @Override
    public boolean removeTeam(String name) {
        requireEditable();
        boolean removed = arena.removeTeam(name);
        if (removed) changed();
        return removed;
    }

    @Override
    public void addGenerator(GeneratorKind kind, Location location) {
        requireEditable();
        requireInArenaWorld(location);
        Generator generator = switch (kind) {
            case DIAMOND -> new DiamondGenerator(location.clone());
            case EMERALD -> new EmeraldGenerator(location.clone());
            default -> throw new IllegalArgumentException("only DIAMOND and EMERALD generators belong to the map; use TeamEditor#setGenerator for a team generator");
        };
        arena.addGenerator(generator);
        changed();
    }

    @Override
    public boolean removeGenerator(int index) {
        requireEditable();
        boolean removed = arena.removeGenerator(index) != null;
        if (removed) changed();
        return removed;
    }

    @Override
    public void save() {
        ApiPersistence.saveNow(arena);
    }
}
