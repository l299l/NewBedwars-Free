package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GuiManager;
import com.l299l.newbedwars.gui.GuiSave;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

public class GamePlayer {
    private final Player player;
    private Team team;
    private ArmorContents inventory;
    private final GuiSave playerShopGui;
    private final GuiSave playerUpgradesGui;
    private int kills;
    private int finalKills;
    private int bedsBroken;
    private int deaths;
    private BukkitTask respawnTask;
    private Inventory virtualEnderChest;

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

    /** Teams are assigned at game start, so a player joins the waiting lobby without one. */
    public void setTeam(Team team) {
        this.team = team;
        this.inventory = new ArmorContents(player, team);
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

    /** Per-game ender chest; the player's real ender chest is never touched. */
    public Inventory getVirtualEnderChest() {
        if (virtualEnderChest == null) {
            virtualEnderChest = Bukkit.createInventory(null, 27,
                    NewBedwars.plugin.getMessages().getMsg(player, "EnderChestTitle"));
        }
        return virtualEnderChest;
    }

    public void cancelRespawnTask() {
        if (respawnTask != null && !respawnTask.isCancelled()) {
            respawnTask.cancel();
            respawnTask = null;
        }
    }
}
