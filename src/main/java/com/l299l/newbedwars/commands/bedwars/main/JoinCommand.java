package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.Arena;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.commands.bedwars.SubCommand;
import com.l299l.newbedwars.config.Messages;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JoinCommand extends SubCommand {
    private final Messages msg;

    public JoinCommand() {
        msg = NewBedwars.plugin.getMessages();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Command to join arena";
    }

    @Override
    public String getSyntax() {
        return "/bw join (<arena|group>)";
    }

    @Override
    public String getExample() {
        return "/bw join arena1";
    }

    @Override
    public void perform(Player p, String[] args, IArena old) {
        if (!ArenaJoinService.mayJoin(p)) return;
        if (args.length > 1) {
            IArena arena = Arena.arenaByName.get(args[1]);
            if (arena == null) {
                ArenaGroup group = ArenaGroupManager.lookup(args[1]);
                if (group == null) {
                    msg.send(p, "ArenaNotExists");
                    return;
                }
                IArena best = ArenaJoinService.findBest(ArenaGroupManager.arenas(group), a -> true, ArenaJoinService.joinSize(p));
                if (best == null) {
                    msg.send(p, "NpcNoFreeArena", new java.util.HashMap<>() {{ put("/group/", group.getName()); }});
                    return;
                }
                arena = best;
            }
            ArenaJoinService.joinArena(p, arena);
        } else {
            p.openInventory(new ArenaSelectGUI(p).getInventory());
        }
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            for (String arenaName : Arena.arenaByName.keySet()) {
                IArena arena = Arena.arenaByName.get(arenaName);
                if (arena != null && arena.isEnabled()) {
                    completions.add(arena.getArenaName());
                }
            }
            ArenaGroupManager groups = ArenaGroupManager.get();
            if (groups != null) {
                for (ArenaGroup group : groups.getGroups()) completions.add(group.getName());
            }
            return completions;
        }
        return null;
    }
}
