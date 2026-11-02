package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import com.l299l.newbedwars.world.template.ArenaMaps;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Level;

public class SaveSettingsCommand extends BwCommand {
    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "Saves arena!";
    }

    @Override
    public String getSyntax() {
        return "/bw save";
    }

    @Override
    public String getExample() {
        return "/bw save";
    }

    @Override
    public void perform(Player player, String[] args, IArena arena) {
        try {
            ArenaDataJson arenaDataJson = new ArenaDataJson(arena);
            arenaDataJson.save();
            arena.getArenaWorld().save();
            player.sendMessage(msg.getMsg(player, "saveSuccess"));
            if (ArenaMaps.isEnabled()) {
                // The saved world becomes the pristine copy every game is reset from.
                ArenaMaps.createTemplateAsync(arena.getArenaName(), arena.getArenaWorld(), error -> {
                    if (error == null) {
                        msg.send(player, "MapTemplateSaved");
                    } else {
                        NewBedwars.plugin.getLogger().warning("Could not save map template of " + arena.getArenaName() + ": " + error);
                        msg.send(player, "MapTemplateFailed");
                    }
                });
            }
        } catch (Exception e) {
            NewBedwars.plugin.getLogger().log(Level.WARNING, "Could not save arena " + arena.getArenaName(), e);
            msg.send(player, "ArenaSaveFailed");
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
