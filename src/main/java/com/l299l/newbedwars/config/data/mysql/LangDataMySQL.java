package com.l299l.newbedwars.config.data.mysql;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.config.data.mysql.models.PlayerModel;
import com.l299l.newbedwars.config.data.mysql.repos.PlayersRepo;
import com.l299l.newbedwars.player.PlayerIns;
import com.l299l.newbedwars.player.PlayerManager;
import com.l299l.newbedwars.player.PlayerStats;

public class LangDataMySQL {
    private final MySQLManager mySQLManager;

    public LangDataMySQL(MySQLManager mySQLManager) {
        this.mySQLManager = mySQLManager;
    }

    public void load() {
        PlayersRepo playersRepo = new PlayersRepo(mySQLManager);
        PlayerManager playerManager = NewBedwars.plugin.getPlayerManager();
        for (PlayerModel playerModel : playersRepo.findAll()) {
            playerManager.addPlayer(playerModel);
            playerManager.addStats(playerModel.getName(), new PlayerStats(
                    playerModel.getWins(), playerModel.getLosses(), playerModel.getKills(),
                    playerModel.getDeaths(), playerModel.getFinalKills(),
                    playerModel.getBedsBroken(), playerModel.getGamesPlayed()
            ));
        }
        playersRepo.close();
    }

    public void save() {
        PlayersRepo playersRepo = new PlayersRepo(mySQLManager);
        PlayerManager playerManager = NewBedwars.plugin.getPlayerManager();
        for (String key : playerManager.getPlayers().keySet()) {
            PlayerIns playerIns = playerManager.getPlayers().get(key);
            playersRepo.updatePlayerLanguage(key, playerIns.language());
        }
        for (String key : playerManager.getAllStats().keySet()) {
            playersRepo.updateStats(key, playerManager.getStats(key));
        }
        playersRepo.close();
    }
}
