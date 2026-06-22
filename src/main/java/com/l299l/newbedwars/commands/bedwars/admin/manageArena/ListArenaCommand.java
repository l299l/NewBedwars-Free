package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

public class ListArenaCommand extends SubCommand {
    private final Messages msg;

    public ListArenaCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Lists all arenas and their enabled/disabled status.";
    }

    @Override
    public String getSyntax() {
        return "/bw arena list";
    }

    @Override
    public String getExample() {
        return "/bw arena list";
    }

    @Override
    public void perform(Player player, String[] args, IArena old) {
        Collection<IArena> arenas = Arena.arenaByName.values();
        if (arenas.isEmpty()) {
            msg.send(player, "ArenaListEmpty");
            return;
        }
        player.sendMessage(msg.getMsg(player, "ArenaListHeader")
                .replaceAll("/count/", String.valueOf(arenas.size())));
        for (IArena arena : arenas) {
            String statusKey = Boolean.TRUE.equals(arena.isEnabled()) ? "ArenaListEnabled" : "ArenaListDisabled";
            player.sendMessage(msg.getMsg(player, "ArenaListEntry")
                    .replaceAll("/arenaname/", arena.getArenaName())
                    .replaceAll("/status/", msg.getMsg(player, statusKey)));
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}