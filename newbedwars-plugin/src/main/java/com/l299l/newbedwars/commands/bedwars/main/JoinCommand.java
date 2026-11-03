package com.l299l.newbedwars.commands.bedwars.main;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.group.ArenaGroup;
import com.l299l.newbedwars.arena.group.ArenaGroupManager;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.commands.bedwars.BwCommand;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class JoinCommand extends BwCommand {

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
        com.l299l.newbedwars.network.NetworkManager network = com.l299l.newbedwars.network.NetworkManager.get();
        if (network != null && com.l299l.newbedwars.network.NetworkManager.isLobby()) {

            if (args.length > 1) {
                network.requestJoinByName(p, args[1]);
            } else if (ArenaJoinService.mayJoin(p)) {
                network.openSelector(p, com.l299l.newbedwars.common.protocol.payload.TargetKind.ANY, null);
            }
            return;
        }
        if (!ArenaJoinService.mayJoin(p)) return;
        if (args.length > 1) {
            IArena arena = ArenaRegistry.byName(args[1]);
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
            com.l299l.newbedwars.network.NetworkManager network = com.l299l.newbedwars.network.NetworkManager.get();
            if (network != null && com.l299l.newbedwars.network.NetworkManager.isLobby()) {
                java.util.Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (com.l299l.newbedwars.network.RemoteArena a : network.getRemoteArenas().all()) {
                    names.add(a.getName());
                    names.add(a.getServer() + ":" + a.getName());
                    a.getGroup().ifPresent(names::add);
                }
                completions.addAll(names);
                return completions;
            }
            for (String arenaName : ArenaRegistry.names()) {
                IArena arena = ArenaRegistry.byName(arenaName);
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
