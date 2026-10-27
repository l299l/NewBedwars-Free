package com.l299l.newbedwars.apiimpl;

import com.l299l.newbedwars.api.ExtraData;
import com.l299l.newbedwars.api.TeamView;
import com.l299l.newbedwars.arena.shops.Upgrade;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ApiTeam implements TeamView {
    private final Team team;

    public ApiTeam(Team team) {
        this.team = team;
    }

    @Override
    public String getName() {
        return team.getName();
    }

    @Override
    public ChatColor getColor() {
        return team.getColor();
    }

    @Override
    public List<Player> getPlayers() {
        return new ArrayList<>(team.getPlayers());
    }

    @Override
    public boolean isAlive() {
        return team.isAlive();
    }

    @Override
    public boolean isBedDestroyed() {
        return team.isBedDestroyed();
    }

    @Override
    public Location getSpawn() {
        return team.getTeamSpawn();
    }

    @Override
    public int getUpgradeLevel(String upgradeName) {
        if (team.getTeamUpgrades() == null) return 0;
        try {
            return team.getTeamUpgrades().getUpgradeLevel(Upgrade.valueOf(upgradeName));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public ExtraData getExtra(org.bukkit.plugin.Plugin owner) {
        return new ApiExtra(team::getExtra, owner.getName(), () -> {
            if (team.getArena() != null) ApiPersistence.requestSave(team.getArena());
        });
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ApiTeam other && other.team == team;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(team);
    }
}
