package com.l299l.newbedwars.apiimpl.edit;

import com.l299l.newbedwars.api.edit.TeamEditor;
import com.l299l.newbedwars.arena.generators.BasicGenerator;
import com.l299l.newbedwars.arena.shops.TeamShop;
import com.l299l.newbedwars.arena.shops.TeamUpgrades;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.admin.setup.TabArgsUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public final class ApiTeamEditor implements TeamEditor {
    private final ApiEditor editor;
    private final Team team;

    ApiTeamEditor(ApiEditor editor, Team team) {
        this.editor = editor;
        this.team = team;
    }

    private void prepare(Location location) {
        editor.requireEditable();
        if (location != null) editor.requireInArenaWorld(location);
    }

    private static void requireNpcType(EntityType type) {
        if (type == null || TabArgsUtils.wrongEntities().contains(type)) {
            throw new IllegalArgumentException(type + " can not be used as an NPC (it must be a living entity)");
        }
    }

    @Override
    public String getName() {
        return team.getName();
    }

    @Override
    public void setColor(ChatColor color) {
        editor.requireEditable();
        if (color == null || !color.isColor()) throw new IllegalArgumentException("not a color: " + color);
        team.setColor(color);
        editor.changed();
    }

    @Override
    public void setSpawn(Location location) {
        prepare(location);
        team.setTeamSpawn(location.clone());
        editor.changed();
    }

    @Override
    public void setBed(Location location) {
        prepare(location);
        if (!location.getBlock().getType().name().endsWith("_BED")) {
            throw new IllegalArgumentException("there is no bed at " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
        }
        team.setTeamBed(location.clone());
        editor.changed();
    }

    @Override
    public void clearBed() {
        editor.requireEditable();
        team.clearTeamBed();
        editor.changed();
    }

    @Override
    public void setShop(Location location, EntityType npcType) {
        prepare(location);
        requireNpcType(npcType);
        team.setTeamShop(new TeamShop(location.clone(), npcType, team));
        editor.changed();
    }

    @Override
    public void clearShop() {
        editor.requireEditable();
        team.setTeamShop(null);
        editor.changed();
    }

    @Override
    public void setUpgrades(Location location, EntityType npcType) {
        prepare(location);
        requireNpcType(npcType);
        team.setTeamUpgrades(new TeamUpgrades(location.clone(), npcType, team));
        editor.changed();
    }

    @Override
    public void clearUpgrades() {
        editor.requireEditable();
        team.setTeamUpgrades(null);
        editor.changed();
    }

    @Override
    public void setGenerator(Location location) {
        prepare(location);
        team.setGenerator(new BasicGenerator(location.clone()));
        editor.changed();
    }

    @Override
    public void clearGenerator() {
        editor.requireEditable();
        team.setGenerator(null);
        editor.changed();
    }
}
