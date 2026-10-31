package com.l299l.newbedwars.arena;

import com.google.gson.JsonElement;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.config.data.json.JsonUtils;
import com.l299l.newbedwars.config.data.json.arenas.ArenaMigrator;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Writes an arena file (schema {@link ArenaMigrator#CURRENT_VERSION}); loading is done with Gson in {@code ArenaDataJson}. */
final class ArenaSerializer {
    private ArenaSerializer() {}

    static String toJson(Arena arena) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"schemaVersion\": ").append(ArenaMigrator.CURRENT_VERSION).append(",");
        sb.append("\"arenaName\": ").append(JsonUtils.quote(arena.getArenaName())).append(",");
        sb.append("\"worldName\": ").append(JsonUtils.quote(arena.getWorldName())).append(",");
        sb.append("\"minPlayers\": ").append(arena.getMinPlayers()).append(",");
        sb.append("\"maxInTeam\": ").append(arena.getMaxInTeam()).append(",");
        sb.append("\"waitingTime\": ").append(arena.getWaitingTime()).append(",");
        sb.append("\"respawnTime\": ").append(arena.getRespawnTime()).append(",");
        sb.append("\"setup\": \"").append(arena.getSetupMode()).append("\",");
        sb.append("\"enabled\": ").append(arena.isEnabled()).append(",");
        sb.append("\"quickVoidY\": ").append(arena.getQuickVoidY() == null ? "null" : arena.getQuickVoidY()).append(",");
        sb.append("\"waitingPos1\": ").append(location(arena.getWaitingPos1())).append(",");
        sb.append("\"waitingPos2\": ").append(location(arena.getWaitingPos2())).append(",");
        sb.append("\"waitingSpawn\": ").append(location(arena.getWaitingSpawn())).append(",");
        sb.append("\"gamePhasesId\": ").append(JsonUtils.quote(arena.getGamePhases() != null
                ? arena.getGamePhases().getID() : orNull(arena.getUnresolvedGamePhasesId()))).append(",");
        sb.append("\"generatorsLeveling\": ").append(JsonUtils.quote(arena.getGeneratorsLeveling() != null
                ? arena.getGeneratorsLeveling().getId() : orNull(arena.getUnresolvedGeneratorsLevelingId()))).append(",");
        sb.append("\"gamerules\": ").append(arena.getGamerules().toJson()).append(",");
        List<String> teams = new ArrayList<>();
        for (Team team : arena.getTeams().values()) teams.add(team.toJson());
        sb.append("\"teams\": [").append(String.join(",", teams)).append("],");
        List<String> generators = new ArrayList<>();
        for (Generator generator : arena.getGenerators()) generators.add(JsonUtils.generatorToJson(generator));
        sb.append("\"generators\": [").append(String.join(",", generators)).append("],");
        sb.append("\"resourcePackUrl\": ").append(JsonUtils.quote(arena.getResourcePackUrl())).append(",");
        sb.append("\"resourcePackHash\": ").append(JsonUtils.quote(arena.getResourcePackHash())).append(",");
        String shopGuiId = arena.getShopGuiId();
        String upgradeGuiId = arena.getUpgradeGuiId();
        sb.append("\"shopGuiId\": ").append(JsonUtils.quote(shopGuiId == null ? Properties.DefaultTeamShopGui : shopGuiId)).append(",");
        sb.append("\"upgradeGuiId\": ").append(JsonUtils.quote(upgradeGuiId == null ? Properties.DefaultUpgradeShopGui : upgradeGuiId)).append(",");
        sb.append("\"extra\": ").append(arena.getExtra());
        for (Map.Entry<String, JsonElement> unknown : arena.getUnknownFields().entrySet()) {
            sb.append(",").append(JsonUtils.quote(unknown.getKey())).append(": ").append(unknown.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

    private static String location(Location loc) {
        return loc == null ? "null" : JsonUtils.locationToJson(loc);
    }

    /** Missing ids are written as the string "null", as before. */
    private static String orNull(String id) {
        return id != null ? id : "null";
    }
}
