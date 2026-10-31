package com.l299l.newbedwars.apiimpl.adapter;

import com.l299l.newbedwars.apiimpl.view.ApiArena;
import com.l299l.newbedwars.apiimpl.view.ApiTeam;
import com.l299l.newbedwars.api.registry.UpgradeDefinition;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.shops.UpgradeHandler;
import com.l299l.newbedwars.arena.team.Team;
import org.bukkit.entity.Player;

/** Runs an addon's {@link UpgradeDefinition} as an upgrade or trap. */
public final class ApiUpgradeHandler implements UpgradeHandler {
    private final UpgradeDefinition definition;

    public ApiUpgradeHandler(UpgradeDefinition definition) {
        this.definition = definition;
    }

    @Override
    public void apply(Player player, Team team, int level) {
        if (definition.getEffect() != null) definition.getEffect().apply(player, new ApiTeam(team), level);
    }

    @Override
    public boolean reapplyOnRespawn() {
        return definition.isReapplyOnRespawn();
    }

    @Override
    public boolean isTrap() {
        return definition.getTrap() != null;
    }

    @Override
    public void trigger(Player enemy, Team team, IArena arena) {
        if (definition.getTrap() != null) definition.getTrap().trigger(enemy, new ApiTeam(team), new ApiArena(arena));
    }
}
