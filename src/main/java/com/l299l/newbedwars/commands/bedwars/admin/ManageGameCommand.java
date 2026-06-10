package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class ManageGameCommand extends SubCommand {
    private final Messages msg;

    public ManageGameCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public String getDescription() {
        return "Command to manage game. You can start, stop, reset and manage of game here.";
    }

    @Override
    public String getSyntax() {
        return "/bw game <forcestart|stop|forcestop|nextPhase>";
    }

    @Override
    public String getExample() {
        return "/bw game gui";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (!player.hasPermission("newbedwars.bw.managegame") && !player.isOp()) {
            msg.send(player, "NoPermissions");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
            return;
        }
        if (arena == null) {
            msg.send(player, "NotInArena");
            return;
        }
        switch (args[1]) {
            case "gui" -> {
                player.sendMessage(ChatColor.GOLD + "[NewBedwars] " + ChatColor.YELLOW + "The in-game game manager GUI is available in " + ChatColor.GOLD + "NewBedwars Premium" + ChatColor.YELLOW + ".");
            }
            case "forcestart" -> {
                arena.broadcast("AdminForcedStart", new HashMap<>() {{
                    put("/admin/", player.getName());
                }});
                arena.start();
            }
            case "stop" -> {
                if (arena.status() != GameStatus.playing) {
                    player.sendMessage(ChatColor.RED + "The game must be in progress to use this command.");
                    return;
                }
                arena.endGame("DRAW");
            }
            case "forcestop" -> {
                arena.broadcast("AdminStoppedGame", new HashMap<>() {{
                    put("/admin/", player.getName());
                }});
                arena.stop();
            }
            case "troll" -> {
                player.sendMessage(ChatColor.GOLD + "[NewBedwars] " + ChatColor.YELLOW + "Troll commands are available in " + ChatColor.GOLD + "NewBedwars Premium" + ChatColor.YELLOW + ".");
            }
            case "nextPhase" -> {
                if (arena.status() != GameStatus.playing) {
                    player.sendMessage(ChatColor.RED + "The game must be in progress to advance phases.");
                    return;
                }
                String next = arena.getNextGamePhase();
                arena.advancePhase();
                player.sendMessage(ChatColor.GREEN + "Advanced to next phase" + (next.isEmpty() ? "." : ": " + next));
            }
            default -> player.sendMessage(msg.getMsg(player, "CorrectUsage") + getSyntax());
        }

    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return List.of("forcestart", "stop", "forcestop", "nextPhase");
        }
        return null;
    }
}
