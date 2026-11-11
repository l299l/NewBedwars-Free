package com.l299l.newbedwars.arena.player;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.gui.GUISave;
import com.l299l.newbedwars.arena.IArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

public class GamePlayer {
    private final Player player;
    private Team team;
    private ArmorContents inventory;
    private final IArena arena;
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
        this.arena = ArenaRegistry.byWorld(player.getWorld());
    }

    public Player getPlayer() {
        return player;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
        this.inventory = new ArmorContents(player, team);
    }

    public ArmorContents getInventory() {
        return inventory;
    }

    public GUISave getPlayerShopGui() {
        String id = arena != null ? arena.getShopGuiId() : Properties.DefaultTeamShopGui;
        return NewBedwars.plugin.getGuiManager().getGui(id);
    }

    public GUISave getPlayerUpgradesGui() {
        String id = arena != null ? arena.getUpgradeGuiId() : Properties.DefaultUpgradeShopGui;
        return NewBedwars.plugin.getGuiManager().getGui(id);
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
