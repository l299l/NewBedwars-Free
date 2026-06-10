package com.l299l.newbedwars.arena.team;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.player.inventory.ArmorContents;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.utils.JsonUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Team {
    private HashMap<UUID, ArmorContents> armorContents;
    private final ChatColor color;
    private final String name;
    private final IArena arena;
    private final ArrayList<Player> players;
    private TeamBed teamBed;
    private Location teamSpawn;
    /** Smaller zone — players cannot place blocks inside (block-protection area). */
    private Location teamBuildProtAreaPos1;
    private Location teamBuildProtAreaPos2;
    /** Larger whole-base zone — used for trap detection and heal pool. */
    private Location teamBasePos1;
    private Location teamBasePos2;
    private Location teamAccessories;
    private TeamShop teamShop;
    private TeamUpgrades teamUpgrades;
    private boolean isAlive;

    private Generator generator;

    public Team(ChatColor color, String name, IArena arena) {
        this.color = color;
        this.name = name;
        this.arena = arena;
        players = new ArrayList<>();
        isAlive = true;
    }

    public void start() {
        isAlive = true;
        armorContents = new HashMap<>();
        for (Player player : players) {
            player.teleport(teamSpawn);
            player.getInventory().clear();
            ArmorContents armorContents = new ArmorContents(player, this);
            armorContents.loadPlayerArmorContents(player);
            this.armorContents.put(player.getUniqueId(), armorContents);
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setInvulnerable(false);
            player.setGameMode(GameMode.SURVIVAL);
            player.setBedSpawnLocation(teamSpawn, true);
        }
        teamBed.respawn();
        teamShop.start();
        teamUpgrades.start();
        generator.start();
    }

    public void stop() {
        new ArrayList<>(players).forEach(this::removePlayer);
        players.clear();
        isAlive = false;
        if (teamBed != null) teamBed.destroy();
        if (teamShop != null) teamShop.stop();
        if (teamUpgrades != null) teamUpgrades.stop();
        if (generator != null) generator.stop();
    }

    public boolean isBedDestroyed() {
        return !teamBed.isAlive();
    }

    public ArmorContents getArmorContents(Player player) {
        return armorContents.get(player.getUniqueId());
    }

    public void addPlayer(Player player) {
        players.add(player);
        isAlive = true;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        if (players.isEmpty()) {
            isAlive = false;
        }
    }

    public boolean isAlive() {
        return isAlive;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void setTeamBed(Location teamBed) {
        this.teamBed = new TeamBed(teamBed, this);
    }

    public void clearTeamBed() {
        this.teamBed = null;
    }

    public void setTeamSpawn(Location teamSpawn) {
        this.teamSpawn = teamSpawn;
    }

    public void setTeamBuildProtAreaPos1(Location pos) {
        this.teamBuildProtAreaPos1 = pos;
    }

    public void setTeamBuildProtAreaPos2(Location pos) {
        this.teamBuildProtAreaPos2 = pos;
    }

    public Location getTeamBuildProtAreaPos1() {
        return teamBuildProtAreaPos1;
    }

    public Location getTeamBuildProtAreaPos2() {
        return teamBuildProtAreaPos2;
    }

    public boolean isBuildProtAreaPos1Set() {
        return teamBuildProtAreaPos1 != null;
    }

    public boolean isBuildProtAreaPos2Set() {
        return teamBuildProtAreaPos2 != null;
    }

    public boolean isBuildProtAreaPosSet() {
        return teamBuildProtAreaPos1 != null && teamBuildProtAreaPos2 != null;
    }

    /** Returns true if {@code loc} is inside the block-protection zone (no placement allowed). */
    public boolean isInBuildProtArea(Location loc) {
        if (teamBuildProtAreaPos1 == null || teamBuildProtAreaPos2 == null) return false;
        int minX = Math.min(teamBuildProtAreaPos1.getBlockX(), teamBuildProtAreaPos2.getBlockX());
        int maxX = Math.max(teamBuildProtAreaPos1.getBlockX(), teamBuildProtAreaPos2.getBlockX());
        int minY = Math.min(teamBuildProtAreaPos1.getBlockY(), teamBuildProtAreaPos2.getBlockY());
        int maxY = Math.max(teamBuildProtAreaPos1.getBlockY(), teamBuildProtAreaPos2.getBlockY());
        int minZ = Math.min(teamBuildProtAreaPos1.getBlockZ(), teamBuildProtAreaPos2.getBlockZ());
        int maxZ = Math.max(teamBuildProtAreaPos1.getBlockZ(), teamBuildProtAreaPos2.getBlockZ());
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
    }

    public void setTeamBasePos1(Location pos) {
        this.teamBasePos1 = pos;
    }

    public void setTeamBasePos2(Location pos) {
        this.teamBasePos2 = pos;
    }

    public Location getTeamBasePos1() {
        return teamBasePos1;
    }

    public Location getTeamBasePos2() {
        return teamBasePos2;
    }

    public boolean isBasePos1Set() {
        return teamBasePos1 != null;
    }

    public boolean isBasePos2Set() {
        return teamBasePos2 != null;
    }

    public boolean isBasePosSet() {
        return teamBasePos1 != null && teamBasePos2 != null;
    }

    /** Returns true if {@code loc} is inside the team's whole base (trap/heal pool detection). */
    public boolean isInBase(Location loc) {
        if (teamBasePos1 == null || teamBasePos2 == null) return false;
        int minX = Math.min(teamBasePos1.getBlockX(), teamBasePos2.getBlockX());
        int maxX = Math.max(teamBasePos1.getBlockX(), teamBasePos2.getBlockX());
        int minY = Math.min(teamBasePos1.getBlockY(), teamBasePos2.getBlockY());
        int maxY = Math.max(teamBasePos1.getBlockY(), teamBasePos2.getBlockY());
        int minZ = Math.min(teamBasePos1.getBlockZ(), teamBasePos2.getBlockZ());
        int maxZ = Math.max(teamBasePos1.getBlockZ(), teamBasePos2.getBlockZ());
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
    }

    public void setTeamAccessories(Location teamAccessories) {
        this.teamAccessories = teamAccessories;
    }

    public void setTeamShop(TeamShop teamShop) {
        this.teamShop = teamShop;
    }

    public void setTeamUpgrades(TeamUpgrades teamUpgrades) {
        this.teamUpgrades = teamUpgrades;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    public TeamBed getTeamBed() {
        return teamBed;
    }

    public Location getTeamSpawn() {
        return teamSpawn;
    }

    public Location getTeamAccessories() {
        return teamAccessories;
    }

    public TeamShop getTeamShop() {
        return teamShop;
    }

    public TeamUpgrades getTeamUpgrades() {
        return teamUpgrades;
    }

    public Generator getGenerator() {
        return generator;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public Boolean isBedSet() {
        return teamBed != null;
    }

    public Boolean isSpawnSet() {
        return teamSpawn != null;
    }

    public Boolean isAccessoriesSet() {
        return teamAccessories != null;
    }

    public Boolean isShopSet() {
        return teamShop != null;
    }

    public Boolean isUpgradesSet() {
        return teamUpgrades != null;
    }

    public Boolean isGeneratorSet() {
        return generator != null;
    }

    public void updateWorldReference(World world) {
        if (teamSpawn != null) teamSpawn.setWorld(world);
        if (teamBuildProtAreaPos1 != null) teamBuildProtAreaPos1.setWorld(world);
        if (teamBuildProtAreaPos2 != null) teamBuildProtAreaPos2.setWorld(world);
        if (teamBasePos1 != null) teamBasePos1.setWorld(world);
        if (teamBasePos2 != null) teamBasePos2.setWorld(world);
        if (teamAccessories != null) teamAccessories.setWorld(world);
        if (teamBed != null) teamBed.getLocation().setWorld(world);
        if (teamShop != null) teamShop.getLocation().setWorld(world);
        if (teamUpgrades != null) teamUpgrades.getLocation().setWorld(world);
        if (generator != null) generator.getLocation().setWorld(world);
    }

    public String toJson() {
        String sb = "{" +
                "\"color\": \"" + color.name() + "\"," +
                "\"name\": \"" + name + "\"," +
                "\"teamBed\": " + (teamBed == null ? "null" : JsonUtils.locationToJson(teamBed.getLocation())) + "," +
                "\"teamSpawn\": " + (teamSpawn == null ? "null" : JsonUtils.locationToJson(teamSpawn)) + "," +
                "\"teamBuildProtAreaPos1\": " + (teamBuildProtAreaPos1 == null ? "null" : JsonUtils.locationToJson(teamBuildProtAreaPos1)) + "," +
                "\"teamBuildProtAreaPos2\": " + (teamBuildProtAreaPos2 == null ? "null" : JsonUtils.locationToJson(teamBuildProtAreaPos2)) + "," +
                "\"teamBasePos1\": " + (teamBasePos1 == null ? "null" : JsonUtils.locationToJson(teamBasePos1)) + "," +
                "\"teamBasePos2\": " + (teamBasePos2 == null ? "null" : JsonUtils.locationToJson(teamBasePos2)) + "," +
                "\"teamAccessories\": " + (teamAccessories == null ? "null" : JsonUtils.locationToJson(teamAccessories)) + "," +
                "\"teamShop\": " + (teamShop == null ? "null" : teamShop.toJson()) + "," +
                "\"teamUpgrades\": " + (teamUpgrades == null ? "null" : teamUpgrades.toJson()) + "," +
                "\"generator\": " + (generator == null ? "null" : JsonUtils.generatorToJson(generator)) +
                "}";
        return sb;
    }
}
