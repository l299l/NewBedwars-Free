package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.player.PlayerManager;
import org.bukkit.entity.Player;

public class GamePlayer {
    private final Player player;
    private final Team team;
    private final ArmorContents inventory;
    private final GuiSave playerShopGui;
    private final GuiSave playerUpgradesGui;
    private int kills;
    private int finalKills;
    private int bedsBroken;

    public GamePlayer(Player player, Team team) {
        this.player = player;
        this.team = team;
        this.inventory = new ArmorContents(player, team);
        PlayerManager playerManager = NewBedwars.plugin.getPlayerManager();
        GuiManager guiManager = NewBedwars.plugin.getGuiManager();
        playerShopGui = guiManager.getGui(playerManager.getPlayer(player.getName()).shopGui());
        playerUpgradesGui = guiManager.getGui(playerManager.getPlayer(player.getName()).upgradeGui());
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public ArmorContents getInventory() {
        return inventory;
    }

    public GuiSave getPlayerShopGui() {
        return playerShopGui;
    }

    public GuiSave getPlayerUpgradesGui() {
        return playerUpgradesGui;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public void addFinalKill() {
        this.finalKills++;
    }

    public int getBedsBroken() {
        return bedsBroken;
    }

    public void addBedBroken() {
        this.bedsBroken++;
    }
}
