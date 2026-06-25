package com.l299l.newbedwars.config.data.json.arenas;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.BasicGenerator;
import com.l299l.newbedwars.arena.generators.DiamondGenerator;
import com.l299l.newbedwars.arena.generators.EmeraldGenerator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.properties.Properties;
import com.l299l.newbedwars.utils.JsonUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ArenaDataJson {
    IArena arena;

    public ArenaDataJson(IArena arena) {
        this.arena = arena;
    }
    public ArenaDataJson(String json) {
        Gson gson = new Gson();
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            String arenaName = jsonObject.get("arenaName").getAsString();
            String worldName = jsonObject.get("worldName").getAsString();
            int minPlayers = jsonObject.get("minPlayers").getAsInt();
            int maxInTeam = jsonObject.get("maxInTeam").getAsInt();
            int waitingTime = jsonObject.get("waitingTime").getAsInt();
            int respawnTime = jsonObject.get("respawnTime").getAsInt();
            Setup setup = gson.fromJson(jsonObject.get("setup"), Setup.class);
            boolean enabled = jsonObject.get("enabled").getAsBoolean();
            List<LinkedTreeMap> teams = gson.fromJson(jsonObject.get("teams"), List.class);
            List<LinkedTreeMap> generators = gson.fromJson(jsonObject.get("generators"), List.class);
            HashMap<String, Boolean> specialGamerules = gson.fromJson(jsonObject.get("specialGamerules"), HashMap.class);
            String shopGuiId = jsonObject.has("shopGuiId") && !jsonObject.get("shopGuiId").isJsonNull()
                    ? jsonObject.get("shopGuiId").getAsString()
                    : (jsonObject.has("shopGui") && !jsonObject.get("shopGui").isJsonNull()
                            ? jsonObject.get("shopGui").getAsString() : null);
            String upgradeGuiId = jsonObject.has("upgradeGuiId") && !jsonObject.get("upgradeGuiId").isJsonNull()
                    ? jsonObject.get("upgradeGuiId").getAsString()
                    : (jsonObject.has("upgradeGui") && !jsonObject.get("upgradeGui").isJsonNull()
                            ? jsonObject.get("upgradeGui").getAsString() : null);
            GamePhases gamePhases = gson.fromJson(jsonObject.get("gamePhasesId"), Object.class) == null ? null : Properties.GamePhases.get(jsonObject.get("gamePhasesId").getAsString());
            GeneratorLeveling generatorLeveling = gson.fromJson(jsonObject.get("generatorsLeveling"), Object.class) == null ? null : NewBedwars.plugin.getGeneratorLeveling().get(jsonObject.get("generatorsLeveling").getAsString());
            HashMap<String, Boolean> gamerules = gson.fromJson(jsonObject.get("gamerules"), HashMap.class);

            if (NewBedwars.plugin.getServer().getWorld(worldName) == null) {
                NewBedwars.plugin.getServer().createWorld(new WorldCreator(worldName));
            }
            Location waitingSpawn = getLocation(gson.fromJson(jsonObject.get("waitingSpawn"), Object.class));
            Location waitingPos1 = getLocation(gson.fromJson(jsonObject.get("waitingPos1"), Object.class));
            Location waitingPos2 = getLocation(gson.fromJson(jsonObject.get("waitingPos2"), Object.class));
            IArena arena = new Arena(arenaName, Objects.requireNonNull(NewBedwars.plugin.getServer().getWorld(worldName)));
            arena.setMinPlayers(minPlayers);
            arena.setMaxInTeam(maxInTeam);
            arena.setWaitingTime(waitingTime);
            arena.setRespawnTime(respawnTime);
            arena.changeSetup(setup);
            arena.setSpecialGamerules(specialGamerules);
            if (enabled) {
                arena.enable();
            } else {
                arena.disable();
            }
            if (waitingSpawn != null) {
                arena.setWaitingSpawn(waitingSpawn);
            }
            if (waitingPos1 != null) {
                arena.setWaitingPos1(waitingPos1);
            }
            if (waitingPos2 != null) {
                arena.setWaitingPos2(waitingPos2);
            }
            if (jsonObject.has("quickVoidY") && !jsonObject.get("quickVoidY").isJsonNull()) {
                arena.setQuickVoidY(jsonObject.get("quickVoidY").getAsInt());
            }
            for (LinkedTreeMap team: teams) {
                JsonObject teamObject = gson.fromJson(gson.toJson(team), JsonObject.class);
                String teamName = teamObject.get("name").getAsString();
                arena.createTeam(teamName, gson.fromJson(teamObject.get("color"), ChatColor.class));
                Team arenaTeam = arena.getTeams().get(teamName);
                Location teamSpawn = getLocation(gson.fromJson(teamObject.get("teamSpawn"), Object.class));
                Location teamBed = getLocation(gson.fromJson(teamObject.get("teamBed"), Object.class));
                Location teamAccessories = getLocation(gson.fromJson(teamObject.get("teamAccessories"), Object.class));
                if (teamSpawn != null) {
                    arenaTeam.setTeamSpawn(teamSpawn);
                }
                if (teamBed != null) {
                    arenaTeam.setTeamBed(teamBed);
                }
                if (teamObject.has("teamBuildProtAreaPos1")) {
                    Location p = getLocation(gson.fromJson(teamObject.get("teamBuildProtAreaPos1"), Object.class));
                    if (p != null) arenaTeam.setTeamBuildProtAreaPos1(p);
                }
                if (teamObject.has("teamBuildProtAreaPos2")) {
                    Location p = getLocation(gson.fromJson(teamObject.get("teamBuildProtAreaPos2"), Object.class));
                    if (p != null) arenaTeam.setTeamBuildProtAreaPos2(p);
                }
                if (teamObject.has("teamBasePos1")) {
                    Location p = getLocation(gson.fromJson(teamObject.get("teamBasePos1"), Object.class));
                    if (p != null) arenaTeam.setTeamBasePos1(p);
                }
                if (teamObject.has("teamBasePos2")) {
                    Location p = getLocation(gson.fromJson(teamObject.get("teamBasePos2"), Object.class));
                    if (p != null) arenaTeam.setTeamBasePos2(p);
                }
                if (teamAccessories != null) {
                    arenaTeam.setTeamAccessories(teamAccessories);
                }
                JsonObject teamShop = gson.fromJson(teamObject.get("teamShop"), JsonObject.class);
                if (teamShop != null) {
                    arenaTeam.setTeamShop(new TeamShop(getLocation(gson.fromJson(teamShop.get("location"), Object.class)),
                            gson.fromJson(teamShop.get("entityType"), EntityType.class), arenaTeam, gson.fromJson(
                            teamShop.get("uuid"), UUID.class)));
                }
                JsonObject teamUpgrades = gson.fromJson(teamObject.get("teamUpgrades"), JsonObject.class);
                if (teamUpgrades != null) {
                    arenaTeam.setTeamUpgrades(new TeamUpgrades(getLocation(gson.fromJson(teamUpgrades.get("location"), Object.class)),
                            gson.fromJson(teamUpgrades.get("entityType"), EntityType.class), arenaTeam, gson.fromJson(
                            teamUpgrades.get("uuid"), UUID.class)));
                }
                JsonObject teamGenerator = gson.fromJson(teamObject.get("generator"), JsonObject.class);
                if (teamGenerator != null) {
                    arenaTeam.setGenerator(new BasicGenerator(getLocation(gson.fromJson(teamGenerator.get("location"), Object.class))));
                }
            }
            for (LinkedTreeMap generator: generators) {
                JsonObject generatorObject = gson.fromJson(gson.toJson(generator), JsonObject.class);
                GeneratorType generatorType = gson.fromJson(generatorObject.get("type"), GeneratorType.class);
                switch (generatorType) {
                    case BASIC:
                        arena.addGenerator(new BasicGenerator(getLocation(gson.fromJson(generatorObject.get("location"), Object.class))));
                        break;
                    case DIAMOND:
                        arena.addGenerator(new DiamondGenerator(getLocation(gson.fromJson(generatorObject.get("location"), Object.class))));
                        break;
                    case EMERALD:
                        arena.addGenerator(new EmeraldGenerator(getLocation(gson.fromJson(generatorObject.get("location"), Object.class))));
                        break;
                }
            }
            for (String gamerule: gamerules.keySet()) {
                arena.getGamerules().setGamerule(gamerule, gamerules.get(gamerule));
            }
            arena.setGamePhases(gamePhases);
            arena.setGeneratorsLeveling(generatorLeveling);
            if (shopGuiId != null) arena.setShopGuiId(shopGuiId);
            if (upgradeGuiId != null) arena.setUpgradeGuiId(upgradeGuiId);
            if (jsonObject.has("resourcePackUrl") && !jsonObject.get("resourcePackUrl").isJsonNull()) {
                arena.setResourcePackUrl(jsonObject.get("resourcePackUrl").getAsString());
            }
            if (jsonObject.has("resourcePackHash") && !jsonObject.get("resourcePackHash").isJsonNull()) {
                arena.setResourcePackHash(jsonObject.get("resourcePackHash").getAsString());
            }
            this.arena = arena;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(){
        File dataDir = new File(NewBedwars.plugin.getDataFolder(), "data/arenas");
        if (!dataDir.exists()) {
            boolean correct = dataDir.mkdirs();
            if (!correct) {
                System.out.println("[NewBedwars]: Could not create arenas data folder!");
            }
        }
        try (FileWriter file = new FileWriter(new File(dataDir, arena.getArenaName() + ".json"))) {
            file.write(JsonUtils.beautifyJson(arena.toJson()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Location getLocation(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof JsonObject jobject) {
            return new Location(
                    NewBedwars.plugin.getServer().getWorld(jobject.get("world").getAsString()),
                    jobject.get("x").getAsDouble(),
                    jobject.get("y").getAsDouble(),
                    jobject.get("z").getAsDouble(),
                    jobject.get("yaw").getAsFloat(),
                    jobject.get("pitch").getAsFloat()
            );
        }else if (object instanceof Location) {
            return (Location) object;
        }else if (object instanceof LinkedTreeMap map) {
            return new Location(
                    NewBedwars.plugin.getServer().getWorld((String) map.get("world")),
                    Double.parseDouble(map.get("x").toString()),
                    Double.parseDouble(map.get("y").toString()),
                    Double.parseDouble(map.get("z").toString()),
                    Float.parseFloat(map.get("yaw").toString()),
                    Float.parseFloat(map.get("pitch").toString())
            );
        }else {
            return null;
        }
    }
}
