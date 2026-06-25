package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.gui.configuration.game.guis.ProfileGUI;
import com.l299l.newbedwars.player.PlayerIns;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class StatsCommand extends SubCommand {

    @Override public String getName() { return "stats"; }
    @Override public String getDescription() { return "View your or another player's stats."; }
    @Override public String getSyntax() { return "/bw stats [player]"; }
    @Override public String getExample() { return "/bw stats Steve"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length < 2) {
            player.openInventory(new ProfileGUI(NewBedwars.plugin.getGuiManager(), player, player).getInventory());
            return;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        String displayName = target != null ? target.getName() : targetName;
        PlayerStats stats = NewBedwars.plugin.getPlayerManager().getStats(displayName);
        PlayerIns ins = NewBedwars.plugin.getPlayerManager().getPlayer(displayName);
        String language = ins != null ? ins.language().name() : null;
        sendStatsChat(player, displayName, stats, language);
    }

    public static void sendStatsChat(Player receiver, String targetName, PlayerStats stats, String language) {
        String prefix = ChatColor.GOLD + "[NewBedwars] " + ChatColor.RESET;
        String line = prefix + ChatColor.GRAY + "------------------------------";
        receiver.sendMessage(line);
        receiver.sendMessage(prefix + ChatColor.GOLD + "" + ChatColor.BOLD + targetName + "'s Stats");
        if (language != null) {
            receiver.sendMessage(prefix + ChatColor.AQUA + "Language: " + ChatColor.WHITE + language);
        }
        receiver.sendMessage(prefix + ChatColor.GREEN + "Wins: " + ChatColor.WHITE + stats.wins()
                + "  " + ChatColor.RED + "Losses: " + ChatColor.WHITE + stats.losses());
        double wl = stats.losses() == 0 ? stats.wins() : (double) stats.wins() / stats.losses();
        receiver.sendMessage(prefix + ChatColor.YELLOW + "W/L: " + ChatColor.WHITE + String.format("%.2f", wl));
        receiver.sendMessage(prefix + ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + stats.kills()
                + "  " + ChatColor.GRAY + "Deaths: " + ChatColor.WHITE + stats.deaths());
        double kd = stats.deaths() == 0 ? stats.kills() : (double) stats.kills() / stats.deaths();
        receiver.sendMessage(prefix + ChatColor.YELLOW + "K/D: " + ChatColor.WHITE + String.format("%.2f", kd));
        receiver.sendMessage(prefix + ChatColor.AQUA + "Final Kills: " + ChatColor.WHITE + stats.finalKills());
        receiver.sendMessage(prefix + ChatColor.GOLD + "Beds Broken: " + ChatColor.WHITE + stats.bedsBroken());
        receiver.sendMessage(prefix + ChatColor.LIGHT_PURPLE + "Games Played: " + ChatColor.WHITE + stats.gamesPlayed());
        receiver.sendMessage(line);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return null;
    }
}
