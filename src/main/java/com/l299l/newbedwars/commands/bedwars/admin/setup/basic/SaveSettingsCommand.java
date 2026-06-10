package com.l299l.newbedwars.commands.bedwars.admin.setup.basic;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.data.json.arenas.ArenaDataJson;
import org.bukkit.entity.Player;

import java.util.List;

public class SaveSettingsCommand extends SubCommand {
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
            player.sendMessage(NewBedwars.plugin.getMessages().getMsg(player, "saveSuccess"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}
