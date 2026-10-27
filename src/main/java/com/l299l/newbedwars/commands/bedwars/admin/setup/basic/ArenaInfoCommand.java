package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.gamerules.Gamerules;
import com.l299l.newbedwars.arena.generators.Generator;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoGUI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
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
        return "/bw arena [gui]";
    }

    @Override
    public String getExample() {
        return "/bw arena";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length > 1 && args[1].equalsIgnoreCase("gui")) {
            player.openInventory(new ArenaInfoGUI(arena).getInventory());
            return;
        }

        boolean isAdvanced = arena.getSetupMode() == Setup.ADVANCED_SETUP;

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
            if (isAdvanced) {
                player.sendMessage("  " + tick(team.isAccessoriesSet())   + " Accessories "
                        + (team.isAccessoriesSet() ? locStr(team.getTeamAccessories()) : ""));
            }
        }

        player.sendMessage("");
        java.util.List<Generator> generators = arena.getGenerators();
        int diamond = 0, emerald = 0;
        for (Generator g : generators) {
            if (g.getType() == GeneratorType.DIAMOND) diamond++;
            else if (g.getType() == GeneratorType.EMERALD) emerald++;
        }
        player.sendMessage(ChatColor.AQUA + "Generators: "
                + ChatColor.AQUA + diamond + " diamond  "
                + ChatColor.GREEN + emerald + " emerald");

        if (isAdvanced) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + "Advanced Settings:");
            
            // Gamerules
            Gamerules gamerules = arena.getGamerules();
            player.sendMessage("  " + ChatColor.GREEN + "Gamerules:");
            player.sendMessage("    RandomTeams: " + gameruleStr(gamerules.RandomTeams));
            player.sendMessage("    AllowParties: " + gameruleStr(gamerules.AllowParties));
            player.sendMessage("    AllowSpectators: " + gameruleStr(gamerules.AllowSpectators));
            player.sendMessage("    AllowTeamChat: " + gameruleStr(gamerules.AllowTeamChat));
            player.sendMessage("    AllowGlobalChat: " + gameruleStr(gamerules.AllowGlobalChat));
            player.sendMessage("    AllowPrivateChat: " + gameruleStr(gamerules.AllowPrivateChat));
            player.sendMessage("    AllowTeamDamage: " + gameruleStr(gamerules.AllowTeamDamage));
            player.sendMessage("    MakeSwordsPermanent: " + gameruleStr(gamerules.MakeSwordsPermanent));
            player.sendMessage("    AllowMultipleGolems: " + gameruleStr(gamerules.AllowMultipleGolems));
            player.sendMessage("    SplitPartiesToStart: " + gameruleStr(gamerules.SplitPartiesToStart));
            
            // Resource pack
            player.sendMessage("  " + field("Resource pack") + (arena.getResourcePackUrl() != null
                    ? ChatColor.WHITE + arena.getResourcePackUrl()
                    : missing("not set")));
            
            // GUI IDs
            player.sendMessage("  " + field("Shop GUI") + (arena.getShopGuiId() != null
                    ? ChatColor.WHITE + arena.getShopGuiId()
                    : missing("not set")));
            player.sendMessage("  " + field("Upgrade GUI") + (arena.getUpgradeGuiId() != null
                    ? ChatColor.WHITE + arena.getUpgradeGuiId()
                    : missing("not set")));
            
            // Generator leveling
            GeneratorLeveling leveling = arena.getGeneratorsLeveling();
            player.sendMessage("  " + field("Generator leveling") + (leveling != null
                    ? ChatColor.WHITE + leveling.getId()
                    : missing("not set")));
        }

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

    private static String gameruleStr(Boolean value) {
        if (value == null) return ChatColor.RED + "not set";
        return value ? ChatColor.GREEN + "true" : ChatColor.RED + "false";
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return List.of("gui");
        }
        return null;
    }
}
