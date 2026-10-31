package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.gui.configuration.game.guis.ProfileGUI;
import com.l299l.newbedwars.player.PlayerProfile;
import com.l299l.newbedwars.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ProfileCommand extends BwCommand {


    @Override public String getName() { return "profile"; }
    @Override public String getDescription() { return "View your or another player's profile."; }
    @Override public String getSyntax() { return "/bw profile [player]"; }
    @Override public String getExample() { return "/bw profile Steve"; }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.profile") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (args.length < 2) {
            player.openInventory(new ProfileGUI(NewBedwars.plugin.getGuiManager(), player, player).getInventory());
            return;
        }
        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            player.openInventory(new ProfileGUI(NewBedwars.plugin.getGuiManager(), player, target).getInventory());
        } else {
            PlayerStats stats = NewBedwars.plugin.getPlayerManager().getStats(targetName);
            PlayerProfile ins = NewBedwars.plugin.getPlayerManager().getPlayer(targetName);
            String language = ins != null ? ins.language().name() : null;
            msg.send(player, "StatsOffline", Map.of("/player/", targetName));
            StatsCommand.sendStatsChat(player, targetName, stats, language);
        }
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
