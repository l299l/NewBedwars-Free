package com.l299l.newbedwars.commands.bedwars.admin.setup.advanced;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.generators.leveling.GeneratorLeveling;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class SetGeneratorLevelingCommand extends BwCommand {


    @Override
    public String getName() {
        return "setGeneratorLeveling";
    }

    @Override
    public String getDescription() {
        return "Set which generator leveling configuration this arena uses.";
    }

    @Override
    public String getSyntax() {
        return "/bw setGeneratorLeveling <configId>";
    }

    @Override
    public String getExample() {
        return "/bw setGeneratorLeveling DefaultGenerators";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        if (args.length != 2) {
            usage(player);
            return;
        }
        String configId = args[1];
        GeneratorLeveling leveling = NewBedwars.plugin.getGeneratorLeveling().get(configId);
        if (leveling == null) {
            String available = String.join(", ", NewBedwars.plugin.getGeneratorLeveling().keySet());
            msg.send(player, "GeneratorLevelingInvalid", Map.of(
                    "/configId/", configId,
                    "/available/", available));
            return;
        }
        arena.setGeneratorsLeveling(leveling);
        new ArenaDataJson(arena).save();
        msg.send(player, "GeneratorLevelingSet", Map.of("/configId/", configId));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(NewBedwars.plugin.getGeneratorLeveling().keySet());
        }
        return List.of();
    }
}
