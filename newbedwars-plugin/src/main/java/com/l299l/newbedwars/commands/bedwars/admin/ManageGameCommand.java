package com.l299l.newbedwars.commands.bedwars.admin;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.GameStatus;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.game.GameControlService;
import com.l299l.newbedwars.arena.game.GameControlService.Action;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageGameCommand extends BwCommand {

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public String getDescription() {
        return "Manage the current game: start, stop, next phase, broadcast a message.";
    }

    @Override
    public String getSyntax() {
        return "/bw game <start|forcestart|stop|forcestop|nextPhase|broadcast <message>>";
    }

    @Override
    public String getExample() {
        return "/bw game broadcast &eRestart in 5 minutes";
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
        GameControlService.Outcome outcome;
        switch (args[1]) {
            case "forcestart" -> outcome = GameControlService.run(arena, Action.FORCE_START, player.getName());
            case "start" -> outcome = GameControlService.run(arena, Action.START, player.getName());
            case "stop" -> outcome = GameControlService.run(arena, Action.STOP, player.getName());
            case "forcestop" -> outcome = GameControlService.run(arena, Action.FORCE_STOP, player.getName());
            case "nextPhase" -> outcome = GameControlService.run(arena, Action.NEXT_PHASE, player.getName());
            case "broadcast" -> outcome = GameControlService.broadcast(arena, player.getName(),
                    String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            default -> {
                usage(player);
                return;
            }
        }
        String key = outcome.key();
        if (!outcome.success() || key.equals("PhaseAdvanced") || key.equals("PhaseAdvancedTo")) {
            msg.send(player, key, outcome.replacements());
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
        if (arena.status() == GameStatus.ending) {
            completion.add("forcestop");
        }
        completion.add("broadcast");
        return completion;
    }
}
