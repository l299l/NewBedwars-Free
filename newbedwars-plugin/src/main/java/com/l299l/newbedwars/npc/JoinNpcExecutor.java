package com.l299l.newbedwars.npc;

import com.l299l.newbedwars.arena.ArenaRegistry;
import com.l299l.newbedwars.NewBedwars;
import com.l299l.newbedwars.arena.IArena;
import com.l299l.newbedwars.arena.join.ArenaJoinService;
import com.l299l.newbedwars.gui.configuration.game.guis.ArenaSelectGUI;
import com.l299l.newbedwars.common.protocol.payload.TargetKind;
import com.l299l.newbedwars.network.NetworkManager;
import org.bukkit.entity.Player;

import java.util.Map;

public final class JoinNpcExecutor {
    private JoinNpcExecutor() {}

    public static void perform(Player p, JoinNpcAction action) {
        NetworkManager network = NetworkManager.get();
        if (network != null && NetworkManager.isLobby()) {
            performOnLobby(network, p, action);
            return;
        }
        switch (action.getType()) {
            case GROUP -> {
                if (!ArenaJoinService.mayJoin(p)) return;
                IArena best = ArenaJoinService.findBest(ArenaRegistry.all(), action::matches, ArenaJoinService.joinSize(p));
                if (best == null) {
                    NewBedwars.plugin.getMessages().send(p, "NpcNoFreeArena", Map.of("/group/", action.getTarget()));
                    return;
                }
                ArenaJoinService.joinArena(p, best);
            }
            case ARENA -> {
                IArena arena = ArenaRegistry.find(action.getTarget());
                if (arena == null) {
                    NewBedwars.plugin.getMessages().send(p, "ArenaNotExists");
                    return;
                }
                ArenaJoinService.join(p, arena);
            }
            case SELECTOR -> {
                if (!ArenaJoinService.mayJoin(p)) return;
                p.openInventory(new ArenaSelectGUI(p, action::matches).getInventory());
            }
        }
    }

    private static void performOnLobby(NetworkManager network, Player p, JoinNpcAction action) {
        switch (action.getType()) {
            case GROUP -> network.requestJoin(p, TargetKind.GROUP, action.getTarget());
            case ARENA -> network.requestArena(p, action.getTarget());
            case SELECTOR -> {
                if (!ArenaJoinService.mayJoin(p)) return;
                network.openSelector(p, action.getTarget() == null ? TargetKind.ANY : TargetKind.GROUP, action.getTarget());
            }
        }
    }
}
