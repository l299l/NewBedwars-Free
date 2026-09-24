package com.l299l.newbedwars.commands.bedwars.admin;

import com.google.common.collect.Lists;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        return "/bw game <start|forcestart|stop|forcestop|nextPhase>";
    }

    @Override
    public String getExample() {
        return "/bw game stop";
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
            case "forcestart" -> {
                if (arena.status() == GameStatus.starting || arena.status() == GameStatus.waiting) {
                    arena.broadcast("AdminForcedStart", new HashMap<>() {{
                        put("/admin/", player.getName());
                    }});
                    arena.start();
                }else {
                    player.sendMessage(ChatColor.RED + "The game must be in waiting status to use this command.");
                }
            }
            case "start" -> {
                if (arena.status() == GameStatus.waiting) {
                    arena.broadcast("AdminForcedStart", new HashMap<>() {{
                        put("/admin/", player.getName());
                    }});
                    arena.setArenaStarting();
                }else {
                    player.sendMessage(ChatColor.RED + "The game must be in waiting status to use this command.");
                }
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
        if (args.length != 2) return null;
        IArena arena = Arena.arenaByWorld.get(player.getWorld());
        ArrayList<String> completion = new ArrayList<>();
        if (arena.status() == GameStatus.playing) {
            completion.add("stop");
            completion.add("forcestop");
            completion.add("nextPhase");
        }
        if (arena.status() == GameStatus.waiting) {
            completion.add("start");
            completion.add("forcestart");
        }
        if (arena.status() == GameStatus.starting) {
            completion.add("forcestart");
        }
        return completion;

    }
}
