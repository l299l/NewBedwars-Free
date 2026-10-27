package com.l299l.newbedwars.commands.bedwars.admin.manageArena;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.setup.Setup;
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
            Setup setup = arena.getSetupMode();
            String state = setup != null && setup != Setup.READY
                    ? setup.name().toLowerCase() : arena.status().name();
            int max = arena.getTeams().size() * arena.getMaxInTeam();
            player.sendMessage(msg.getMsg(player, "ArenaListEntry")
                    .replaceAll("/arenaname/", arena.getArenaName())
                    .replaceAll("/status/", msg.getMsg(player, statusKey))
                    + msg.getMsg(player, "ArenaListDetails")
                    .replace("/state/", state)
                    .replace("/players/", String.valueOf(arena.getPlayers().size()))
                    .replace("/max/", String.valueOf(max)));
            if (!arena.getLoadProblems().isEmpty()) {
                player.sendMessage(msg.getMsg(player, "ArenaListProblems")
                        .replace("/problems/", String.join("; ", arena.getLoadProblems())));
            }
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return null;
    }
}