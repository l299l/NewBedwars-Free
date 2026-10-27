package com.l299l.newbedwars.config.data.json.arenas;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.l299l.newbedwars.config.data.IOExecutor;
import com.l299l.newbedwars.world.template.ArenaMaps;
import org.bukkit.World;
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
import com.l299l.newbedwars.world.chunkgenerators.VoidGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class ArenaDataJson {
    IArena arena;

    public ArenaDataJson(IArena arena) {
        this.arena = arena;
    }
    private static final Set<String> KNOWN_FIELDS = Set.of("schemaVersion", "arenaName", "worldName", "minPlayers",
            "maxInTeam", "waitingTime", "respawnTime", "setup", "enabled", "quickVoidY", "waitingPos1", "waitingPos2",
            "waitingSpawn", "gamePhasesId", "generatorsLeveling", "gamerules", "teams", "generators", "resourcePackUrl",
            "resourcePackHash", "shopGuiId", "upgradeGuiId", "shopGui", "upgradeGui", "extra");

    public ArenaDataJson(String json) {
        Gson gson = new Gson();
        String loadingArena = null;
        World loadingWorld = null;
        try {
            JsonObject jsonObject = ArenaMigrator.migrate(gson.fromJson(json, JsonObject.class));
            String arenaName = jsonObject.get("arenaName").getAsString();
            loadingArena = arenaName;
            String worldName = jsonObject.get("worldName").getAsString();
            int minPlayers = jsonObject.get("minPlayers").getAsInt();
            int maxInTeam = jsonObject.get("maxInTeam").getAsInt();
            int waitingTime = jsonObject.get("waitingTime").getAsInt();
            int respawnTime = jsonObject.get("respawnTime").getAsInt();
            Setup setup = gson.fromJson(jsonObject.get("setup"), Setup.class);
            boolean enabled = jsonObject.get("enabled").getAsBoolean();
            List<LinkedTreeMap> teams = gson.fromJson(jsonObject.get("teams"), List.class);
            List<LinkedTreeMap> generators = gson.fromJson(jsonObject.get("generators"), List.class);
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
                restoreIfNeeded(arenaName, worldName);
                WorldCreator wc = new WorldCreator(worldName);
                wc.generator(new VoidGenerator());
                NewBedwars.plugin.getServer().createWorld(wc);
            }
            loadingWorld = NewBedwars.plugin.getServer().getWorld(worldName);
            Location waitingSpawn = getLocation(gson.fromJson(jsonObject.get("waitingSpawn"), Object.class));
            Location waitingPos1 = getLocation(gson.fromJson(jsonObject.get("waitingPos1"), Object.class));
            Location waitingPos2 = getLocation(gson.fromJson(jsonObject.get("waitingPos2"), Object.class));
            IArena arena = new Arena(arenaName, Objects.requireNonNull(NewBedwars.plugin.getServer().getWorld(worldName)));
            arena.setMinPlayers(minPlayers);
            arena.setMaxInTeam(maxInTeam);
            arena.setWaitingTime(waitingTime);
            arena.setRespawnTime(respawnTime);
            arena.changeSetup(setup);
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
                if (teamObject.has("extra") && teamObject.get("extra").isJsonObject()) {
                    arenaTeam.setExtra(teamObject.getAsJsonObject("extra"));
                }
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
            if (jsonObject.has("extra") && jsonObject.get("extra").isJsonObject()) {
                arena.setExtra(jsonObject.getAsJsonObject("extra"));
            }
            JsonObject unknown = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                if (!KNOWN_FIELDS.contains(entry.getKey())) unknown.add(entry.getKey(), entry.getValue());
            }
            arena.setUnknownFields(unknown);
            if (gamePhases == null && jsonObject.has("gamePhasesId") && !jsonObject.get("gamePhasesId").isJsonNull()
                    && !"null".equals(jsonObject.get("gamePhasesId").getAsString())) {
                arena.getLoadProblems().add("unknown game phases id '" + jsonObject.get("gamePhasesId").getAsString() + "'");
            }
            if (ArenaMaps.isEnabled() && loadingWorld != null && !ArenaMaps.hasTemplate(arenaName)) {
                createMissingTemplate(arenaName, loadingWorld);
            }
            if (enabled) {
                arena.getLoadProblems().addAll(ArenaValidator.validate(arena));
                if (!arena.getLoadProblems().isEmpty()) {
                    arena.disable();
                    NewBedwars.plugin.getLogger().warning("Arena " + arenaName + " was loaded DISABLED because: "
                            + String.join("; ", arena.getLoadProblems()));
                }
            }
            this.arena = arena;
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not load arena " + loadingArena + ": " + e, e);
            if (loadingArena != null) {
                Arena.arenaByName.remove(loadingArena);
                if (loadingWorld != null) Arena.arenaByWorld.remove(loadingWorld);
            }
        }
    }

    /** Restores the world folder from the template when the last game never finished (server crash). */
    private static void restoreIfNeeded(String arenaName, String worldName) {
        if (!ArenaMaps.isEnabled() || !ArenaMaps.hasTemplate(arenaName)) return;
        boolean missing = !Files.isRegularFile(ArenaMaps.worldFolder(worldName).resolve("level.dat"));
        if (!missing && !ArenaMaps.isDirty(arenaName)) return;
        try {
            ArenaMaps.restoreOffline(arenaName, worldName);
            NewBedwars.plugin.getLogger().info("Restored map of arena " + arenaName + " from its template.");
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not restore map of arena " + arenaName, e);
        }
    }

    /** First start after the update: existing arenas get their template from the current world. */
    private static void createMissingTemplate(String arenaName, World world) {
        try {
            ArenaMaps.createTemplate(arenaName, world);
            NewBedwars.plugin.getLogger().info("Created map template for arena " + arenaName + ".");
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not create map template for arena " + arenaName
                    + " (the legacy reload will be used): " + e.getMessage());
        }
    }

    /**
     * Saves the arena. The JSON is built (and validated by parsing it) on the calling thread; the file
     * is then written atomically on the IO thread, or immediately when the plugin is shutting down.
     * The previous file is kept as a backup.
     */
    public void save(){
        String name = arena.getArenaName();
        Path target = NewBedwars.plugin.getDataFolder().toPath().resolve("data").resolve("arenas").resolve(name + ".json");
        Path backups = ArenaMaps.arenaBackupDir(name);
        int keep = Properties.BackupsKeep;
        String content;
        try {
            content = JsonUtils.beautifyJson(arena.toJson());
        } catch (IOException e) {
            NewBedwars.plugin.getLogger().log(Level.SEVERE, "Arena " + name + " could not be saved (invalid data); the old file was kept", e);
            return;
        }
        Runnable write = () -> {
            try {
                AtomicFileWriter.write(target, content, backups, keep);
            } catch (IOException e) {
                NewBedwars.plugin.getLogger().log(Level.SEVERE, "Could not write arena file " + target, e);
            }
        };
        if (NewBedwars.plugin.isEnabled()) {
            IOExecutor.submit(write);
        } else {
            write.run();
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
