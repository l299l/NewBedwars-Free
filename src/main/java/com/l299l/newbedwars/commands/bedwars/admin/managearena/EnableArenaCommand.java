package com.l299l.newbedwars.commands.bedwars.admin.managearena;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.world.template.ArenaMaps;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.List;

public class EnableArenaCommand extends BwCommand {

    @Override
    public String getName() {
        return "enable";
    }

    @Override
    public String getDescription() {
        return "Command to enable arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw arena enable <arenaName>";
    }

    @Override
    public String getExample() {
        return "/bw arena enable castle";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        if (args.length == 3) {
            IArena arena = ArenaRegistry.byName(args[2]);
            if (arena != null) {
                if (arena.isEnabled()) {
                    msg.send(player, "AlreadyEnabled", Map.of("/arenaname/", arena.getArenaName()));
                    return;
                }
                if (!arena.canBeEnabled()) {
                    msg.send(player, "ArenaCantBeEnabled", Map.of("/arenaname/", arena.getArenaName()));
                    return;
                }
                if (ArenaMaps.isEnabled()) {
                    // Leaving setup: whatever the admin built is the map from now on.
                    try {
                        ArenaMaps.createTemplate(arena.getArenaName(), arena.getArenaWorld());
                    } catch (java.io.IOException e) {
                        NewBedwars.plugin.getLogger().warning("Could not save map template of " + arena.getArenaName() + ": " + e.getMessage());
                        msg.send(player, "MapTemplateFailed");
                        return;
                    }
                }
                arena.enable();
                msg.send(player, "ArenaEnabled", Map.of("/arenaname/", arena.getArenaName()));
            } else {
                msg.send(player, "ArenaNotExists");
            }
        } else {
            usage(player);
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 3) {
            return ArenaRegistry.names().stream().toList();
        }
        return null;
    }
}
