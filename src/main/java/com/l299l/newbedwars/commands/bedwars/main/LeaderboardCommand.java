package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public class LeaderboardCommand extends SubCommand {
    private final Messages msg;

    public LeaderboardCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "leaderboard"; }
    @Override public String getDescription() { return "Show top 10 players by a stat."; }
    @Override public String getSyntax() { return "/bw leaderboard [wins|kills|beds|fk]"; }
    @Override public String getExample() { return "/bw leaderboard wins"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.leaderboard") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        String category = args.length >= 2 ? args[1].toLowerCase() : "wins";
        List<Map.Entry<String, PlayerStats>> top;
        String title;
        ToIntFunction<PlayerStats> valueFunc;

        switch (category) {
            case "kills" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByKills(10);
                title = "LeaderboardCategoryKills";
                valueFunc = PlayerStats::kills;
            }
            case "beds" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByBeds(10);
                title = "LeaderboardCategoryBeds";
                valueFunc = PlayerStats::bedsBroken;
            }
            case "fk", "finalkills" -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByFinalKills(10);
                title = "LeaderboardCategoryFinalKills";
                valueFunc = PlayerStats::finalKills;
            }
            default -> {
                top = NewBedwars.plugin.getPlayerManager().getTopByWins(10);
                title = "LeaderboardCategoryWins";
                valueFunc = PlayerStats::wins;
            }
        }

        msg.send(player, "LeaderboardHeader", new HashMap<>() {{ put("/category/", msg.getMsg(player, title)); }});
        if (top.isEmpty()) {
            msg.send(player, "LeaderboardEmpty");
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, PlayerStats> entry = top.get(i);
            int rankIndex = i;
            msg.send(player, "LeaderboardEntry", new HashMap<>() {{
                put("/rank/", String.valueOf(rankIndex + 1));
                put("/player/", entry.getKey());
                put("/value/", String.valueOf(valueFunc.applyAsInt(entry.getValue())));
            }});
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return List.of("wins", "kills", "beds", "fk");
        return null;
    }
}
