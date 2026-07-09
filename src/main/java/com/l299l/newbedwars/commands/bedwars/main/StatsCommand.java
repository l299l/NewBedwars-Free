package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.ProfileGUI;
import com.l299l.newbedwars.player.PlayerIns;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class StatsCommand extends SubCommand {
    private final Messages msg;

    public StatsCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override public String getName() { return "stats"; }
    @Override public String getDescription() { return "View your or another player's stats."; }
    @Override public String getSyntax() { return "/bw stats [player]"; }
    @Override public String getExample() { return "/bw stats Steve"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.stats") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
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
        Messages msg = NewBedwars.plugin.getMessages();
        msg.send(receiver, "StatsDivider");
        msg.send(receiver, "StatsHeader", new HashMap<>() {{ put("/player/", targetName); }});
        if (language != null) {
            msg.send(receiver, "StatsLanguage", new HashMap<>() {{ put("/language/", language); }});
        }
        msg.send(receiver, "StatsWinsLosses", new HashMap<>() {{
            put("/wins/", String.valueOf(stats.wins()));
            put("/losses/", String.valueOf(stats.losses()));
        }});
        double wl = stats.losses() == 0 ? stats.wins() : (double) stats.wins() / stats.losses();
        msg.send(receiver, "StatsWL", new HashMap<>() {{ put("/wl/", String.format("%.2f", wl)); }});
        msg.send(receiver, "StatsKillsDeaths", new HashMap<>() {{
            put("/kills/", String.valueOf(stats.kills()));
            put("/deaths/", String.valueOf(stats.deaths()));
        }});
        double kd = stats.deaths() == 0 ? stats.kills() : (double) stats.kills() / stats.deaths();
        msg.send(receiver, "StatsKD", new HashMap<>() {{ put("/kd/", String.format("%.2f", kd)); }});
        msg.send(receiver, "StatsFinalKills", new HashMap<>() {{ put("/finalkills/", String.valueOf(stats.finalKills())); }});
        msg.send(receiver, "StatsBedsBroken", new HashMap<>() {{ put("/beds/", String.valueOf(stats.bedsBroken())); }});
        msg.send(receiver, "StatsGamesPlayed", new HashMap<>() {{ put("/games/", String.valueOf(stats.gamesPlayed())); }});
        msg.send(receiver, "StatsDivider");
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
