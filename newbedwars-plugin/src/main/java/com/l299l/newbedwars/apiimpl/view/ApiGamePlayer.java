package com.l299l.newbedwars.apiimpl.view;

import com.l299l.newbedwars.api.view.GamePlayerView;
import com.l299l.newbedwars.api.view.TeamView;
import com.l299l.newbedwars.arena.player.GamePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class ApiGamePlayer implements GamePlayerView {
    private final GamePlayer player;

    ApiGamePlayer(GamePlayer player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player.getPlayer();
    }

    @Override
    public Optional<TeamView> getTeam() {
        return player.getTeam() == null ? Optional.empty() : Optional.of(new ApiTeam(player.getTeam()));
    }

    @Override
    public int getKills() {
        return player.getKills();
    }

    @Override
    public int getFinalKills() {
        return player.getFinalKills();
    }

    @Override
    public int getDeaths() {
        return player.getDeaths();
    }

    @Override
    public int getBedsBroken() {
        return player.getBedsBroken();
    }
}
