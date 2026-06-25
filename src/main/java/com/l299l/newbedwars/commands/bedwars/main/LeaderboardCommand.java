package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public class LeaderboardCommand extends SubCommand {

    @Override public String getName() { return "leaderboard"; }
    @Override public String getDescription() { return "Show top 10 players by a stat."; }
    @Override public String getSyntax() { return "/bw leaderboard [wins|kills|beds|fk]"; }
    @Override public String getExample() { return "/bw leaderboard wins"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        String category = args.length >= 2 ? args[1].toLowerCase() : "wins";
        List<Map.Entry<String, PlayerStats>> top;
        String title;
        ToIntFunction<PlayerStats> valueFunc;

        switch (category) {
            case "kills" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByKills(10);
                title = "Kills";
                valueFunc = PlayerStats::kills;
            }
            case "beds" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByBeds(10);
                title = "Beds Broken";
                valueFunc = PlayerStats::bedsBroken;
            }
            case "fk", "finalkills" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByFinalKills(10);
                title = "Final Kills";
                valueFunc = PlayerStats::finalKills;
            }
            default -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByWins(10);
                title = "Wins";
                valueFunc = PlayerStats::wins;
            }
        }

        player.sendMessage(ChatColor.GOLD + "--- Top 10 by " + title + " ---");
        if (top.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No data yet.");
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, PlayerStats> entry = top.get(i);
            player.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                    + ChatColor.WHITE + entry.getKey()
                    + ChatColor.GRAY + " - " + ChatColor.AQUA + valueFunc.applyAsInt(entry.getValue()));
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("wins", "kills", "beds", "fk");
        return null;
    }
}
