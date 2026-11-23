package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.modes.ModeApplier;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.properties.GameMode;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SetModeCommand extends BwCommand {

    @Override
    public String getName() {
        return "setMode";
    }

    @Override
    public String getDescription() {
        return "Copy a mode's generators, game phases, GUIs and team size into this arena.";
    }

    @Override
    public String getSyntax() {
        return "/bw setMode <mode>";
    }

    @Override
    public String getExample() {
        return "/bw setMode Squads";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length != 2) {
            usage(player);
            return;
        }
        GameMode mode = find(args[1]);
        if (mode == null) {
            msg.send(player, "ModeInvalid", Map.of("/mode/", args[1], "/available/", String.join(", ", Properties.Modes.keySet())));
            return;
        }
        ModeApplier.Result result = ModeApplier.apply(arena, mode);
        if (!result.applied()) {
            msg.send(player, "ModeMissingParts", Map.of("/mode/", mode.id(), "/missing/", String.join(", ", result.missing())));
            return;
        }
        msg.send(player, "ModeSet", Map.of("/mode/", mode.id()));
        if (result.teamsDiffer()) {
            msg.send(player, "ModeTeamsDiffer", Map.of("/mode/", mode.id(),
                    "/teams/", String.valueOf(mode.teams()), "/arenaTeams/", String.valueOf(result.arenaTeams())));
        }
    }

    static GameMode find(String id) {
        return ModeApplier.find(id);
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) return new ArrayList<>(Properties.Modes.keySet());
        return List.of();
    }
}
