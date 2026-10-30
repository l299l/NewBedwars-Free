package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.GeneratorType;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.arena.setup.ArenaInfo;
import com.l299l.newbedwars.arena.setup.Setup;
import com.l299l.newbedwars.arena.team.Team;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.gui.configuration.setup.guis.ArenaInfoGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
            player.openInventory(new ArenaInfoGUI(player, arena).getInventory());
            return;
        }

        boolean isAdvanced = arena.getSetupMode() == Setup.ADVANCED_SETUP;

        String line = ChatColor.AQUA + "=-=-=-=-=-=- " + ChatColor.WHITE + text(player, "Info-Header")
                + ChatColor.AQUA + " =-=-=-=-=-=-";
        player.sendMessage(line);

        player.sendMessage(field(player, "Info-Name") + arena.getArenaName());
        player.sendMessage(field(player, "Info-SetupMode") + arena.getSetupMode());
        player.sendMessage(field(player, "Info-WaitingTime") + arena.getWaitingTime() + "s");
        player.sendMessage(field(player, "Info-MinPlayers") + arena.getMinPlayers());
        player.sendMessage(field(player, "Info-MaxPerTeam") + arena.getMaxInTeam());
        player.sendMessage(field(player, "Info-QuickVoidY") + ArenaInfo.value(player, arena.getQuickVoidY()));
        player.sendMessage(field(player, "Info-WaitingSpawn") + ArenaInfo.loc(player, arena.getWaitingSpawn()));
        player.sendMessage(field(player, "Info-WaitingPos1") + ArenaInfo.loc(player, arena.getWaitingPos1()));
        player.sendMessage(field(player, "Info-WaitingPos2") + ArenaInfo.loc(player, arena.getWaitingPos2()));

        player.sendMessage("");
        var teams = arena.getTeams();
        player.sendMessage(ChatColor.AQUA + text(player, "Info-Teams").replace("/count/", String.valueOf(teams.size())));
        for (Team team : teams.values()) {
            player.sendMessage(team.getColor() + "" + ChatColor.BOLD + team.getName()
                    + ChatColor.RESET + ":");
            for (ArenaInfo.TeamSetting setting : ArenaInfo.teamSettings(team, isAdvanced)) {
                String where = !setting.set() ? ""
                        : setting.area() ? ArenaInfo.loc(player, setting.pos1()) + " → " + ArenaInfo.loc(player, setting.pos2())
                        : ArenaInfo.loc(player, setting.pos1());
                player.sendMessage("  " + ArenaInfo.tick(setting.set()) + " " + String.format("%-10s", setting.label(player)) + where);
            }
        }

        player.sendMessage("");
        player.sendMessage(text(player, "Info-Generators")
                .replace("/diamond/", String.valueOf(ArenaInfo.generatorCount(arena, GeneratorType.DIAMOND)))
                .replace("/emerald/", String.valueOf(ArenaInfo.generatorCount(arena, GeneratorType.EMERALD))));

        if (isAdvanced) {
            player.sendMessage("");
            player.sendMessage(ChatColor.AQUA + text(player, "Info-Advanced"));

            player.sendMessage("  " + ChatColor.GREEN + text(player, "Info-Gamerules") + ":");
            ArenaInfo.gamerules(arena.getGamerules()).forEach((name, value) ->
                    player.sendMessage("    " + name + ": " + ArenaInfo.bool(player, value)));

            player.sendMessage("  " + field(player, "Info-ResourcePack") + ArenaInfo.value(player, arena.getResourcePackUrl()));
            player.sendMessage("  " + field(player, "Info-ShopGui") + ArenaInfo.value(player, arena.getShopGuiId()));
            player.sendMessage("  " + field(player, "Info-UpgradeGui") + ArenaInfo.value(player, arena.getUpgradeGuiId()));

            GeneratorLeveling leveling = arena.getGeneratorsLeveling();
            player.sendMessage("  " + field(player, "Info-GeneratorLeveling")
                    + ArenaInfo.value(player, leveling != null ? leveling.getId() : null));
        }

        player.sendMessage("");
        player.sendMessage(text(player, arena.canBeEnabled() ? "Info-Ready" : "Info-NotReady")
                .replace("/arena/", arena.getArenaName()));

        player.sendMessage(line);
    }

    private static String text(Player player, String key) {
        return ArenaInfo.text(player, key);
    }

    private static String field(Player player, String key) {
        return ChatColor.GREEN + text(player, key) + ": " + ChatColor.WHITE;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return List.of("gui");
        }
        return null;
    }
}
