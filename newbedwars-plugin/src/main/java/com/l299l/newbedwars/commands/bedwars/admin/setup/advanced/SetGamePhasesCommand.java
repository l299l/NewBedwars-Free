package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.phases.GamePhases;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.config.properties.Properties;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SetGamePhasesCommand extends BwCommand {

    @Override
    public String getName() {
        return "setGamePhases";
    }

    @Override
    public String getDescription() {
        return "Set which game phase list (mode timeline) this arena uses.";
    }

    @Override
    public String getSyntax() {
        return "/bw setGamePhases <id>";
    }

    @Override
    public String getExample() {
        return "/bw setGamePhases Squads";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length != 2) {
            usage(player);
            return;
        }
        GamePhases phases = find(args[1]);
        if (phases == null) {
            msg.send(player, "GamePhasesInvalid", Map.of(
                    "/id/", args[1],
                    "/available/", String.join(", ", new TreeSet<>(Properties.GamePhases.keySet()))));
            return;
        }
        arena.setGamePhases(phases);
        new ArenaDataJson(arena).save();
        msg.send(player, "GamePhasesSet", Map.of("/id/", phases.getID()));
    }

    static GamePhases find(String id) {
        GamePhases exact = Properties.GamePhases.get(id);
        if (exact != null) return exact;
        for (Map.Entry<String, GamePhases> e : Properties.GamePhases.entrySet()) {
            if (e.getKey().equalsIgnoreCase(id)) return e.getValue();
        }
        return null;
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(new TreeSet<>(Properties.GamePhases.keySet()));
        }
        return List.of();
    }
}
