package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ArenaInfoCommand extends SubCommand {
    @Override
    public String getName() {
        return "arena";
    }

    @Override
    public String getDescription() {
        return "Shows the setup status of the arena being configured.";
    }

    @Override
    public String getSyntax() {
        return "/bw arena";
    }

    @Override
    public String getExample() {
        return "/bw arena";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        String line = ChatColor.AQUA + "=-=-=-=-=-=- " + ChatColor.WHITE + "ArenaInfo"
                + ChatColor.AQUA + " =-=-=-=-=-=-";
        player.sendMessage(line);

        // Global arena fields
        player.sendMessage(field("Name") + arena.getArenaName());
        player.sendMessage(field("Setup mode") + arena.getSetupMode());
        player.sendMessage(field("Waiting time") + arena.getWaitingTime() + "s");
        player.sendMessage(field("Min players") + arena.getMinPlayers());
        player.sendMessage(field("Max per team") + arena.getMaxInTeam());
        player.sendMessage(field("Quick void Y") + (arena.getQuickVoidY() != null
                ? ChatColor.WHITE + String.valueOf(arena.getQuickVoidY()) : missing("not set")));
        player.sendMessage(field("Waiting spawn") + locStr(arena.getWaitingSpawn()));
        player.sendMessage(field("Waiting pos1") + locStr(arena.getWaitingPos1()));
        player.sendMessage(field("Waiting pos2") + locStr(arena.getWaitingPos2()));

        player.sendMessage("");
        var teams = arena.getTeams();
        player.sendMessage(ChatColor.AQUA + "Teams (" + teams.size() + "):");
        for (Team team : teams.values()) {
            player.sendMessage(team.getColor() + "" + ChatColor.BOLD + team.getName()
                    + ChatColor.RESET + ":");
            player.sendMessage("  " + tick(team.isBedSet())          + " Bed       "
                    + (team.isBedSet() ? locStr(team.getTeamBed().getLocation()) : ""));
            player.sendMessage("  " + tick(team.isSpawnSet())        + " Spawn     "
                    + locStr(team.getTeamSpawn()));
            player.sendMessage("  " + tick(team.isShopSet())         + " Shop      "
                    + (team.isShopSet() ? locStr(team.getTeamShop().getLocation()) : ""));
            player.sendMessage("  " + tick(team.isUpgradesSet())     + " Upgrades  "
                    + (team.isUpgradesSet() ? locStr(team.getTeamUpgrades().getLocation()) : ""));
            player.sendMessage("  " + tick(team.isGeneratorSet())    + " Generator "
                    + (team.isGeneratorSet() ? locStr(team.getGenerator().getLocation()) : ""));
            player.sendMessage("  " + tick(team.isBuildProtAreaPosSet()) + " BuildProt "
                    + (team.isBuildProtAreaPosSet()
                    ? locStr(team.getTeamBuildProtAreaPos1()) + " → " + locStr(team.getTeamBuildProtAreaPos2())
                    : ""));
            player.sendMessage("  " + tick(team.isBasePosSet())      + " BaseArea  "
                    + (team.isBasePosSet()
                    ? locStr(team.getTeamBasePos1()) + " → " + locStr(team.getTeamBasePos2())
                    : ""));
        }

        player.sendMessage("");
        ArrayList<Generator> generators = arena.getGenerators();
        int diamond = 0, emerald = 0;
        for (Generator g : generators) {
            if (g.getType() == GeneratorType.DIAMOND) diamond++;
            else if (g.getType() == GeneratorType.EMERALD) emerald++;
        }
        player.sendMessage(ChatColor.AQUA + "Generators: "
                + ChatColor.AQUA + diamond + " diamond  "
                + ChatColor.GREEN + emerald + " emerald");

        player.sendMessage("");
        boolean canEnable = arena.canBeEnabled();
        if (canEnable) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "✔ Arena is ready to be enabled!"
                    + ChatColor.RESET + ChatColor.GREEN + "  Use /bw enable " + arena.getArenaName());
        } else {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "✘ Arena is NOT ready to be enabled."
                    + ChatColor.RESET + ChatColor.RED + "  Complete all missing fields above.");
        }

        player.sendMessage(line);
    }

    private static String field(String label) {
        return ChatColor.GREEN + label + ": " + ChatColor.WHITE;
    }

    private static String tick(boolean set) {
        return set ? ChatColor.GREEN + "✔" : ChatColor.RED + "✘";
    }

    private static String missing(String text) {
        return ChatColor.RED + text;
    }

    private static String locStr(Location loc) {
        if (loc == null) return ChatColor.RED + "not set";
        return ChatColor.WHITE + String.valueOf(loc.getBlockX()) + " " + loc.getBlockY() + " " + loc.getBlockZ();
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
