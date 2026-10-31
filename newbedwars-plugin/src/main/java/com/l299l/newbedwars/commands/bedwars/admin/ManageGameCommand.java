package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class ManageGameCommand extends BwCommand {


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
            usage(player);
            return;
        }
        if (arena == null) {
            msg.send(player, "NotInArena");
            return;
        }
        switch (args[1]) {
            case "forcestart" -> {
                if (arena.status() == GameStatus.starting || arena.status() == GameStatus.waiting) {
                    arena.broadcast("AdminForcedStart", Map.of("/admin/", player.getName()));
                    arena.start();
                }else {
                    msg.send(player, "GameNotWaiting");
                }
            }
            case "start" -> {
                if (arena.status() == GameStatus.waiting) {
                    arena.broadcast("AdminForcedStart", Map.of("/admin/", player.getName()));
                    arena.setArenaStarting();
                }else {
                    msg.send(player, "GameNotWaiting");
                }
            }
            case "stop" -> {
                if (arena.status() != GameStatus.playing) {
                    msg.send(player, "GameNotInProgress");
                    return;
                }
                arena.endGame("DRAW");
            }
            case "forcestop" -> {
                arena.broadcast("AdminStoppedGame", Map.of("/admin/", player.getName()));
                arena.stop();
            }
            case "nextPhase" -> {
                if (arena.status() != GameStatus.playing) {
                    msg.send(player, "GameNotInProgress");
                    return;
                }
                String next = arena.getNextGamePhase();
                arena.advancePhase();
                msg.send(player, next.isEmpty() ? "PhaseAdvanced" : "PhaseAdvancedTo", Map.of("/phase/", next));
            }
            default -> usage(player);
        }

    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length != 2) return null;
        IArena arena = ArenaRegistry.byWorld(player.getWorld());
        ArrayList<String> completion = new ArrayList<>();
        if (arena == null) return completion;
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
