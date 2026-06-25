package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class GamePlayer {
    private final Player player;
    private final Team team;
    private final ArmorContents inventory;
    private final GuiSave playerShopGui;
    private final GuiSave playerUpgradesGui;
    private int kills;
    private int finalKills;
    private int bedsBroken;
    private int deaths;
    private BukkitTask respawnTask;

    public GamePlayer(Player player, Team team) {
        this.player = player;
        this.team = team;
        this.inventory = new ArmorContents(player, team);
        GuiManager guiManager = NewBedwars.plugin.getGuiManager();
        IArena arena = com.l299l.newbedwars.arena.Arena.arenaByWorld.get(player.getWorld());
        String shopGuiId = arena != null ? arena.getShopGuiId() : Properties.DefaultTeamShopGui;
        String upgradesGuiId = arena != null ? arena.getUpgradeGuiId() : Properties.DefaultUpgradeShopGui;
        playerShopGui = guiManager.getGui(shopGuiId);
        playerUpgradesGui = guiManager.getGui(upgradesGuiId);
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

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public void setRespawnTask(BukkitTask task) {
        this.respawnTask = task;
    }

    public void cancelRespawnTask() {
        if (respawnTask != null && !respawnTask.isCancelled()) {
            respawnTask.cancel();
            respawnTask = null;
        }
    }
}
